Die **Better-PDF-Cloud-Alternative** ist deutlich einfacher, weil ihr dort keinen eigenen Service hosten müsst – die PDF-Erzeugung bleibt komplett innerhalb des Better-PDF-Ökosystems.## Wie das funktioniert

**Alles bleibt innerhalb der App** – Better PDF Cloud übernimmt Templating und PDF-Rendering intern:

- Ihr ladet eure Velocity-Templates direkt in die App hoch (genau wie bisher in Data Center).
- Der Trigger läuft über eine **Jira-Automation-Regel**, die als Aktion "PDF generieren" (bzw. den entsprechenden Better-PDF-Cloud-Automation-Baustein) aufruft – oder über den in Better PDF Cloud eingebauten eigenen Trigger-Mechanismus, je nachdem was die App-Version anbietet.
- Velocity füllt die Platzhalter, FOP rendert daraus das finale PDF, die App hängt es selbst ans Issue an.
- Kein eigener Server, kein Hosting, keine Authentifizierungs-Infrastruktur zwischen Jira und einem externen Dienst nötig.

## Der Haken – und der Grund, warum ihr überhaupt über OpenPDF nachdenkt

Better PDF Cloud rendert innerhalb einer **kontrollierten Sandbox**. Das bedeutet konkret:

| | Better PDF Cloud | Externer OpenPDF-Service |
|---|---|---|
| Hosting-Aufwand | Keiner | Eigener Service nötig |
| Direkter Java-Zugriff (`com.lowagie.text.*`, `$issueLinkManager` etc.) | Nur die von der App dokumentierten Velocity-Kontextobjekte | Voller Zugriff, beliebige Java-Logik |
| Groovy-Helper wie in DC gewohnt | Eingeschränkt auf das von Better PDF Cloud unterstützte Subset | Uneingeschränkt |
| Wartung/Updates | Läuft mit der App mit | Müsst ihr selbst pflegen |
| Layout-Flexibilität | Durch Velocity/XSL-FO begrenzt | Volle Kontrolle über PDF-Aufbau |

Das war ja genau euer bisheriges Thema: Ob `$pdfContent.linkCollectionByIssue()` in Better PDF Cloud funktioniert, war so eine Grenze der Sandbox – gelöst durch `$issueLinkManager.getLinkCollection()`, weil das die von der App unterstützte API ist.

**Praktische Empfehlung:** Wenn eure 8 Templates mit den von Better PDF Cloud unterstützten Velocity-Kontextobjekten auskommen (auch wenn ihr dafür wie beim Link-Problem Umwege über andere Methoden nehmen müsst), ist der Better-PDF-Weg der pragmatischere – kein zusätzliches System zu betreiben. Der externe OpenPDF-Service lohnt sich nur, wenn ihr an eine echte Sandbox-Grenze stoßt, die sich nicht umgehen lässt.

Habt ihr schon getestet, ob alle 8 Templates mit reinem Velocity + den dokumentierten Better-PDF-Cloud-Objekten funktionieren, oder gibt es noch offene Fälle wie das `linkCollectionByIssue`-Problem?