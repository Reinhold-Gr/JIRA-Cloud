https://aktivsenioren.atlassian.net/wiki/home

Wie kann ich PDF-Templates von DC in die Cloud migrieren?






Hier ist eine strukturierte Analyse der Migrationswege und der Frage, ob sich Better PDF vermeiden lässt.

---

## Migration von PDF-Templates: Jira DC → Jira Cloud

### Kernsituation: Was Better PDF (Midori) in der Cloud ist

Better PDF Exporter ist für alle drei Jira-Deployment-Typen verfügbar: Server (Legacy), Data Center und Cloud. Während Server und Data Center dieselben App-Features teilen, basiert die Cloud-Version auf einer anderen Infrastruktur und REST API, was sie in wesentlichen Bereichen anders funktionieren lässt. Die App wurde von Grund auf neu entwickelt.

Das heißt: Die App *existiert* in der Cloud – aber sie ist **nicht dieselbe App**.

---

### Was bei der Migration konkret passiert

#### Wenn du deine Templates **nicht** customized hast:
Wenn du keine Ressourcen auf dem Server angepasst hast, gibt es bei der Cloud-Migration absolut nichts zu tun. Einfach Better PDF Exporter für Jira Cloud installieren – alle Standard-Ressourcen werden automatisch erstellt, genauso wie in der Server-Version.

#### Wenn du Templates customized hast (dein Fall):
Bei customized Default-Ressourcen oder eigenen Templates musst du manuell migrieren: Den Inhalt des angepassten Templates mit dem entsprechenden Cloud-Default vergleichen und alle Änderungen zusammenführen. Zum Beispiel: Wenn du `issue-fo.vm` im Server angepasst hast, musst du das Default-`issue-fo.vm` der Cloud-Version finden und die Änderungen mergen.

Das ist der **kritische Punkt bei euch** – eure `.vm`-Templates (Velocity/XSL-FO), Groovy-Scripte und die Tempo-Integration müssen manuell portiert werden.

---

### Technische Unterschiede DC vs. Cloud, die Templates betreffen

Jira Cloud Apps integrieren sich via Atlassian Connect bzw. jetzt Forge. Wenn die Jira REST API bestimmte Informationen nicht bereitstellt, die für Server/DC-Apps verfügbar wären, kann die Cloud-Version die entsprechenden Features nicht implementieren. Das lässt sich seitenns der App nicht umgehen – die fehlenden Features können erst ergänzt werden, wenn Atlassian die Einschränkung auf der Connect/Forge-Seite aufhebt.

Konkret für eure Tempo-Worklog-Integration: Die Cloud-Version unterstützt die Tempo Timesheets Cloud REST API – aber das bedeutet, dass Groovy-Skripte, die auf DC-internen Java-APIs zugreifen (z.B. `ComponentAccessor`), in der Cloud **nicht funktionieren**. Alles muss auf REST-basierte Aufrufe umgestellt werden.

Die Cloud-Version hat auf Forge migriert (Atlassians serverlose App-Entwicklungsplattform). Außerdem unterstützt die Cloud-Version ab v4.3.0 die Tempo v4 REST API.

---

### Kann man Better PDF *vermeiden*?

Das hängt von euren konkreten Anforderungen ab:

| Anforderung | Ohne Better PDF möglich? |
|---|---|
| Einfacher Issue-Export als PDF | ✅ Jira Cloud hat nativen „Print list → Save as PDF"-Export |
| Custom-Layout mit Firmenlogo, Felder wählen | ❌ Nein |
| Tempo-Worklog-Daten im PDF | ❌ Nein |
| Automatisierter PDF-Versand per Mail | ❌ Nein |
| Dashboard-Export als PDF | ❌ Nein (kein anderes Marketplace-Tool kann das laut Midori) |

**Fazit:** Wenn ihr aktuell customized Templates mit Tempo-Integration und spezifischem Layout nutzt, gibt es **kein realistisches Alternativ-Tool** im Atlassian Marketplace, das das alles abdeckt. Der native Jira-Cloud-Export ist zu rudimentär.

---

### Empfohlener Migrationsweg

**Phase 1 – Inventarisierung (jetzt, im DC)**
- Alle `.vm`-Dateien und `.groovy`-Scripte exportieren und dokumentieren
- Für jedes Template festhalten: Welche Felder, welche Logik, welche Tempo-Calls
- Velocity-Variablen identifizieren, die DC-spezifische Java-Objekte nutzen

**Phase 2 – Cloud-Sandbox aufsetzen**
- Better PDF Exporter Cloud (Trial) in der Sandbox installieren
- Default-Templates der Cloud-Version als Basis nehmen
- Diff zwischen DC-Custom-Template und Cloud-Default erstellen (z.B. mit WinMerge)

**Phase 3 – Template-Portierung**
- Velocity-Syntax bleibt großteils identisch – das ist der Vorteil
- Groovy-Scripte müssen von DC-Java-APIs auf REST-Calls umgestellt werden
- Tempo: DC nutzt evtl. ältere Tempo-Java-APIs → auf Tempo v4 REST API migrieren (Cloud-Pflicht)
- Custom-Field-Referenzen prüfen: IDs können sich nach Migration ändern

**Phase 4 – PDF Views konfigurieren**
Die Konfigurationsoptionen sind zu 90 % identisch in Cloud und Server – manche Optionen fehlen in Cloud. Die Migration ist daher in diesem Bereich trivial.

---

### Kurzes Risiko-Fazit

Das größte Risiko ist nicht die App-Verfügbarkeit (Better PDF existiert in Cloud), sondern die **Groovy-Script-Migration**: Was auf DC direkt gegen die Jira/Tempo Java-APIs lief, muss in der Cloud vollständig auf REST-Calls umgebaut werden. Das ist kalkulierbar, aber Aufwand – vor allem bei komplexen Tempo-Worklog-Abfragen.

Willst du, dass ich für ein konkretes eurer DC-Groovy-Scripts einen Cloud-kompatiblen Entwurf erarbeite?

# Antwort von ROVO

Das ist eine wichtige Frage für eure laufende Cloud-Migration. Basierend auf euren Migrationsdokumenten und den Atlassian-Unterschieden zwischen DC und Cloud gibt es hier zwei Bereiche zu beachten:

## Confluence PDF-Export-Templates

In Confluence DC können PDF-Exporte über **Space-Level CSS-Stylesheets** angepasst werden (Bereichsadministration → Design → PDF-Stylesheet). Diese kundenspezifischen PDF-Stylesheets werden vom **Confluence Cloud Migration Assistant nicht automatisch migriert**. In Confluence Cloud ist die Anpassung des PDF-Exports deutlich eingeschränkter:

- **Benutzerdefinierte CSS-Stylesheets** für den PDF-Export gibt es in Cloud in dieser Form nicht mehr
- Die Kopf-/Fußzeilen-Konfiguration funktioniert anders – in euren Testprotokollen war dies bereits als „n/a (nicht konfiguriert)" vermerkt. [WB-Check-Upgrade Testserver](https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804673&preview=%2F26804673%2F26806397%2FWB-Check-Upgrade%2BTestserver%2BAtlassian%2BSoftware-08-12-23.docx)

**Empfohlene Vorgehensweise:**
1. Alle bestehenden PDF-Stylesheets aus DC dokumentieren/exportieren (als Referenz)
2. In Cloud prüfen, welche Formatierungen über die nativen Cloud-Optionen abbildbar sind
3. Für erweiterte Anforderungen eine Marketplace-App wie **Scroll PDF Exporter** (K15t) in der Cloud-Version evaluieren – diese bietet ähnliche Template-Anpassungen wie in DC

## Jira PDF-Templates (Better PDF Exporter / XPorter)

Laut eurer Migrationsdokumentation nutzt ihr den **Better PDF Exporter** (Midori), der als „Must Have"-App mit dokumentiertem Migrationspfad gelistet ist → **JSM Cloud Form PDFs**. [Atlassian Cloud Migration Jira Activities](https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx)

- Die bestehenden Word/Groovy-basierten Templates aus DC müssen für die Cloud-Version **neu erstellt oder angepasst** werden
- Midori bietet eine Cloud-Version des Better PDF Exporters – die Template-Syntax unterscheidet sich aber teilweise
- Für **XPorter** (das ihr für Rechnungs-PDFs im Proof-of-Concept getestet habt): auch hier gibt es eine Cloud-Version, bei der die Word-Templates grundsätzlich weiterverwendbar sind, aber Anpassungen nötig sein können

## Konkrete nächste Schritte

1. **Inventar erstellen**: Alle PDF-Templates in DC auflisten (Confluence-Stylesheets + Jira-App-Templates)
2. **Cloud-Äquivalente identifizieren**: Für jedes Template prüfen, ob es in Cloud nativ oder per App abbildbar ist
3. **Templates neu erstellen**: Die meisten Templates müssen in der Cloud-Umgebung manuell nachgebaut werden
4. **Testen auf dem Cloud-Testsystem**: Die neu erstellten Templates auf eurem bereitgestellten Testsystem validieren

Soll ich die Details zu einem bestimmten Bereich vertiefen – z.B. speziell zur Rechnungs-PDF-Migration mit XPorter/Better PDF Exporter, oder eher zu den Confluence-Seiten-Export-Optionen in der Cloud?

Quellen:

1. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx

2. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx

3. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx

4. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804970/PG+-+Cloud-Migration+Atlassian+DataCenter

5. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807799%2FConfluence+Cloud+Xwiki.docx

6. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807799%2FConfluence+Cloud+Xwiki.docx

7. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807799%2FConfluence+Cloud+Xwiki.docx

8. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804970&preview=%2F26804970%2F26807857%2FIssue-Liste+Cloud-Migration-Werkbank.csv

9. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804806/2025-09-30+Protokoll+AK-IT

10. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807794%2FPr%C3%A4sentation.pptx

11. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804610/2025-11-26+Protokoll+AK-IT

12. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804610/2025-11-26+Protokoll+AK-IT

13. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804863/Kick-off+Workshop+26.01.2026

14. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804863/Kick-off+Workshop+26.01.2026

15. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804789/2024-01-17+Protokoll+AK-IT

16. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804726/Cloud+Server+Snapshots

17. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804726/Cloud+Server+Snapshots

18. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804828/2024-06-26+Protokoll+AK-IT

19. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804666/Cloud-Speicher+des+AK-IT

20. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804666/Cloud-Speicher+des+AK-IT

21. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804752/2023+11+Emergency+Change+TIC-391

22. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804259/Personelles+zur+Plattform

23. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804596/Better+Content+Archiving+-+Konfiguration+eMail+expired

24. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804596/Better+Content+Archiving+-+Konfiguration+eMail+expired

25. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804250/Cloud+Server+Status+bersicht

26. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804352/Erzeugen+des+Testservers+aus+dem+Backup+des+Produktivsystems+Wiederverwendung+Server

27. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804425/2025+07+Change+TIC-427

28. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804258/2025+08+Emergency+Change+TIC-441+Package+Update+Ubuntu

29. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804862/2026+02+Standard+Change+Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems+ohne+L+schen+Server

30. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804673&preview=%2F26804673%2F26806397%2FWB-Check-Upgrade%2BTestserver%2BAtlassian%2BSoftware-08-12-23.docx

31. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807794%2FPr%C3%A4sentation.pptx

32. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804810/2026-01-29+Protokoll+AK-IT

33. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804810/2026-01-29+Protokoll+AK-IT

34. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804810/2026-01-29+Protokoll+AK-IT

35. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804709&preview=%2F26804709%2F26805036%2FWS+Confluence+Anwender.pdf

36. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804709&preview=%2F26804709%2F26805036%2FWS+Confluence+Anwender.pdf

37. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804709&preview=%2F26804709%2F26805036%2FWS+Confluence+Anwender.pdf

38. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804970&preview=%2F26804970%2F26807857%2FIssue-Liste+Cloud-Migration-Werkbank.csv

39. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804863/Kick-off+Workshop+26.01.2026

40. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804225/2023+02+Upgrade+Plattform+TIC-332

41. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804511/2022+11+Upgrade+Plattform

42. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804516/2023+11+Upgrade+Plattform+TIC-390+zur+ckgestellt

43. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804284/2026-02-26+Agenda+AK-IT

44. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804801/2024-02-28+Protokoll+AK-IT

45. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804673&preview=%2F26804673%2F26806398%2FWB-Check-Upgrade-Testserver-Atlassian-Software-18-01-24.docx

46. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804792/X-Rechnung

47. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804442/Erstellen+einer+klickbaren+Map+mit+Makro+Smart+Images


Hier ist eine detaillierte Übersicht zur Rechnungs-PDF-Migration basierend auf eurer Dokumentation:

## Euer aktueller Rechnungs-PDF-Workflow in VAMPIR (DC)

Laut der [VAMPIR Intern Doku](https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805337%2FVAMPIR_Intern_Doku.docx) läuft die Rechnungserstellung über den **Better PDF Exporter (Midori)** mit Velocity-Templates (`.vm`-Dateien) in einem mehrstufigen automatisierten Workflow:

1. **Regel 03** – Rechnung anlegen (nur Entwurf, kein PDF)
2. **Regel 1** – Rechnung-PDF erstellen via Template `rechnung-in-issue-fo.vm`
3. **Regel 2** – Rechnung-PDF senden (erstellt PDF erneut, versendet per E-Mail, sperrt Editierung)
4. **Regel 3** – Storno-PDF erstellen via Template `rechnung-storno-in-issue-with-current-rechnung-fo.vm`
5. **Regel 4** – Storno-PDF senden

Diese Regeln sind eng mit den **Jira Automation Rules** und **Workflow-Transitionen** verknüpft (z.B. setzt Regel 2 den Status auf „Rechnung gesendet" und friert alle Rechnungsdaten ein via `jira.issue.editable`).

## XPorter – Ergebnis der Evaluation

XPorter wurde 2024 als Alternative evaluiert. NB hat einen Proof-of-Concept erstellt mit einem Word-Template „Test-Rechnung" inkl. Variablen und AS-Briefkopf. Das Ergebnis laut [Protokoll 31.07.2024](https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804836/2024-07-31+Protokoll+AK-IT):

> **„Funktionalität ist leider nicht vollständig – deshalb ist ein Einsatz bei uns nicht möglich."**

XPorter wurde daher als vollständiger Ersatz verworfen.

## Better PDF Exporter – Migrationspfad in die Cloud

Laut eurem [Kick-off Workshop](https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804863/Kick-off+Workshop+26.01.2026) und der [Jira Activities-Dokumentation](https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx):

- Better PDF Exporter ist als **„Must Have"** klassifiziert (Kosten: 7.060 $ → ca. 1.553 € bei Non-Profit-Rabatt)
- Dokumentierter Migrationspfad: **„Better PDF Exporter – Documented Migration – Differences → JSM Cloud Form PDFs"**

### Was sich ändert

| Aspekt | DC (aktuell) | Cloud |
|---|---|---|
| **Template-Engine** | Apache Velocity (`.vm`-Dateien) | Freemarker-basiert (neue Syntax) |
| **Automation-Integration** | Über „Better PDF Automation for Jira" + ScriptRunner | Cloud-native Automation-Anbindung (anders konfiguriert) |
| **Workflow-Properties** | `jira.issue.editable` zum Sperren der Felder | Workflow-Properties werden **nicht migriert** – muss in Cloud über Feld-Konfigurationen oder Automation nachgebaut werden |
| **E-Mail-Versand** | Über Automation Rules mit SMTP | Cloud-eigener E-Mail-Versand |

### Konkrete Herausforderungen

1. **Velocity-Templates müssen neu geschrieben werden** – Die `.vm`-Templates (`rechnung-in-issue-fo.vm`, `rechnung-storno-in-issue-with-current-rechnung-fo.vm`) sind nicht 1:1 kompatibel. Sie müssen in der Cloud-Version von Better PDF Exporter neu erstellt werden.

2. **Workflow-Eigenschaft `jira.issue.editable`** – Diese wird laut Migrations-Doku **nicht migriert**. Das Einfrieren der Rechnungsdaten nach dem Versand muss anders gelöst werden (z.B. über Feld-Berechtigungen oder Automation-basierte Sperren).

3. **Automation Rules** – Eure Regeln 1–4 nutzen teilweise „unsupported components" (Workflow Properties, Triggers). Diese müssen in Cloud Automation neu aufgebaut werden.

4. **E-Rechnung (ab 2028)** – Laut dem [AK-IT Protokoll vom 25.06.2025](https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804824/2025-06-25+Protokoll+AK-IT) sind externe Aufrufe zur XML-Erzeugung aus Jira Cloud nicht einfach möglich. HiQ empfiehlt dafür ein **ScriptRunner-Skript**, das sowohl XML-Generierung als auch PDF-Erstellung und E-Mail-Versand übernehmen könnte.

## Empfohlene Vorgehensweise

1. **Velocity-Templates dokumentieren** – Alle Platzhalter und Datenfelder aus den bestehenden `.vm`-Templates erfassen
2. **Better PDF Exporter Cloud installieren** – Auf eurem Testsystem die Cloud-Version installieren und die Templates in der neuen Syntax nachbauen
3. **Workflow-Sperr-Mechanismus** alternativ implementieren – Statt `jira.issue.editable` z.B. über Field Configurations oder Automation
4. **Automation Rules neu erstellen** – Die Regeln 1–4 in Cloud Automation nachbauen, da die DC-Automations teilweise nicht migriert werden
5. **E-Rechnung mitdenken** – Wenn ihr die Templates ohnehin neu baut, gleich die E-Rechnungs-Anforderung (XRechnung/ZUGFeRD) berücksichtigen, ggf. mit einem ScriptRunner-Skript wie von HiQ vorgeschlagen

Soll ich die Details zur Workflow-Property-Migration (`jira.issue.editable`) vertiefen, oder wäre es hilfreicher, die konkreten Template-Variablen aus den bestehenden `.vm`-Dateien zusammenzustellen?

Quellen:

1. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx

2. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx

3. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804828/2024-06-26+Protokoll+AK-IT

4. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804532&preview=%2F26804532%2F26805478%2FHowto_+restore+a+backup+of+Prod+server+%28Aktivsenioren%29-v8-20210702_112447.pdf

5. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804532&preview=%2F26804532%2F26805478%2FHowto_+restore+a+backup+of+Prod+server+%28Aktivsenioren%29-v8-20210702_112447.pdf

6. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804352&preview=%2F26804352%2F26807300%2FHowto_+restore+a+backup+of+Prod+server+%28Aktivsenioren%29-v8-20210702_112447.pdf

7. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804352&preview=%2F26804352%2F26807300%2FHowto_+restore+a+backup+of+Prod+server+%28Aktivsenioren%29-v8-20210702_112447.pdf

8. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804352&preview=%2F26804352%2F26807300%2FHowto_+restore+a+backup+of+Prod+server+%28Aktivsenioren%29-v8-20210702_112447.pdf

9. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805253%2FApps%26Versionen.xlsx

10. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804836/2024-07-31+Protokoll+AK-IT

11. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804836/2024-07-31+Protokoll+AK-IT

12. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804792/X-Rechnung

13. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804793/2024-05-29+Protokoll+AK-IT

14. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804793/2024-05-29+Protokoll+AK-IT

15. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804793/2024-05-29+Protokoll+AK-IT

16. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804224/2024-03-27+Protokoll+AK-IT

17. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805337%2FVAMPIR_Intern_Doku.docx

18. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805337%2FVAMPIR_Intern_Doku.docx

19. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805337%2FVAMPIR_Intern_Doku.docx

20. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150042&preview=%2F24150042%2F24150637%2FEGR+erechnung+U%CC%88bersicht.pdf

21. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150042&preview=%2F24150042%2F24150637%2FEGR+erechnung+U%CC%88bersicht.pdf

22. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150042&preview=%2F24150042%2F24150637%2FEGR+erechnung+U%CC%88bersicht.pdf

23. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804970/PG+-+Cloud-Migration+Atlassian+DataCenter

24. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804806/2025-09-30+Protokoll+AK-IT

25. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804863/Kick-off+Workshop+26.01.2026

26. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804863/Kick-off+Workshop+26.01.2026

27. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804801/2024-02-28+Protokoll+AK-IT

28. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804555/AG+Auswertungen

29. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804293/2024-11-28+Protokoll+AK-IT

30. https://aktivsenioren.atlassian.net/wiki/spaces/GES/pages/21954623/KI+-+Leitfaden+K+nstliche+Intelligenz

31. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804824/2025-06-25+Protokoll+AK-IT

32. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807794%2FPr%C3%A4sentation.pptx

33. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807794%2FPr%C3%A4sentation.pptx

34. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804792&preview=%2F26804792%2F26807426%2FChangeLog.docx

35. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804970&preview=%2F26804970%2F26807857%2FIssue-Liste+Cloud-Migration-Werkbank.csv

36. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804970&preview=%2F26804970%2F26807857%2FIssue-Liste+Cloud-Migration-Werkbank.csv

37. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804511/2022+11+Upgrade+Plattform

38. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804511/2022+11+Upgrade+Plattform

39. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804511/2022+11+Upgrade+Plattform

40. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804225/2023+02+Upgrade+Plattform+TIC-332

41. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804853/2026+012+Emergency+Change+Jira+Update+TIC-455

42. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804853/2026+012+Emergency+Change+Jira+Update+TIC-455

43. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804853/2026+012+Emergency+Change+Jira+Update+TIC-455

44. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804816/2025+10+Emergency+Change+Jira+Update+TIC-449

45. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804816/2025+10+Emergency+Change+Jira+Update+TIC-449

46. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804816/2025+10+Emergency+Change+Jira+Update+TIC-449

47. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804258/2025+08+Emergency+Change+TIC-441+Package+Update+Ubuntu

48. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804975/Jira+Logfile+analysieren+Linux+Ebene

49. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804858/2025+12+Emergency+Change+TIC-453

50. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804789/2024-01-17+Protokoll+AK-IT

51. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804291/2025+07+Emergency+Change+TIC-434+427

52. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804450/Jira+User+Zugang+reglementieren+im+Maintenance+Mode

53. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26805434/Jira+User+Online+Status+ermitteln

54. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804564/Jira+Debuggen+von+E-Mail-Sendeproblemen+SMTP


Basierend auf der umfangreichen Analyse eurer VAMPIR-Dokumentation, Protokolle und Fehlerberichte habe ich die in den Velocity-Templates verwendeten Felder und Variablen zusammengestellt.

## Templates im Überblick

Euer System nutzt drei zentrale `.vm`-Templates:
1. **`rechnung-in-issue-fo.vm`** → Rechnung-PDF
2. **`rechnung-storno-in-issue-with-current-rechnung-fo.vm`** → Storno-PDF
3. **`initial-confirmation-vo.vm`** → Empfangsbestätigung-PDF

---

## Variablen-Zusammenstellung für `rechnung-in-issue-fo.vm`

### 1. Klient/Kunde-Daten (aus Auftrag)

| Velocity-Variable (typisch) | VAMPIR-Feld | Hinweise |
|---|---|---|
| `${issue.customFields.K-Vorname}` | K.Vorname | Klient Vorname |
| `${issue.customFields.K-Nachname}` | K.Nachname | Klient Nachname |
| `${issue.customFields.K-Adresse}` | K.Adresse / Rechnungsadresse | Kann bei Jobcenter-Aufträgen auf Jobcenter-Adresse geändert werden |
| `${issue.customFields.K-Rechnung-E-Mail}` | K.Rechnung-E-Mail | Unterstützt 2 Adressen per Komma-Trennung (z.B. `name1@web.de,name2@gmail.de`) |
| `${issue.customFields.K-Ort}` | K.Ort / Stadt | Wohnort des Klienten |

### 2. Rechnungsdaten (aus Rechnung-Vorgang)

| Velocity-Variable (typisch) | VAMPIR-Feld | Hinweise |
|---|---|---|
| Rechnungsnummer | Rechnungsnummer | Format aktuell: `yy-RRR-NNNN` (z.B. `19-107-0639`), ab 2026 fortlaufend ab 26000 |
| Rechnungsbetrag | Rechnungsbetrag | Kann 0 € sein bei „Eigenaufträgen". **Darf nicht leer sein** → sonst leeres PDF! |
| MwSt | MwSt-Satz / MwSt-Betrag | Muss mit Auftragsart zusammenpassen (sonst Fehler beim Datev-Export) |
| Rechnungsdatum | Rechnungsdatum | Wird für Bezahlstatus-Prüfung und Datev-Export verwendet |
| `Rechnung comment field 2` | Rechnung comment field 2 | Freitext-Feld für individuelle Rechnungshinweise (z.B. Jobcenter-Klientenzuordnung) |
| Bezahlstatus | Bezahlstatus (BEZSTAT) | Status der Zahlung |

### 3. Auftragsdaten (aus verknüpftem Auftrag)

| Velocity-Variable (typisch) | VAMPIR-Feld | Hinweise |
|---|---|---|
| `{{Auftragsbeginn.format("dd.MM.yyyy")}}` | Auftragsbeginn | Datumsfeld mit `.format()`-Funktion. Ersetzt das ursprüngliche `{{now}}` |
| Auftragsnummer | Auftragsnummer | Eigene Nummer + systemgenerierter Schlüssel (z.B. `AUFTRAG-XXXX`) |
| Auftragsart | Auftragsart / Beratungsfall | Gründung, Beratung, etc. – beeinflusst MwSt-Zuordnung und Sphäre (Zweckbetrieb/Geschäftsbetrieb) |
| `Auftrag mail comment` | Auftrag mail Comment | Text für Auftragsbestätigung |
| `Auftrag mail Comment 2` | Auftrag mail Comment 2 | Zusatztext für Auftragsbestätigung |

### 4. Regionalleiter/Region-Daten (aus verknüpfter Region)

| Velocity-Variable (typisch) | VAMPIR-Feld | Hinweise |
|---|---|---|
| RL-Name | RL Vorname, RL Familienname | Seit 2024 reduziert auf Vorname + Nachname + AS-E-Mail (keine Privatadresse mehr). [Protokoll AK-IT 30.09.2025](https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804806/2025-09-30+Protokoll+AK-IT) |
| RL-E-Mail | RL AS-E-Mail | AktivSenioren-E-Mail des Regionalleiters |

### 5. Vereinsangaben / Footer (aus benutzerdefinierten Feld-Standardwerten)

| Velocity-Variable (typisch) | VAMPIR-Feld | Hinweise |
|---|---|---|
| Vereinsname | Custom Field (Standardwert) | AktivSenioren Bayern e.V. |
| Vereinsadresse | Custom Field (Standardwert) | Landshuter Allee 11, 80637 München |
| Telefon | Custom Field (Standardwert) | 089-22 22 37 |
| Gesetzl. Vertreter | Custom Field (Standardwert) | Seit 11/2024: Link auf `www.aktivsenioren.de/impressum` statt Namensliste. [Protokoll AK-IT 28.11.2024](https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804293/2024-11-28+Protokoll+AK-IT) |
| Social-Media-Links | Custom Field (Standardwert) | Xing-Link wurde entfernt |

> **Design-Prinzip:** Die Vereinsangaben werden bewusst über benutzerdefinierte Felder mit Standardwerten eingespielt, damit bei Änderungen (Adresse, Vorstand) nicht alle Templates manuell geändert werden müssen, sondern die Änderung an einer Stelle erfolgt. [VAMPIR_Intern_Doku.docx](https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805337%2FVAMPIR_Intern_Doku.docx)

### 6. Storno-spezifische Felder (nur in `rechnung-storno-...fo.vm`)

| Variable | VAMPIR-Feld | Hinweise |
|---|---|---|
| Stornotext | Stornotext | Wird beim Übergang „Rechnung gesendet" → „Stornotext editiert" bearbeitbar. Kann mehrfach geändert werden bevor Storno-PDF erzeugt wird |
| Stornodatum | Stornodatum | Für Datev-Export relevant |
| Original-Rechnungsdaten | Alle Felder der Original-Rechnung | Template zeigt sowohl Storno als auch Original-Rechnung |

---

## Kritische Felder (leeres PDF bei fehlendem Inhalt)

Laut der [Fehlerdokumentation](https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804341/Fehler+In+VAMPIR+erzeugtes+PDF+ist+leer) bricht der Parser **ohne Fehlermeldung** ab, wenn bestimmte Felder leer sind. Besonders kritisch sind:
- **Betragsfelder** (Rechnungsbetrag) – „0" eingeben statt leer lassen
- **Auftragstyp-abhängige Felder** – werden normalerweise automatisch befüllt

---

## Implikation für die Cloud-Migration

Beim Nachbau der Templates in der Cloud-Version des Better PDF Exporters müssen:

1. **Alle oben genannten Custom Fields** im Cloud-Jira nachgebildet werden – die Feldnamen und IDs ändern sich bei der Migration
2. **Die Velocity-Syntax** (`${...}`, `{{...format(...)}}`) muss in die Cloud-Template-Syntax übersetzt werden (Freemarker)
3. **Das Prinzip „Vereinsangaben über Standardwerte"** kann in Cloud anders gelöst werden müssen (z.B. über globale Variablen oder ein dediziertes Config-Feld)
4. **Die Null-Check-Logik** (leere Felder → leeres PDF) sollte in Cloud durch explizite Validierung ersetzt werden

Soll ich daraus eine Confluence-Seite erstellen, die als Migrations-Checkliste für die Template-Umstellung dient?

Quellen:

1. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805337%2FVAMPIR_Intern_Doku.docx

2. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805337%2FVAMPIR_Intern_Doku.docx

3. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805337%2FVAMPIR_Intern_Doku.docx

4. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804792/X-Rechnung

5. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804341/Fehler+In+VAMPIR+erzeugtes+PDF+ist+leer

6. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804555/AG+Auswertungen

7. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804555/AG+Auswertungen

8. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804792&preview=%2F26804792%2F26807414%2FDokumentation_2025-09-01.docx

9. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804792&preview=%2F26804792%2F26807480%2FDokumentation_2025-11-01.docx

10. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804245/VAMPIR+Ticket-Darstellung+Bearbeitung

11. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804245/VAMPIR+Ticket-Darstellung+Bearbeitung

12. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804405/Regelm+ige+Administrationsaufgaben

13. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047686/Rechnungsversand+aus+VAMPIR+an+zwei+Mailadressen

14. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804409/VAMPIR+CSV+Massendatenimport+Auftragsnummer+gezielt+setzen

15. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804409/VAMPIR+CSV+Massendatenimport+Auftragsnummer+gezielt+setzen

16. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=30212490&preview=%2F30212490%2F30213575%2F230307+Auswertung+VAMPIR+2022.pdf

17. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804524/VAMPIR+Ticket-Typen+und+ihre+Benutzung

18. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804920/AG+Vampir

19. https://aktivsenioren.atlassian.net/wiki/spaces/ADB/pages/24150035/VVT+Verein+Vampir+Tutorials

20. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150030&preview=%2F24150030%2F24150542%2FDB02+Bedienungshandbuch+Teil+2+System-Mitgliederverwaltung.pdf

21. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150030&preview=%2F24150030%2F24150542%2FDB02+Bedienungshandbuch+Teil+2+System-Mitgliederverwaltung.pdf

22. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150030&preview=%2F24150030%2F24150542%2FDB02+Bedienungshandbuch+Teil+2+System-Mitgliederverwaltung.pdf

23. https://aktivsenioren.atlassian.net/wiki/spaces/BWAK/pages/19071006/VAMPIR+Auftragsbearbeitung+-+Akademie

24. https://aktivsenioren.atlassian.net/wiki/spaces/BWAK/pages/19071006/VAMPIR+Auftragsbearbeitung+-+Akademie

25. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047126/Einf+hrung+neuer+Regionalleiter+RL

26. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804386/Jira+VAMPIR+API+Token+generieren

27. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804704/VAMPIR+Ticketbearbeitung+Prozess+Arbeitsmodell

28. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804508/VAMPIR+Ticketbearbeitung+Entscheidungsprozess+bei+Changes+Typ+Feedback

29. https://aktivsenioren.atlassian.net/wiki/spaces/GES/pages/21954591/Leitfaden+EGR+Auftragsabschluss

30. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804825/Dokumentation-User+VAMPIR

31. https://aktivsenioren.atlassian.net/wiki/spaces/GES/pages/21954593/Leitfaden+EGR+kompakt

32. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26805744/VAMPIR+Ticket-Zielobjekte+betroffene+Systemkomponenten

33. https://aktivsenioren.atlassian.net/wiki/spaces/GES/pages/21954616/Leitfaden+EGR+Erstgespr+ch

34. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804657/Dokumentation-Test+VAMPIR

35. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804959/VAMPIR+Attachment+Upload+Limit+ndern

36. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26805741/VAMPIR+Ticket-Priorit+ten+und+ihre+Bedeutung

37. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047505/Rahmenauftr+ge

38. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804405/Regelm+ige+Administrationsaufgaben

39. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804405/Regelm+ige+Administrationsaufgaben

40. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804422/2025+01+Standard+Change+TIC-423Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems

41. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804697/KI+Anwendungen+f+r+Klienten

42. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804824/2025-06-25+Protokoll+AK-IT

43. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804824/2025-06-25+Protokoll+AK-IT

44. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804824/2025-06-25+Protokoll+AK-IT

45. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804840/2025-07-31+Protokoll+AK-IT

46. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804840/2025-07-31+Protokoll+AK-IT

47. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804840/2025-07-31+Protokoll+AK-IT

48. https://aktivsenioren.atlassian.net/wiki/spaces/GES/pages/21954623/KI+-+Leitfaden+K+nstliche+Intelligenz

49. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804416/2025+01+Standard+Change+TIC-423+Cloning+rebuild

50. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804793/2024-05-29+Protokoll+AK-IT

51. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804806/2025-09-30+Protokoll+AK-IT

52. https://aktivsenioren.atlassian.net/wiki/spaces/BWAK/pages/19071004/Basiswissen+IT+-+Akademie

53. https://aktivsenioren.atlassian.net/wiki/spaces/BWAK/pages/19071004/Basiswissen+IT+-+Akademie

54. https://aktivsenioren.atlassian.net/wiki/spaces/PROJ/pages/18284551/Partner+in+Vampir+CRM

55. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150056&preview=%2F24150056%2F24150362%2FITW+Training+VAMPIR.pdf

56. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804516/2023+11+Upgrade+Plattform+TIC-390+zur+ckgestellt

57. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804500/2025-10-29+Protokoll+AK-IT

58. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804293/2024-11-28+Protokoll+AK-IT

59. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804225/2023+02+Upgrade+Plattform+TIC-332

60. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=30212495&preview=%2F30212495%2F30215106%2FAuftragsverwaltung-Region+M%C3%BCnchen-1-10-2025-konsolidiert.pdf

61. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=30212495&preview=%2F30212495%2F30215106%2FAuftragsverwaltung-Region+M%C3%BCnchen-1-10-2025-konsolidiert.pdf

62. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=30212495&preview=%2F30212495%2F30215106%2FAuftragsverwaltung-Region+M%C3%BCnchen-1-10-2025-konsolidiert.pdf

63. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804409/VAMPIR+CSV+Massendatenimport+Auftragsnummer+gezielt+setzen

64. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804792&preview=%2F26804792%2F26807480%2FDokumentation_2025-11-01.docx

65. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804792&preview=%2F26804792%2F26807414%2FDokumentation_2025-09-01.docx

66. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804704/VAMPIR+Ticketbearbeitung+Prozess+Arbeitsmodell

67. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047206/07_VAMPIR+Doku+Auftragssystem

68. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047416/49.+FK+29.11.2023

69. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047416/49.+FK+29.11.2023

70. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047151/Leitlinien+f+r+die+Auftragsbearbeitung

71. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047419/50.+FK+22.01.2024+-+Agenda

72. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047419/50.+FK+22.01.2024+-+Agenda

73. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047314/Datumsangaben+in+VAMPIR

74. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804875/2025+12+Change+TIC-452+Jahreswechsel

75. https://aktivsenioren.atlassian.net/wiki/spaces/REG/pages/30212392/Auftragsablauf

76. https://aktivsenioren.atlassian.net/wiki/spaces/REG/pages/30212392/Auftragsablauf

77. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047265/56.+FK+21.07.2025+-+Protokoll

78. https://aktivsenioren.atlassian.net/wiki/spaces/GES/pages/21954561/Leitfaden+EGR+Info+Jobcenter


Die Migrations-Checkliste wurde als **Confluence-Datenbank** erstellt mit allen Aufgaben, gegliedert in die Bereiche:

- **Vorbereitung** – Templates sichern, Custom Fields und Variablen dokumentieren
- **Installation** – Better PDF Exporter Cloud einrichten, Felder anlegen
- **Template-Migration** – Alle drei `.vm`-Templates in Freemarker-Syntax nachbauen
- **Automation** – Regeln 1–4 neu erstellen, Ersatz für `jira.issue.editable`
- **Test** – Alle Szenarien durchspielen (Null-Betrag, Jobcenter, Datev-Export)
- **E-Rechnung** – ScriptRunner-Skript für XRechnung/ZUGFeRD (ab 2028)
- **Abnahme** – Fachbereich + Buchhaltung + Go-Live-Freigabe

Die Datenbank hat drei Ansichten: **Alle Aufgaben** (Tabelle), **Nach Status** (Board) und **Nach Bereich** (Board), sodass ihr den Fortschritt flexibel verfolgen könnt.

Quellen:

Hier ist die detaillierte Analyse zur Migration der Workflow-Property `jira.issue.editable` basierend auf eurer Dokumentation:

## Was `jira.issue.editable` in VAMPIR DC heute macht

In eurem VAMPIR-Rechnungsworkflow ist `jira.issue.editable = false` die zentrale Schutzfunktion, die nach dem Rechnungsversand greift. Konkret wird diese Property im Workflow-Übergang zum Status **„Rechnung gesendet"** gesetzt und bewirkt, dass **alle Felder des Rechnungs-Vorgangs eingefroren werden** – Rechnungsbetrag, Rechnungsnummer, MwSt, Klientendaten etc. können nicht mehr verändert werden. Das ist geschäftskritisch für:

- **Buchhaltungsintegrität** – Rechnungsdaten müssen nach Versand unveränderlich sein (Datev-Export)
- **Storno-Konsistenz** – Das Storno-PDF referenziert die Original-Rechnungsdaten, die identisch bleiben müssen
- **Compliance** – Rechnungsdaten dürfen nach Versand nicht rückwirkend manipuliert werden

## Das Migrations-Problem

Laut eurer [Jira Cloud Migration Activities-Dokumentation](https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx) wird diese Property **explizit nicht migriert**:

> *„Workflow functions – Properties / Triggers: Rechnung gesendet-Eigenschaft → jira.issue.editable"* — **wird nicht migriert**

Jira Cloud unterstützt `jira.issue.editable` als Workflow-Property **grundsätzlich nicht**. Es gibt in Cloud kein direktes 1:1-Äquivalent, das auf Workflow-Ebene alle Felder eines Vorgangs gleichzeitig sperrt.

## Vier Lösungsansätze für Jira Cloud

### Ansatz 1: Statusbasierte Feld-Konfigurationen (nativ, kein Add-on)

Jira Cloud erlaubt es, über **Bildschirmmasken pro Workflow-Status** zu steuern, welche Felder sichtbar/editierbar sind:
- Für den Status „Rechnung gesendet" eine eigene Bildschirmmaske erstellen, die alle Rechnungsfelder als **schreibgeschützt** definiert
- **Vorteil**: Native Cloud-Funktion, keine zusätzliche App nötig
- **Nachteil**: Schreibgeschützte Felder können in Cloud-Bildschirmmasken nur bedingt erzwungen werden – Felder können ausgeblendet, aber nicht immer explizit „read-only" gesetzt werden. Erfordert sorgfältige Konfiguration

### Ansatz 2: Jira Automation Rules (nativ)

Eine Cloud-Automation-Regel, die bei jeder Änderung eines Rechnungs-Vorgangs im Status „Rechnung gesendet" prüft und Änderungen automatisch rückgängig macht:
- **Trigger**: „Wenn Feld geändert wird" auf den kritischen Rechnungsfeldern
- **Bedingung**: Status = „Rechnung gesendet"
- **Aktion**: Feld auf den vorherigen Wert zurücksetzen + Benachrichtigung an den Benutzer
- **Vorteil**: Kein Add-on nötig, volle Cloud-Kompatibilität
- **Nachteil**: Reaktiv statt präventiv – die Änderung passiert kurz und wird dann rückgängig gemacht. Erfordert eine Regel pro zu schützendem Feld oder ein ScriptRunner-Skript

### Ansatz 3: ScriptRunner for Jira Cloud (empfohlen von HiQ)

ScriptRunner ist als **Must-Have-App** in eurer Migrationsliste enthalten (Adaptavist, 350 $). In der Cloud-Version bietet ScriptRunner:
- **Behaviours/Listeners**: Können Felder dynamisch auf read-only setzen basierend auf dem Workflow-Status
- **Script-Validatoren**: Können im Workflow-Übergang prüfen, ob geschützte Felder verändert wurden, und die Transition blockieren
- **Vorteil**: Am nächsten am DC-Verhalten – präventive Sperre, nicht reaktiv. HiQ hat im [AK-IT Protokoll vom 25.06.2025](https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804824/2025-06-25+Protokoll+AK-IT) bereits ein ScriptRunner-Skript empfohlen, das PDF-Erstellung, XML-Generierung und E-Mail-Versand kombiniert – das Feldsperr-Verhalten kann dort integriert werden
- **Nachteil**: Abhängigkeit von einer Drittanbieter-App, Cloud-ScriptRunner hat einen eingeschränkteren Funktionsumfang als die DC-Version

### Ansatz 4: Feld-Berechtigungen über Projekt-Rollen

In Jira Cloud können Feldberechtigungen über **Issue Security Schemes** und **Field Configurations** granularer gesteuert werden:
- Bestimmte Felder nur für bestimmte Rollen editierbar machen
- Kombination mit statusbasierten Bildschirmmasken
- **Vorteil**: Granulare Kontrolle
- **Nachteil**: Sperrt Felder nicht dynamisch basierend auf dem Status, sondern statisch basierend auf der Rolle

## Empfehlung für VAMPIR

Basierend auf eurer bestehenden Infrastruktur und den bereits geplanten App-Lizenzen (ScriptRunner ist als Must-Have gelistet) empfehle ich eine **Kombination aus Ansatz 1 + 3**:

1. **Statusbasierte Bildschirmmasken** als erste Schutzschicht – Rechnungsfelder im Status „Rechnung gesendet" ausblenden/einschränken
2. **ScriptRunner-Validator** als zweite Schutzschicht – blockiert jede Transition oder Bearbeitung, die geschützte Felder im falschen Status verändert
3. **Automation Rule als Sicherheitsnetz** – falls trotzdem eine Änderung durchrutscht, wird sie erkannt und rückgängig gemacht

## Betroffene Workflow-Übergänge in VAMPIR

| DC-Übergang | DC-Property | Cloud-Ersatz |
|---|---|---|
| → Rechnung gesendet | `jira.issue.editable = false` | ScriptRunner Behaviour + Screen Scheme |
| → Stornotext editiert | `jira.issue.editable = true` (temporär) | ScriptRunner: nur Stornotext-Feld freischalten |
| → Storno-PDF gesendet | `jira.issue.editable = false` | Wieder komplett sperren |

Soll ich diese Analyse als Confluence-Seite erstellen, oder die konkreten ScriptRunner-Skript-Anforderungen für HiQ spezifizieren?

Quellen:

1. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807858%2FAtlassian_Cloud+Migration_Jira+Activities.docx

2. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804970&preview=%2F26804970%2F26807857%2FIssue-Liste+Cloud-Migration-Werkbank.csv

3. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807794%2FPr%C3%A4sentation.pptx

4. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804970/PG+-+Cloud-Migration+Atlassian+DataCenter

5. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804284/2026-02-26+Agenda+AK-IT

6. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804853/2026+012+Emergency+Change+Jira+Update+TIC-455

7. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804853/2026+012+Emergency+Change+Jira+Update+TIC-455

8. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804853/2026+012+Emergency+Change+Jira+Update+TIC-455

9. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804479/2025+02+Emergency+Change+TIC-426

10. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804296/2023+12+Emergency+Change+TIC-396

11. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804279/Jira+Finden+L+schen+von+nicht+versandten+externen+Nachrichten

12. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804635/2025+06+Emergency+Change+TIC-433

13. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804564/Jira+Debuggen+von+E-Mail-Sendeproblemen+SMTP

14. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804436/2025+04+Emergency+Change+TIC-429

15. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804816/2025+10+Emergency+Change+Jira+Update+TIC-449

16. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804816/2025+10+Emergency+Change+Jira+Update+TIC-449

17. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804816/2025+10+Emergency+Change+Jira+Update+TIC-449

18. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804627/2025+03+Emergency+Change+TIC-428

19. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804558/2024+08+Change+TIC-418+Capacity+Upscale+und+Package+update

20. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804511/2022+11+Upgrade+Plattform

21. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804651/2025+05+Emergency+Change+TIC-430

22. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804488/2024+03+Standard+Change+TIC-404+Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems

23. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804422/2025+01+Standard+Change+TIC-423Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems

24. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804862/2026+02+Standard+Change+Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems+ohne+L+schen+Server

25. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804975/Jira+Logfile+analysieren+Linux+Ebene

26. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804450/Jira+User+Zugang+reglementieren+im+Maintenance+Mode

27. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804468/07+Erzeugen+des+Testservers+als+Clone+des+aktuellen+Produktivsystems

28. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804386/Jira+VAMPIR+API+Token+generieren

29. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804341/Fehler+In+VAMPIR+erzeugtes+PDF+ist+leer

30. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804564/Jira+Debuggen+von+E-Mail-Sendeproblemen+SMTP

31. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804564/Jira+Debuggen+von+E-Mail-Sendeproblemen+SMTP

32. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804409/VAMPIR+CSV+Massendatenimport+Auftragsnummer+gezielt+setzen

33. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804409/VAMPIR+CSV+Massendatenimport+Auftragsnummer+gezielt+setzen

34. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804409/VAMPIR+CSV+Massendatenimport+Auftragsnummer+gezielt+setzen

35. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804245/VAMPIR+Ticket-Darstellung+Bearbeitung

36. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804245/VAMPIR+Ticket-Darstellung+Bearbeitung

37. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047686/Rechnungsversand+aus+VAMPIR+an+zwei+Mailadressen

38. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26805744/VAMPIR+Ticket-Zielobjekte+betroffene+Systemkomponenten

39. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804959/VAMPIR+Attachment+Upload+Limit+ndern

40. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804792/X-Rechnung

41. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804524/VAMPIR+Ticket-Typen+und+ihre+Benutzung

42. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804555/AG+Auswertungen

43. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150056&preview=%2F24150056%2F24150820%2FITW+Training+VAMPIR.pptx

44. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150056&preview=%2F24150056%2F24150820%2FITW+Training+VAMPIR.pptx

45. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150056&preview=%2F24150056%2F24150820%2FITW+Training+VAMPIR.pptx

46. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150056&preview=%2F24150056%2F24150362%2FITW+Training+VAMPIR.pdf

47. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=24150056&preview=%2F24150056%2F24150362%2FITW+Training+VAMPIR.pdf

48. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804792&preview=%2F26804792%2F26807480%2FDokumentation_2025-11-01.docx

49. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804792&preview=%2F26804792%2F26807414%2FDokumentation_2025-09-01.docx

50. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804508/VAMPIR+Ticketbearbeitung+Entscheidungsprozess+bei+Changes+Typ+Feedback

51. https://aktivsenioren.atlassian.net/wiki/spaces/BWAK/pages/19071006/VAMPIR+Auftragsbearbeitung+-+Akademie

52. https://aktivsenioren.atlassian.net/wiki/spaces/BWAK/pages/19071006/VAMPIR+Auftragsbearbeitung+-+Akademie

53. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804920/AG+Vampir

54. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804704/VAMPIR+Ticketbearbeitung+Prozess+Arbeitsmodell

55. https://aktivsenioren.atlassian.net/wiki/spaces/GES/pages/21954616/Leitfaden+EGR+Erstgespr+ch

56. https://aktivsenioren.atlassian.net/wiki/spaces/DOK/pages/32047126/Einf+hrung+neuer+Regionalleiter+RL

57. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804474/AG+Systemplattform

58. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804725/2025+02+Emergency+Change+TIC-424

59. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804499/Jira+Suchen+von+abgeschlossenen+Tickets+nach+Stichworten+Inhalten

60. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804499/Jira+Suchen+von+abgeschlossenen+Tickets+nach+Stichworten+Inhalten

61. https://aktivsenioren.atlassian.net/wiki/spaces/ADB/pages/24150035/VVT+Verein+Vampir+Tutorials

62. https://aktivsenioren.atlassian.net/wiki/spaces/GES/pages/21954591/Leitfaden+EGR+Auftragsabschluss

63. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804863&preview=%2F26804863%2F26807794%2FPr%C3%A4sentation.pptx

64. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804432&preview=%2F26804432%2F26805255%2FJira-Configuration.xlsx

65. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=26804970&preview=%2F26804970%2F26807857%2FIssue-Liste+Cloud-Migration-Werkbank.csv

66. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804467/2024+10+Change+TIC-420

67. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804416/2025+01+Standard+Change+TIC-423+Cloning+rebuild

68. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804309/Jira+Logfiles+analysieren+Jira+Admin+Ebene

69. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804940/Jira+Crowd+user+Synchronisation

70. https://aktivsenioren.atlassian.net/wiki/pages/viewpageattachments.action?pageId=32047206&preview=%2F32047206%2F32047591%2FRechte+Jira_Projekte.pdf

71. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804519/2024+05+Emergency+Change+TIC-413

72. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26804519/2024+05+Emergency+Change+TIC-413

73. https://aktivsenioren.atlassian.net/wiki/spaces/AR/pages/26805434/Jira+User+Online+Status+ermitteln


