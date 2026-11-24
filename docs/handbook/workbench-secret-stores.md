# Replace the Workbench secret store

[Handbook home](README.md) · [Extensions](extensions.md)

Passwords and tokens are kept apart from everything else the Workbench stores. Metadata and
Collector configurations hold only a reference; the value is behind
`io.softwareecg.awb.secrets.api.SecretStore`:

```java
public interface SecretStore {
    void store(String reference, String secret);
    String read(String reference);      // null if nothing is stored under the reference
    void delete(String reference);
}
```

The one implementation today is `FileSecretStore` in `secrets.store`. It writes one file
per secret to `~/.ekg/secrets`, readable by the owner only. The files are not encrypted: who can
read the user's files can read the secrets.

To keep secrets somewhere else, for example in the keyring of the operating system:

1. Implement `SecretStore` in `secrets.store`. A reference consists of letters, digits,
   dot, dash and underscore.
2. Bind it as described for the metadata stores: `@Singleton` on the new store, and either remove
   the annotation from `FileSecretStore` or mark the new store `@Primary`.
3. Decide what happens to secrets already in `~/.ekg/secrets`: read them once and store them in
   the new place, or ask the user to enter them again.
4. Test storing, replacing, reading a missing reference and deleting, and that a failure of the
   store is reported rather than answered with an empty secret.
