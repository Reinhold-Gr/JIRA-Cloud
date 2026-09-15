# Planungsvorschlag: Migration Better-PDF-Exporter-Templates (DC → Cloud)

## 1. Grundprinzip: erweiterbares ID-Schema

- **Hauptaufgabe** = `T-n` (fortlaufend, z. B. T-1, T-2, T-3 …)
- **Unteraufgabe** = `T-n.m` (z. B. T-1.1, T-1.2 …)
- **Noch nicht eingeordnete / unklare Aufgaben** = `T-x` (Platzhalter, bis Zuordnung geklärt ist)
- Neue Templates bekommen einfach die nächste freie `T-n`, ohne bestehende Nummern zu verschieben → die Struktur bleibt stabil, auch wenn Themenblöcke wachsen.

## 2. Status-Legende

| Kürzel | Bedeutung |
|---|---|
| fertig | Hauptaufgabe abgeschlossen |
| erl. | Teilaufgabe abgeschlossen |
| i.Arb. | in Arbeit |
| ersetzbar? | zu klären, ob Aufgabe überhaupt noch nötig ist |
| mit T-x | wird zusammen mit übergeordneter Aufgabe erledigt/übernommen |
| (leer) | noch nicht begonnen bzw. nicht erfasst |

## 3. Themenblöcke (aus der Tabelle abgeleitet)

### Block A – Auftragsbestätigung
| ID | Name (DC) | Status | CF angepasst | Beta-Test | Integrationstest | Übernahme Produktion | Offene Aktion |
|---|---|---|---|---|---|---|---|
| T-1 | acceptation-confirmation-fo.vm | i.Arb. | erl. | erl. | i.Arb. | – | – |
| T-1.1 | Auftragsbestätigung PDF erzeugen u. anhängen | fertig | erl. | erl. | mit T-1 | – | – |
| T-1.2 | Auftragsbestätigung PDF erzeugen, anhängen u. mailen | fertig | erl. | erl. | mit T-1 | – | – |
| T-1.3 | auftrag-common-elements-fo.vm | fertig | erl. | erl. | mit T-1 | – | – |

### Block B – Eingangsbestätigung
| ID | Name (DC) | Status | CF angepasst | Beta-Test | Integrationstest | Übernahme Produktion | Offene Aktion |
|---|---|---|---|---|---|---|---|
| T-2 | initial-confirmation-fo.vm | i.Arb. | erl. | erl. | – | – | **Wie wird das Template angetriggert? Von wem?** |
| T-2.1 | Flow: Eingangsbestätigung → PDF-Export des Issues inkl. Mail | fertig | erl. | erl. | – | – | Test über Kommentareingabe |

### Block C – Rechnung
| ID | Name (DC) | Status | CF angepasst | Beta-Test | Integrationstest | Übernahme Produktion | Offene Aktion |
|---|---|---|---|---|---|---|---|
| T-3 | rechnung-in-issue-fo.vm | i.Arb. | erl. | – | – | – | – |
| T-3.1 | rechnung-common-elements-fo.vm | i.Arb. | erl. | – | mit T-3 | – | – |
| T-3.2 | rechnung-number-tool.groovy | erl. | erl. | erl. | – | – | – |
| T-3.3 | QR-Code | erl. | erl. | erl. | – | – | nicht integriert in .vm |

### Block D – Storno / Korrektur *(neu vorgeschlagen, noch nicht begonnen)*
| ID | Name (DC) | Status | CF angepasst | Beta-Test | Integrationstest | Übernahme Produktion | Offene Aktion |
|---|---|---|---|---|---|---|---|
| T-4 | rechnung-storno-in-issue-with-current-rechnung-fo.vm | – | – | – | – | – | noch einzuplanen |
| T-4.1 | storno-korrektur-mit-manueller-rechnungsnummer-fo.vm | – | – | – | – | – | noch einzuplanen |

### Sonstige / noch nicht eingeordnet
| ID | Name (DC) | Status | CF angepasst | Beta-Test | Integrationstest | Übernahme Produktion | Offene Aktion |
|---|---|---|---|---|---|---|---|
| T-5 | abschlussbericht-fo.vm | – | – | – | – | – | noch keine Einordnung |
| T-x | articles-tool.groovy | ersetzbar? | – | – | – | – | Prüfen, ob überhaupt benötigt |

## 4. Offene Punkte auf einen Blick

1. **T-2** – Trigger-Mechanismus für `initial-confirmation-fo.vm` klären: Welches Event/wer löst den Versand aus?
2. **T-3.3** – QR-Code ist fertig, aber noch nicht in `rechnung-in-issue-fo.vm` integriert.
3. **T-4 / T-4.1** – Storno-Templates sind bisher nicht bearbeitet, Aufwand noch nicht geschätzt.
4. **T-5** – `abschlussbericht-fo.vm` ist nur als Name gelistet, ohne jeglichen Status.
5. **T-x** – Klären, ob `articles-tool.groovy` in der Cloud-Version noch gebraucht wird oder durch Bordmittel ersetzbar ist.

## 5. Vorschlag für zusätzliche Spalten (macht die Tabelle steuerbarer)

| Neue Spalte | Zweck |
|---|---|
| Priorität | Hoch/Mittel/Niedrig – für Reihenfolge bei begrenzter Zeit |
| Verantwortlich | falls mehrere Personen beteiligt sind |
| Zieltermin / Sprint | Planbarkeit, z. B. an Jira-Sprints gekoppelt |
| Abhängigkeit | z. B. „T-3 abhängig von T-3.1 und T-3.3" |
| Jira-Ticket-Nr. | Verknüpfung zur eigentlichen Migrations-Story |

Diese Spalten lassen sich einfach an die bestehende Tabelle anhängen, ohne die Struktur der Blöcke zu verändern.

## 6. Empfohlene nächste Schritte

1. Offene Frage zu T-2 (Trigger) klären – blockiert sonst den Übergang in die Produktion.
2. QR-Code-Integration in T-3 abschließen (T-3.3 fertigstellen).
3. Block D (Storno) grob schätzen und in die Planung mit aufnehmen (T-4/T-4.1).
4. T-5 und T-x einordnen: Bedarf prüfen, ggf. Status und Teilaufgaben ergänzen.
5. Bei Bedarf die vorgeschlagenen Zusatzspalten (Abschnitt 5) einführen, sobald Priorisierung/Termine feststehen.
