# Migrations-Checkliste: Rechnung-Template (rechnung-in-issue-fo.vm)

## DC → Cloud — was geändert wurde

| # | DC-Code (alt) | Cloud-Code (neu) | Grund |
|---|---|---|---|
| 1 | `$ComponentAccessor.customFieldManager` | entfernt | Existiert in Cloud nicht (Java-API) |
| 2 | `$xyz.getCustomFieldValue($customFieldManager.getCustomFieldObject("customfield_N"))` | `$xyz.getCustomFieldValue("customfield_N")` | Cloud-natives Better-PDF-API-Pattern, kein customFieldManager nötig |
| 3 | `$remoteIssueLinkManager.getRemoteIssueLinksForIssue(...)` | entfernt | DC-Java-API; im Original nur zur Existenzprüfung verwendet, Ergebnis nicht im PDF angezeigt → ersatzlos entfernbar |
| 4 | `$pdfContent.linkCollectionByIssue(...)` | unverändert übernommen (Variante A) | Better-PDF-eigenes Tool, vermutlich auch in Cloud verfügbar — **muss validiert werden** |
| 5 | Logo-/Footer-URLs, QR-Code-Block | unverändert | reine HTTP(S)-Aufrufe, deployment-unabhängig |
| 6 | `articles-tool.groovy` | unverändert | keine Jira-Abhängigkeiten |

## Offene Validierungspunkte (in der Cloud-Sandbox zu testen)

### 1. `$pdfContent.linkCollectionByIssue()` — KRITISCH
Das ist exakt die offene Frage aus eurer Migrationsplanung. Testschritte:

1. Better PDF Exporter Cloud (Trial) in Sandbox installieren
2. `rechnung-common-elements-fo.vm` und `rechnung-in-issue-fo.vm` als neue PDF-Ressourcen anlegen
3. Auf einem Test-Issue im Projekt "Rechnungen" (mit bestehenden Issue-Links zu Auftrag/Region) den PDF-Export auslösen
4. Falls Fehler `Method linkCollectionByIssue not found` o.ä. auftritt → **Variante B** in `rechnung-common-elements-fo.vm` aktivieren (auskommentierten Block einkommentieren, REST-Helper-Script schreiben)
5. Falls Fehler bei `.linkTypes`, `.getInwardIssues()` oder `.getOutwardIssues()` → Methodensignatur in Cloud-Doku unter "Recipes" / "Scripting" mit Midori-Support klären

### 2. Custom-Field-IDs nach Migration
**Wichtig:** Custom-Field-IDs (`customfield_10222`, `customfield_11457` etc.) sind in Jira **nicht garantiert stabil** über eine JCMA-Migration. Nach dem Produktiv-Cutover:
- Alle referenzierten Custom-Field-IDs in Cloud neu prüfen (Administration → Custom Fields)
- Ggf. alle `customfield_NNNNN`-Referenzen in beiden `.vm`-Dateien per Suchen/Ersetzen aktualisieren

### 3. Projektnamen-Matching (`"Auftrag"`, `"Region"`, `"Klienten"`, etc.)
Die Logik matched über `$linkedIssue.projectObject.name == "Auftrag"`. Sicherstellen, dass diese Projektnamen nach der Migration identisch bleiben (Projektnamen werden bei JCMA i.d.R. übernommen, aber Projekt-**Keys** können sich ändern — hier unkritisch, da nur `.name` verwendet wird).

### 4. Externe Bild-URLs (vampir.as-by.de)
Better PDF Cloud läuft auf Forge (serverlose Infrastruktur) bzw. älteren Connect-Versionen. Prüfen, ob:
- Forge-Apps ausgehende HTTP-Calls zu beliebigen externen Domains ohne Egress-Allowlist erlauben
- Falls nicht: Logo/Footer/QR-Bilder ggf. als Issue-Attachments oder über die App-eigene Bild-Einbettung lösen müssen

### 5. `$pdfRenderer.asRendered(...)`
Wird im Original für mehrzeilige Beschreibungsfelder verwendet (Workaround für Zeilenumbrüche). Sollte als Standard-Velocity-Tool von Better PDF Exporter in Cloud identisch vorhanden sein — niedriges Risiko, aber im Test-Render mitprüfen.

## Empfohlene Testreihenfolge

1. `articles-tool.groovy` isoliert testen (z.B. mit Mini-Template, das nur `$articlesTool.formatNumber(123.45)` ausgibt)
2. `rechnung-common-elements-fo.vm` isoliert testen: Debug-Block einbauen, der `$auftrag`, `$region`, `$rechnung` als einfachen Text ausgibt, um zu prüfen, ob die Link-Auflösung funktioniert
3. Erst danach das komplette `rechnung-in-issue-fo.vm` mit echten Testdaten rendern
4. Mit `#set($debug = true)` und `#set($env = "TEST")` arbeiten, bis alles stimmt — dann erst auf `$env = "PROD"` umstellen
