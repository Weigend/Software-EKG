package io.softwareecg.collector.source.log;

import java.io.IOException;

/**
 * Reads what was added to a log file since the last time, on this machine or on another one.
 * <p>
 * A file is followed by remembering where reading ended, not by a process that stays connected
 * to it. A connection that breaks then costs nothing: the next reading continues at the
 * remembered place, with no line missing and none read twice.
 */
interface LogFileReader {

    /**
     * Reads the next part of the file.
     * <p>
     * A log file is replaced now and then - renamed and begun anew, or emptied. That is the case
     * if it is not the file it was the last time, or shorter than what was already read; reading
     * then begins at the start of what is there now.
     *
     * @param fileKey    what identified the file the last time ({@link Part#fileKey()});
     *                   {@code null} for the first reading
     * @param position   up to where the file was read the last time
     * @param startAtEnd for the first reading: {@code true} to begin with what is written from now
     *                   on, {@code false} to begin with the content the file already has
     * @param maxBytes   how much is read at most
     * @return what was read, or {@code null} if there is no such file or it may not be read
     * @throws IOException if the file or its machine cannot be reached
     */
    Part read(String fileKey, long position, boolean startAtEnd, int maxBytes) throws IOException;

    /**
     * @return where the file is, for messages
     */
    String describe();

    /**
     * @param fileKey identifies the file - not its name, which stays when the file is replaced
     * @param size    the size of the file when it was read
     * @param start   from where it was read: the position asked for, the end for a first reading
     *                that begins there, or 0 if the file was replaced
     * @param end     up to where it was read; less than the size if there was more than asked for
     * @param text    what was read. It ends in the middle of a line if the line was not written
     *                completely yet or did not fit; the rest comes with the next reading.
     */
    record Part(String fileKey, long size, long start, long end, String text) {

        /**
         * @return {@code true} if the file has more that was not read yet
         */
        boolean hasMore() {
            return end < size;
        }
    }
}
