package io.softwareecg.awb.infrastructure.ssh;

/**
 * Holds the output of a synchronous SSH command execution.
 *
 * @param stdout the standard output (may be null if no output was produced)
 * @param stderr the error output (may be null if no errors occurred)
 */
public record SshResult(String stdout, String stderr) {

    /**
     * Returns true if the command produced error output.
     */
    public boolean hasErrors() {
        return stderr != null && !stderr.isBlank();
    }
}
