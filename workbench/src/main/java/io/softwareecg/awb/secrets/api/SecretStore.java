package io.softwareecg.awb.secrets.api;

/**
 * Keeps secrets - passwords, tokens - apart from everything else the AWB stores. Configuration
 * and metadata only hold a reference to a secret; the value itself lives here.
 * <p>
 * The interface is the seam for where secrets are kept: a file readable by the owner only,
 * the keyring of the operating system or an encrypted store.
 */
public interface SecretStore {

    /**
     * Stores a secret under the reference, replacing an existing one.
     *
     * @param reference the name of the secret; letters, digits, dot, dash and underscore
     */
    void store(String reference, String secret);

    /**
     * @return the secret, or {@code null} if nothing is stored under the reference
     */
    String read(String reference);

    void delete(String reference);
}
