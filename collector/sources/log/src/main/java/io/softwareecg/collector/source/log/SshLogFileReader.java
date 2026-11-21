package io.softwareecg.collector.source.log;

import io.softwareecg.collector.ssh.ConnectionFactory;
import io.softwareecg.collector.ssh.ExecSession;
import io.softwareecg.collector.ssh.SshConnection;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Reads a log file of another machine over SSH.
 * <p>
 * Every reading is one short command that says what the file is and how long, and prints the
 * part asked for. Nothing has to be installed on the machine: the command needs a shell and
 * {@code ls}, {@code wc}, {@code tail} and {@code head}, as every Linux has them.
 */
final class SshLogFileReader implements LogFileReader {

    /** Frames what the command prints, so that it is told from what a login script may print. */
    private static final String MARK = "@@EKG-LOG@@";

    private final String host;

    private final int port;

    private final String user;

    private final String password;

    private final String file;

    /** The character set to convert from on the machine; {@code null} if the file is UTF-8. */
    private final String convertFrom;

    private SshConnection connection;

    SshLogFileReader(String host, int port, String user, String password, String file, Charset charset) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.file = file;
        this.convertFrom = StandardCharsets.UTF_8.equals(charset) ? null : charset.name();
    }

    @Override
    public Part read(String fileKey, long position, boolean startAtEnd, int maxBytes) throws IOException {
        return parse(execute(command(file, fileKey, position, startAtEnd, maxBytes, convertFrom)));
    }

    private String execute(String command) throws IOException {
        if (connection == null) {
            connection = ConnectionFactory.getInstance().getConnection(host, port, user, password);
        }
        try {
            // a session runs one command
            ExecSession session = connection.openSynchronousExecSession(command);
            String output = session.executeCommand().getFullStdOutput();
            session.close();
            return output;
        } catch (Exception e) {
            // the connection may be stale: the next reading opens a new one
            try {
                connection.close();
            } catch (RuntimeException ignored) {
                // it is given up anyway
            }
            connection = null;
            throw new IOException(describe() + " cannot be read over SSH: " + reason(e), e);
        }
    }

    private static String reason(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    /**
     * The command of one reading. It prints a line with what identifies the file (its inode),
     * its size and the part that follows, then that part, then a line that ends the answer.
     * <p>
     * It is run by {@code sh}, whatever the login shell of the user is.
     */
    static String command(String file, String fileKey, long position, boolean startAtEnd, int maxBytes,
                          String convertFrom) {
        String convert = convertFrom == null ? "" : " | iconv -c -f " + quote(convertFrom) + " -t UTF-8";
        String script = "f=" + quote(file) + "; i=" + quote(fileKey == null ? "" : fileKey)
                + "; o=" + position + "; m=" + maxBytes + "; e=" + (startAtEnd ? 1 : 0) + "; "
                + "if [ ! -f \"$f\" ] || [ ! -r \"$f\" ]; then echo '" + MARK + " missing'; exit 0; fi; "
                // -L: a name that is a link stands for the file it points to
                + "n=$(ls -Li -- \"$f\" | awk '{print $1; exit}'); "
                + "s=$(wc -c < \"$f\" | tr -d ' \\t'); "
                + "if [ -z \"$i\" ]; then if [ \"$e\" = 1 ]; then o=$s; else o=0; fi; "
                + "elif [ \"$n\" != \"$i\" ] || [ \"$s\" -lt \"$o\" ]; then o=0; fi; "
                + "c=$((s-o)); if [ \"$c\" -gt \"$m\" ]; then c=$m; fi; "
                + "echo \"" + MARK + " $n $s $o $c\"; "
                + "if [ \"$c\" -gt 0 ]; then tail -c +$((o+1)) -- \"$f\" | head -c \"$c\"" + convert + "; fi; "
                + "echo; echo '" + MARK + " end'";
        return "sh -c " + quote(script);
    }

    /**
     * @return the text as one word of a shell command
     */
    static String quote(String text) {
        return "'" + text.replace("'", "'\\''") + "'";
    }

    /**
     * Reads the answer of {@link #command}.
     *
     * @return the part read, or {@code null} if the command found no file it may read
     * @throws IOException if the answer is not that of the command, or not complete
     */
    static Part parse(String output) throws IOException {
        if (output == null) {
            throw new IOException("The log file was not read: the command gave no answer.");
        }
        // the session runs in a terminal, which ends lines with a carriage return and a line feed
        String answer = output.replace("\r\n", "\n");

        int header = answer.startsWith(MARK + " ") ? 0 : answer.indexOf("\n" + MARK + " ") + 1;
        if (header == 0 && !answer.startsWith(MARK + " ")) {
            throw new IOException("The log file was not read. The machine answered: " + shortened(answer));
        }
        int headerEnd = answer.indexOf('\n', header);
        String[] fields = answer.substring(header, headerEnd < 0 ? answer.length() : headerEnd).strip().split(" ");
        if (fields.length == 2 && "missing".equals(fields[1])) {
            return null;
        }
        int end = answer.lastIndexOf("\n" + MARK + " end");
        if (fields.length != 5 || headerEnd < 0 || end < headerEnd) {
            throw new IOException("The log file was not read completely. The machine answered: " + shortened(answer));
        }
        try {
            long size = Long.parseLong(fields[2]);
            long start = Long.parseLong(fields[3]);
            long count = Long.parseLong(fields[4]);
            return new Part(fields[1], size, start, start + count, answer.substring(headerEnd + 1, end));
        } catch (NumberFormatException e) {
            throw new IOException("The log file was not read. The machine answered: " + shortened(answer));
        }
    }

    private static String shortened(String text) {
        String line = text.strip();
        return line.length() <= 200 ? line : line.substring(0, 200) + " ...";
    }

    @Override
    public String describe() {
        return file + " on " + host;
    }
}
