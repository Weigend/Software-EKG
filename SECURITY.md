# Security Policy

**English** | [Deutsch](SECURITY.de.md)

## Supported versions

Security fixes are provided for the most recently published version. Older versions do not
receive regular security updates.

## Reporting a vulnerability

Please do not report security vulnerabilities as a public GitHub issue. Use GitHub's private
vulnerability reporting instead, under **Security → Report a vulnerability**. If this feature is
not available, open an issue without technical details and ask for a confidential contact
channel.

Please describe the affected versions, steps to reproduce, possible impact and – if known – a
possible remedy. We aim to acknowledge receipt within seven days and coordinate disclosure and
fix with the reporter.

## Security model

Software-EKG processes system metrics and can store credentials for data sources. By default,
the workbench data is stored locally under `~/.ekg`. Configuration files should reference
secrets via environment variables and not contain them directly.

The optional AI Assistant can send excerpts of the analysed data to the model provider
configured by the user. Which data leaves the machine depends on the chosen provider and model
endpoint. With Ollama or another local endpoint, processing can take place entirely locally.
Before use with production data, the data protection and retention rules of the respective
provider must be checked.

SSH commands proposed by the AI Assistant are only executed after explicit approval by the user.
Nevertheless, use accounts with minimal privileges and preferably read-only access. Credentials,
private keys and production configurations do not belong in this repository.
