# Security Policy

## Unterstützte Versionen

Sicherheitskorrekturen werden für die jeweils neueste veröffentlichte Version bereitgestellt.
Ältere Versionen erhalten keine regelmäßigen Sicherheitsupdates.

## Sicherheitslücken melden

Bitte Sicherheitslücken nicht als öffentliches GitHub-Issue melden. Verwende stattdessen GitHubs
private Vulnerability-Reporting-Funktion unter **Security → Report a vulnerability**. Falls diese
Funktion nicht verfügbar ist, eröffne ein Issue ohne technische Details und bitte um einen
vertraulichen Kontaktweg.

Bitte beschreibe betroffene Versionen, Reproduktionsschritte, mögliche Auswirkungen und – soweit
bekannt – eine mögliche Abhilfe. Wir bestätigen den Eingang möglichst innerhalb von sieben Tagen
und stimmen Veröffentlichung und Korrektur mit dem Meldenden ab.

## Sicherheitsmodell

Software-EKG verarbeitet Systemmetriken und kann Zugangsdaten für Datenquellen speichern. Die
Daten der Workbench liegen standardmäßig lokal unter `~/.ekg`. Konfigurationsdateien sollten
Geheimnisse über Umgebungsvariablen referenzieren und nicht direkt enthalten.

Der optionale AI Assistant kann Ausschnitte der analysierten Daten an den vom Anwender
konfigurierten Modellanbieter senden. Welche Daten den Rechner verlassen, hängt vom gewählten
Anbieter und Modell-Endpunkt ab. Bei Ollama oder einem anderen lokalen Endpunkt kann die
Verarbeitung vollständig lokal erfolgen. Vor dem Einsatz mit Produktionsdaten müssen die
Datenschutz- und Aufbewahrungsregeln des jeweiligen Anbieters geprüft werden.

Vom AI Assistant vorgeschlagene SSH-Kommandos werden erst nach ausdrücklicher Freigabe des
Anwenders ausgeführt. Verwende dennoch Konten mit minimalen Rechten und vorzugsweise nur lesendem
Zugriff. Zugangsdaten, private Schlüssel und produktive Konfigurationen gehören nicht in dieses
Repository.
