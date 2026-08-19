Eine native "In Verwendung"-Anzeige (wie man sie z. B. von Confluence-Makros oder Datenbanktabellen kennt) bietet der Better PDF Exporter laut Dokumentation nicht. Es gibt aber mehrere praktische Wege, das trotzdem zuverlässig zu prüfen:

## 1. Manueller Abgleich über die PDF-Views-Liste (wichtigster Weg)
Jede PDF View hat ein Dropdown-Feld "PDF template", das genau ein Template referenziert. PDF views generate PDF exports using different templates and different Groovy scripts, wobei jede View genau ein Template referenziert. Gehe also die Liste aller Views durch (egal ob enabled oder disabled) und notiere dir, welches Template dort jeweils ausgewählt ist. Templates, die in **keiner** View auftauchen, sind zumindest über die manuellen Export-Wege nicht mehr referenziert.

## 2. Textsuche in den Template-Dateien selbst (wichtig für "common-elements")
Templates können sich gegenseitig einbinden, z. B. über `#parse("auftrag-common-elements-fo.vm")` (Velocity-Include-Direktive). Das erklärt auch, warum du z. B. `V2-4_auftrag-common-elements-fo.vm` mit der Beschreibung "Here we define the common elements... to just include this file in each template" siehst. Um das zu prüfen:
- Öffne jedes `.vm`-Template im Editor (PDF Templates-Seite) und suche nach `#parse` oder dem Dateinamen anderer Templates als Text.
- Alternativ: Falls du per REST-API oder Datenbankzugriff rankommst, könntest du alle Template-Inhalte exportieren und mit einem einfachen `grep` nach Dateinamen suchen (das wäre schneller als 81 Dateien einzeln durchzuklicken).

## 3. Automatisierungsregeln separat prüfen
Automatisierte Exporte rendern die PDF-Templates direkt, nicht über PDF-Views – das heißt, ein Template kann *ausschließlich* über eine Automatisierungsregel verwendet werden, ohne in irgendeiner View aufzutauchen. Prüfe also zusätzlich alle bestehenden Automation-Regeln (meist unter einem eigenen Menüpunkt "PDF Automation" in den App-Einstellungen) auf das dort hinterlegte Template.

## 4. Groovy-Skripte cross-referenzieren
Ähnlich wie bei den Templates: Skripte wie `charts.groovy` werden von mehreren `.vm`-Dateien genutzt (siehe deine CSV, z. B. bei `sales-report-fo.vm` oder `x-hello-charts-fo.vm`). Auch hier hilft nur eine Textsuche nach dem Skriptnamen in den Templates.

---

**Praktischer Vorschlag:** Da du bereits die CSV mit allen 81 Templates hast, könnte ich dir ein kleines Script bauen, das (falls du die Template-Inhalte z. B. als Textdateien exportierst) automatisch prüft, welche Dateinamen in welchen anderen Templates referenziert werden – das würde dir Punkt 2 automatisieren. Hast du Zugriff auf die Rohinhalte der Templates (z. B. als Exportdatei oder über die REST-API), oder liegen die nur in der Jira-Oberfläche?