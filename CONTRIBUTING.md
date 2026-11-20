# Contributing to Software-EKG

**English** | [Deutsch](CONTRIBUTING.de.md)

Thank you for your interest in Software-EKG. Small, clearly scoped bug fixes and documentation
improvements are especially welcome. For larger changes, please open an issue first so that
goal, architecture and maintenance effort can be agreed on.

## Development environment

You need JDK 25 with JavaFX 25, Maven 3.9 or newer and, on Linux, a display or `xvfb-run`. The
external WFX dependency is installed once:

```bash
./install-wfx.sh
mvn clean package
xvfb-run -a mvn test
```

Further build and start variants are described in the [README](README.md#build-and-test).

## Pull requests

- Keep changes as small and thematically self-contained as possible.
- Add or update tests for changed behaviour.
- Run `xvfb-run -a mvn verify` before opening the pull request.
- Document visible changes and new configuration options.
- Do not commit credentials, private keys, production data or generated build files.
- Accept that collector extension points currently do not promise a permanently stable
  third-party API; incompatible changes are documented in the release notes where possible.

By contributing, you agree that your contribution is published under the project's Apache
License 2.0.

## Bugs and feature requests

Use the appropriate GitHub issue forms and include version, operating system, JDK version,
steps to reproduce and relevant log excerpts. Remove passwords, tokens, host names and other
confidential information first. Security vulnerabilities are reported confidentially as
described in [SECURITY.md](SECURITY.md).
