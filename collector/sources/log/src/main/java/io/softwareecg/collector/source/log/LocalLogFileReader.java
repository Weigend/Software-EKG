package io.softwareecg.collector.source.log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Reads a log file of the machine the collector runs on. Needs nothing but Java, so it works on
 * Windows as well.
 */
final class LocalLogFileReader implements LogFileReader {

    private final Path file;

    private final Charset charset;

    LocalLogFileReader(Path file, Charset charset) {
        this.file = file;
        this.charset = charset;
    }

    @Override
    public Part read(String fileKey, long position, boolean startAtEnd, int maxBytes) throws IOException {
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            return null;
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            String key = key(Files.readAttributes(file, BasicFileAttributes.class));
            long size = channel.size();

            long start = position;
            if (fileKey == null) {
                start = startAtEnd ? size : 0;
            } else if (!fileKey.equals(key) || size < position) {
                start = 0;
            }

            int length = (int) Math.min(maxBytes, size - start);
            ByteBuffer buffer = ByteBuffer.allocate(length);
            while (buffer.hasRemaining()) {
                if (channel.read(buffer, start + buffer.position()) < 0) {
                    break;   // the file became shorter while it was read
                }
            }
            int read = buffer.position();
            byte[] bytes = buffer.array();

            // If there is more than was read, the part ends where a line ends: a character of
            // several bytes is then never cut in two.
            if (start + read < size) {
                for (int i = read - 1; i >= 0; i--) {
                    if (bytes[i] == '\n') {
                        read = i + 1;
                        break;
                    }
                }
            }
            return new Part(key, size, start, start + read, new String(bytes, 0, read, charset));
        } catch (NoSuchFileException e) {
            return null;   // replaced just now; the next reading finds the new file
        }
    }

    /**
     * The name of a file stays when the file is replaced; what the file system identifies it by
     * does not. Where the file system has no such key the time of creation takes its place.
     */
    private static String key(BasicFileAttributes attributes) {
        Object key = attributes.fileKey();
        return key != null ? key.toString() : "created " + attributes.creationTime().toMillis();
    }

    @Override
    public String describe() {
        return file.toString();
    }
}
