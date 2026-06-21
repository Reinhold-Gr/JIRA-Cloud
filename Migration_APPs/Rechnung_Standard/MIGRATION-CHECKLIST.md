# Migrations-Checkliste: Rechnung-Template (rechnung-in-issue-fo.vm)

## DC → Cloud — was geändert wurde

| # | DC-Code (alt)                           | Cloud-Code (neu)                            | Grund                                          |
|---|-----------------------------------------|---------------------------------------------|------------------------------------------------|
| 1 | `$ComponentAccessor.customFieldManager` | entfernt                                    | Existiert in Cloud nicht (Java-API)            |
| 2 | `$xyz.getCustomFieldValue($customFieldManager.getCustomFieldObject("customfield_N"))` |                                                |
|   |                                         | `$xyz.getCustomFieldValue("customfield_N")` | Cloud-natives Better-PDF-API-Pattern,          |
|   |                                         |                                             |     kein customFieldManager nötig              |
| 3 | `$remoteIssueLinkManager.getRemoteIssueLinksForIssue(...)`                            |                                                |
|   |                                         | entfernt                                    | DC-Java-API; im Original nur                   |
|   |                                         |                                             | zur Existenzprüfung verwendet, Ergebnis nicht  | 
|   |                                         |                                             | im PDF angezeigt → ersatzlos entfernbar        |
| 4 | `$pdfContent.linkCollectionByIssue(...)`| unverändert übernommen (Variante A)         | Better-PDF-eigenes Tool, vermutlich auch in    |
|   |                                         |                                             | Cloud verfügbar — **muss validiert werden**    |
| 5 | Logo-/Footer-URLs, QR-Code-Block        | unverändert                                 | reine HTTP(S)-Aufrufe, deployment-unabhängig   |
| 6 | `articles-tool.groovy`                  | unverändert                                 | keine Jira-Abhängigkeiten                      |

## Offene Validierungspunkte (in der Cloud-Sandbox zu testen)

### 1. `$pdfContent.linkCollectionByIssue()` — KRITISCH
Das ist exakt die offene Frage aus eurer Migrationsplanung. Testschritte:

1. Better PDF Exporter Cloud (Trial) in Sandbox installieren
2. `rechnung-common-elements-fo.vm` und `rechnung-in-issue-fo.vm` als neue PDF-Ressourcen anlegen
3. Auf einem Test-Issue im Projekt "Rechnungen" (mit bestehenden Issue-Links zu Auftrag/Region) den PDF-Export auslösen
4. Falls Fehler `Method linkCollectionByIssue not found` o.ä. auftritt → **Variante B** in `rechnung-common-elements-fo.vm` aktivieren (auskommentierten Block einkommentieren, REST-Helper-Script schreiben)
5. Falls Fehler bei `.linkTypes`, `.getInwardIssues()` oder `.getOutwardIssues()` → Methodensignatur in Cloud-Doku unter "Recipes" / "Scripting" mit Midori-Support klären

Die Methode `$pdfContent.linkCollectionByIssue()` stammt aus dem **Better PDF Exporter for Jira** von Midori, den Sie ja auch auf Ihrem Data Center System verwenden. [Kick-off Workshop 26.01.2026](https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433869/Kick-off+Workshop+26.01.2026)

Zur Ihrer Frage bezüglich der **Cloud-Version**:

**Die Cloud-Version des Better PDF Exporter unterscheidet sich erheblich von der Server/DC-Version:**

1. **Template-Engine**: In der Cloud-Version verwendet Midori **Groovy-Templates** statt Velocity-Templates. Das bedeutet, dass die gesamte Template-Syntax anders ist – `$pdfContent.linkCollectionByIssue()` als Velocity-Aufruf funktioniert so nicht direkt.

2. **API-Oberfläche**: Die verfügbaren Objekte und Methoden im Template-Kontext der Cloud-Version sind anders strukturiert. Einige Methoden wurden umbenannt, haben eine andere Signatur oder wurden durch neue Konzepte ersetzt.

3. **Issue Links in Cloud**: Für Issue Links in der Cloud-Version stellt Midori typischerweise andere Accessor-Methoden bereit (z.B. über `issue.fields.issuelinks` oder ähnliche Groovy-basierte Zugriffspfade).

**Empfehlung:**

Da die genaue Cloud-API-Dokumentation von Midori nicht in Ihren internen Dokumenten vorliegt, empfehle ich:

- Die **offizielle Midori-Dokumentation** für die Cloud-Version zu prüfen: 
[midori-global.com/products/better-pdf-exporter-for-jira-cloud]
(https://www.midori-global.com/products/better-pdf-exporter-for-jira-cloud/documentation)
- Midori bietet üblicherweise einen **Template Migration Guide** (Server/DC → Cloud) an, 
  der die Unterschiede in den verfügbaren Methoden auflistet
- Alternativ direkt den **Midori-Support** kontaktieren, um die genaue Cloud-Entsprechung von    `linkCollectionByIssue()` zu erfragen

Kurz gesagt: Die Methode **fehlt in dieser Form** in der Cloud-Version, da die gesamte Template-Architektur auf Groovy umgestellt wurde. Es gibt aber sehr wahrscheinlich eine funktionale Entsprechung mit anderer Syntax.

Quellen:

1. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433869/Kick-off+Workshop+26.01.2026

2. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433799/2024-06-26+Protokoll+AK-IT

3. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427509/Jira+Logfiles+analysieren+Jira+Admin+Ebene

4. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433745/2024-02-28+Protokoll+AK-IT

5. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427723/2025+01+Standard+Change+TIC-423+Cloning+rebuild

6. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=428309&preview=%2F428309%2F445175%2FWS+Confluence+Anwender.pdf

7. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=428309&preview=%2F428309%2F445175%2FWS+Confluence+Anwender.pdf

8. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427867/2024+03+Standard+Change+TIC-404+Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems

9. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433775/2025+10+Emergency+Change+Jira+Update+TIC-449

10. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433775/2025+10+Emergency+Change+Jira+Update+TIC-449

11. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433775/2025+10+Emergency+Change+Jira+Update+TIC-449

12. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433849/2026+012+Emergency+Change+Jira+Update+TIC-455

13. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433849/2026+012+Emergency+Change+Jira+Update+TIC-455

14. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433849/2026+012+Emergency+Change+Jira+Update+TIC-455

15. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433815/2024-07-31+Protokoll+AK-IT

16. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427735/2025+01+Standard+Change+TIC-423Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems

17. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433867/2026+02+Standard+Change+Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems+ohne+L+schen+Server

18. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/428019/Jira+Debuggen+von+E-Mail-Sendeproblemen+SMTP

19. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/428019/Jira+Debuggen+von+E-Mail-Sendeproblemen+SMTP

20. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/428019/Jira+Debuggen+von+E-Mail-Sendeproblemen+SMTP

21. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427419/Jira+User+Sessions+gerade+aktive+User+anzeigen

22. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=433791&preview=%2F433791%2F513632%2FJiraSkills20250625.pptx

23. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/434079/PG+-+Cloud-Migration+Atlassian+DataCenter

24. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427827/07+Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems

25. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427473/2025+07+Emergency+Change+TIC-434+427

26. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/434174/Jira+User+Online+Status+ermitteln

27. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433721/2024-01-17+Protokoll+AK-IT

28. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433763/2026-01-29+Protokoll+AK-IT

29. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433859/2025+12+Emergency+Change+TIC-453

30. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427791/Jira+User+Zugang+reglementieren+im+Maintenance+Mode

31. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427791/Jira+User+Zugang+reglementieren+im+Maintenance+Mode

32. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/434019/Jira+Crowd+user+Synchronisation

33. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433869/Kick-off+Workshop+26.01.2026

34. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433869/Kick-off+Workshop+26.01.2026

35. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/428083/Better+Content+Archiving+-+Konfiguration+eMail+expired

36. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/428083/Better+Content+Archiving+-+Konfiguration+eMail+expired

37. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/428083/Better+Content+Archiving+-+Konfiguration+eMail+expired

38. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427573/Fehler+In+VAMPIR+erzeugtes+PDF+ist+leer

39. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=430138&preview=%2F430138%2F548303%2FMitgliedsstatistik202506.pdf

40. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=429157&preview=%2F429157%2F523131%2FStatistik.pdf

41. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=429283&preview=%2F429283%2F523224%2FStatistik.pdf

42. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=430138&preview=%2F430138%2F548198%2FJubilaeumsliste202504.pdf

43. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=430138&preview=%2F430138%2F547400%2FJubilaeumsliste202509.pdf

44. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=430138&preview=%2F430138%2F547652%2FJubilaeumsliste202501.pdf

45. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/zInformati/pages/429900/Verlinkung+bombenfest+Perma-Link+berdauert+auch+nderungen+von+Seitennamen

46. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/zInformati/pages/429900/Verlinkung+bombenfest+Perma-Link+berdauert+auch+nderungen+von+Seitennamen

47. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=435731&preview=%2F435731%2F516263%2FMarion+Schultz.pdf

48. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433660/Content+Management+Werkbank

49. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433660/Content+Management+Werkbank

50. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/433660/Content+Management+Werkbank

51. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427555/Linux+Nutzung+privilegierter+Zugang+f+r+Web+Content+Management

52. https://aktivsenioren-sandbox.atlassian.net/wiki/spaces/AR/pages/427555/Linux+Nutzung+privilegierter+Zugang+f+r+Web+Content+Management

53. https://aktivsenioren-sandbox.atlassian.net/wiki/pages/viewpageattachments.action?pageId=436605&preview=%2F436605%2F517427%2F230428_Coffeetalk.pdf

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


