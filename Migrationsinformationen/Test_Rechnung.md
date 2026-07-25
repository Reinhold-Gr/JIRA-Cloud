Gute Frage — ich würde in drei Stufen vorgehen, von innen nach außen:

---

## Stufe 1 — Isolierter Komponententest (ohne FO-Rendering)

Zuerst prüfen ob die Velocity-Kontextvariablen überhaupt korrekt befüllt werden, bevor das FO-Layout dazukommt. Dazu ein minimales Debug-Template anlegen:

```velocity
## debug-rechnung.vm — NUR zum Testen, kein FO
$include.parse($ctx, "rechnung-common-elements-fo.vm")
$scripting.execute("articles-tool.groovy")

PROJEKT:  $project.name
AUFTRAG:  $auftrag.summary  ($auftrag.key)
REGION:   $region.summary   ($region.key)
RECHNUNG: $rechnung.summary ($rechnung.key)

CF-Test customfield_10287: $region.getCustomFieldValue($customFieldManager.getCustomFieldObject("customfield_10287"))
CF-Test customfield_10303: $region.getCustomFieldValue($customFieldManager.getCustomFieldObject("customfield_10303"))
CF-Test customfield_10309: $region.getCustomFieldValue($customFieldManager.getCustomFieldObject("customfield_10309"))

articlesTool.formatNumber(1234.5): $articlesTool.formatNumber(1234.5)
```

Das gibt dir sofort Klarheit über die zwei Kernfragen:
- Funktioniert `$issueLinkManager.getLinkCollection()` → sind `$auftrag`, `$region`, `$rechnung` befüllt?
- Ist `$customFieldManager` direkt verfügbar?

Falls `$auftrag` leer ist, siehst du es hier — ohne dass du erst durch ein gescheitertes FO-Rendering suchen musst.

---

## Stufe 2 — Gezielter Tabellentest (Leistungspositionen + Preise)

Der komplexeste Teil des Templates ist die Preistabelle mit den sechs Leistungspositionen und der Null-Wert-Logik. Dafür ein Issue anlegen, das gezielt Grenzfälle abdeckt:

| Testfall | Setup | Erwartung |
|---|---|---|
| Nur Leistung 1 befüllt | `customfield_11456`/`11457` gesetzt, Rest leer | Nur Zeile 1, korrekte Zwischensumme |
| Betrag ohne Leistungstext (TIC-338) | `customfield_11459` gesetzt, `11458` leer | Dummy-Zeile ohne Absturz |
| Alle 6 Positionen | Alle Felder befüllt | Summe korrekt, MwSt. korrekt |
| Betrag = 0 oder null (TIC-305-Logik) | `customfield_11461` = null | Keine leere EUR-Zeile |

---

## Stufe 3 — Vollständiger Render-Test mit echten Daten

Erst wenn Stufe 1 und 2 sauber sind, das komplette `rechnung-in-issue-fo.vm` mit `#set($debug = true)` und `#set($env = "TEST")` gegen ein echtes Rechnungs-Issue rendern. Dabei konkret prüfen:

- QR-Code erscheint (URL `vampir.aktivsenioren-bayern.de/.../KEY_qr.png` erreichbar?)
- Banner-Grafik und Fußzeile laden
- Seitenzahl stimmt
- Datumsformat korrekt (`$currentDate` statt gespeichertem Datum)
- RL-Name und E-Mail erscheinen, Straße/Ort **nicht** (Vorstandsbeschluss 1.10.25)
- Länderspalte nur bei Nicht-Deutschland sichtbar

---

## Praktischer Ablauf in Better PDF Exporter Cloud

1. Im App-Menü: **PDF Templates → New Template** → `debug-rechnung.vm` als erstes anlegen
2. Über **"Preview"** direkt auf einem Test-Issue rendern — der Renderer zeigt Velocity-Fehler inline, kein PDF-Download nötig
3. Erst nach grünem Stufe-1-Test: `rechnung-in-issue-fo.vm` hochladen und über **"Export as PDF"** testen
4. Fehler im FO-Rendering erscheinen als roter Stack-Trace im Browser — deutlich schneller zu debuggen als im DC

Der entscheidende Vorteil der Stufe-1-Methode: Velocity-Fehler (fehlendes `$auftrag`, falscher Custom-Field-Key) und FO-Fehler (Layoutproblem, fehlende Grafik) sind sauber getrennt.

Soll ich das Debug-Template als fertige Datei aufbereiten?
# Migrations-Checkliste V3.1: Rechnung-Template (DC → Cloud)

## Zusammenfassung: Was tatsächlich geändert werden muss

Die Migration ist **minimal** — nur eine einzige Zeile muss entfernt werden:

```velocity
## ENTFERNEN (nur in rechnung-common-elements-fo.vm):
#set($customFieldManager = $ComponentAccessor.customFieldManager)
```

`$ComponentAccessor` ist DC-spezifisch. In Cloud stellt Better PDF Exporter
`$customFieldManager` direkt als Velocity-Tool bereit — die Zuweisung entfällt einfach.

---

## Vollständige Änderungsübersicht

| # | Datei | DC-Code | Cloud-Code | Grund |
|---|---|---|---|---|
| 1 | `rechnung-common-elements-fo.vm` | `#set($customFieldManager = $ComponentAccessor.customFieldManager)` | **Zeile löschen** | `$ComponentAccessor` nicht in Cloud; `$customFieldManager` direkt verfügbar |
| 2 | `rechnung-common-elements-fo.vm` | `$pdfContent.linkCollectionByIssue($issue)` | `$issueLinkManager.getLinkCollection($issue, $user)` | `$issueLinkManager` ist dokumentiertes Cloud-Velocity-Tool; `$pdfContent.linkCollectionByIssue()` in Cloud-Doku nicht gelistet |
| 3 | `rechnung-common-elements-fo.vm` | `$remoteIssueLinkManager.getRemoteIssueLinksForIssue(...)` | **Block entfernt** | Nur zur Existenzprüfung verwendet, nie zur Anzeige — ersatzlos entfernbar |
| 4 | `rechnung-in-issue-fo.vm` | **unverändert** | **unverändert** | Alle `$customFieldManager.getCustomFieldObject()`, `$scripting.execute()`, `$pdfRenderer`, `$xmlutils` etc. sind in Cloud identisch verfügbar |
| 5 | `articles-tool.groovy` | **unverändert** | **unverändert** | Reine Java/Groovy-Logik, keine Jira-Abhängigkeiten |

---

## Laut Midori Cloud-Doku verfügbare Velocity-Tools (Auswahl)

| Tool | Cloud verfügbar | Anmerkung |
|---|---|---|
| `$customFieldManager` | ✅ | Direkt im Kontext, kein ComponentAccessor |
| `$issueLinkManager` | ✅ | `.getLinkCollection(issue, user)` |
| `$remoteIssueLinkManager` | ✅ | Falls doch benötigt |
| `$scripting` | ✅ | `.execute("script.groovy")` identisch |
| `$pdfRenderer` | ✅ | `.asRendered(...)` identisch |
| `$xmlutils` | ✅ | `.escape(...)` identisch |
| `$userDateTimeFormatter` | ✅ | Identisch |
| `$ComponentAccessor` | ❌ | DC-Java-API, nicht in Cloud |
| `$pdfContent.linkCollectionByIssue()` | ⚠️ | Nicht dokumentiert — als Fallback auskommentiert |

---

## Offene Validierungspunkte (in Cloud-Sandbox zu testen)

### 1. `$issueLinkManager.getLinkCollection()` — PRIORITÄT HOCH
Primäre Methode für Issue-Link-Auflösung. Testschritte:
1. Better PDF Exporter Cloud (Trial) in Sandbox installieren
2. `rechnung-common-elements-fo.vm` und `rechnung-in-issue-fo.vm` als neue PDF-Ressourcen anlegen
3. Debug-Block einbauen, der `$auftrag.summary`, `$region.summary` als Text ausgibt
4. Test-Render auf einem Rechnungs-Issue mit bestehenden Links auslösen
5. Falls `$auftrag` leer bleibt → Fallback `$pdfContent.linkCollectionByIssue()` in `rechnung-common-elements-fo.vm` aktivieren (auskommentierten Block einkommentieren)

### 2. Custom-Field-IDs nach JCMA-Migration — PRIORITÄT HOCH
Custom-Field-IDs (`customfield_10222`, `customfield_11457` etc.) sind nach einer
JCMA-Migration **nicht garantiert stabil**. Nach dem Produktiv-Cutover:
- Administration → Custom Fields → alle referenzierten Felder prüfen
- Ggf. alle `customfield_NNNNN`-Referenzen in `.vm`-Dateien per Suchen/Ersetzen aktualisieren
- Betrifft beide `.vm`-Dateien

### 3. Projektnamen-Matching — PRIORITÄT MITTEL
Link-Auflösung matcht über `$linkedIssue.projectObject.name == "Auftrag"` etc.
Sicherstellen, dass Projektnamen nach Migration identisch bleiben:
- Auftrag, Klienten, Region, Abschlussbericht, Rechnungen

### 4. Externe Bild-URLs (vampir.as-by.de) — PRIORITÄT NIEDRIG
Better PDF Cloud läuft auf Forge/Connect — prüfen ob ausgehende HTTP-Calls
zu externen Domains ohne Allowlist erlaubt sind. Bei Problemen:
Midori Support kontaktieren re. externe Domain-Freischaltung.

### 5. `$env`-Erkennung automatisieren — OPTIONAL
Derzeit manuell: `#set($env = "TEST")` / `#set($env = "PROD")`
In Cloud könnte über `$baseUrl` automatisch erkannt werden:
```velocity
#if($baseUrl.contains("as-by"))
    #set($env = "PROD")
#else
    #set($env = "TEST")
#end
```

---

## Empfohlene Testreihenfolge

1. `articles-tool.groovy` isoliert: Mini-Template mit `$articlesTool.formatNumber(123.45)`
2. Link-Auflösung isoliert: Debug-Template das nur `$auftrag.summary` und `$region.summary` ausgibt
3. Erst danach vollständiges `rechnung-in-issue-fo.vm` mit Testdaten
4. Mit `#set($debug = true)` und `#set($env = "TEST")` bis alles stimmt
5. Dann `$env = "PROD"` und Produktiv-Cutover

---

## Dateien in diesem Migrationspaket

| Datei | Version | Status |
|---|---|---|
| `rechnung-in-issue-fo.vm` | V3.1-CLOUD | ✅ fertig |
| `rechnung-common-elements-fo.vm` | V3.1-CLOUD | ✅ fertig |
| `articles-tool.groovy` | unverändert | ✅ 1:1 übernehmen |
| `Test_Rechnung.md` | V3.1 | Dokument MIGRATION-CHECKLIST Rechnung|