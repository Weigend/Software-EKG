# Zu Software-EKG beitragen

[English](CONTRIBUTING.md) | **Deutsch**

Danke für dein Interesse an Software-EKG. Kleine, klar abgegrenzte Fehlerkorrekturen und
Dokumentationsverbesserungen sind besonders willkommen. Für größere Änderungen bitte zuerst ein
Issue eröffnen, damit Ziel, Architektur und Wartungsaufwand abgestimmt werden können.

## Entwicklungsumgebung

Benötigt werden JDK 25 mit JavaFX 25, Maven 3.9 oder neuer sowie unter Linux ein Display oder
`xvfb-run`. Die externe WFX-Abhängigkeit wird einmalig installiert:

```bash
./install-wfx.sh
mvn clean package
xvfb-run -a mvn test
```

Weitere Build- und Startvarianten stehen im [README](README.de.md#bauen-und-testen).

## Pull Requests

- Halte Änderungen möglichst klein und thematisch geschlossen.
- Ergänze oder aktualisiere Tests für verändertes Verhalten.
- Führe vor dem Pull Request `xvfb-run -a mvn verify` aus, und `./run-e2e-tests.sh`, wenn du einen
  Importer, seinen Dialog oder die Menüs geändert hast.
- Dokumentiere sichtbare Änderungen und neue Konfigurationsoptionen.
- Committe keine Zugangsdaten, privaten Schlüssel, Produktionsdaten oder generierten Build-Dateien.
- Akzeptiere, dass Collector-Erweiterungspunkte derzeit keine dauerhaft stabile Drittanbieter-API
  versprechen; inkompatible Änderungen werden möglichst in den Release Notes dokumentiert.

Mit einem Beitrag erklärst du dich damit einverstanden, dass er unter der Apache License 2.0 des
Projekts veröffentlicht wird.

## Fehler und Funktionswünsche

Nutze die passenden GitHub-Issue-Formulare und füge Version, Betriebssystem, JDK-Version,
Reproduktionsschritte und relevante Logauszüge hinzu. Entferne vorher Kennwörter, Tokens,
Hostnamen und andere vertrauliche Informationen. Sicherheitslücken werden nach
[SECURITY.de.md](SECURITY.de.md) vertraulich gemeldet.
