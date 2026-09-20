# LFD.NR Rechnung

1. Zu $scripting.execute("articles-tool.groovy") (Einfluss auf das Issue)
Ja, dieses Groovy-Skript hat im Server/DC-System einen massiven Einfluss gehabt:

Unter Jira Server/Data Center (DC):

Midori Better PDF Exporter konnte Server-seitigen Groovy-Code ausführen.

Das Skript articles-tool.groovy hat im Hintergrund nach den verknüpften Rechnungen/Teilrechnungen (Subtasks oder Linked Issues) gesucht, die laufende Nummer (00, 01, 02...) ermittelt und das Rechnungs-Issue geladen.

Dadurch war $issues.get(0).summary im Template nicht mehr Auftrag-26500, sondern die ermittelte Rechnungsnummer 26500-00.

Unter Jira Cloud:

Aus Sicherheits- und Architekturgründen gibt es in Jira Cloud keine Groovy-Skriptausführung ($scripting.execute existiert in Cloud nicht und wird wirkungslos ignoriert).

2. Wie die Nummerierung (00 bis 10) in Jira Cloud abgebildet wird
Da das Groovy-Skript in der Cloud fehlt, übernimmt jetzt Jira Automation diese Logik:

Woher die 26500-00 kommt, hängt davon ab, wie die Rechnungen in Ihrem Jira-Projekt gepflegt werden:

Fall A: Die Rechnungen sind Unteraufgaben (Sub-tasks) oder verknüpfte Vorgänge
Wenn für jede Rechnung ein Untervorgang existiert (dessen Summary 26500-00, 26500-01 etc. lautet):

Fügen Sie in der Automation vor dem Web-Request eine Verzweigung (Branch rule / Related issues) ein:

Für Unteraufgaben (Sub-tasks) oder Verknüpfte Vorgänge (Linked issues).

Innerhalb des Branchs senden Sie den Web-Request mit:




filename: "{{issue.summary}}_Rechnung.pdf"
(Innerhalb der Verzweigung ist {{issue.summary}} dann automatisch 26500-00.)

Fall B: Die Rechnungsnummer bzw. der Index steht in einem benutzerdefinierten Feld
Falls beim Auftrag ein Feld existiert (z. B. Rechnungsindex 00 oder customfield_10xxx):




filename: "{{issue.summary.replace("Auftrag-", "")}}-{{issue.customfield_10xxx}}_Rechnung.pdf"
Fall C: Fester Standardwert -00 für Einzelrechnungen
Falls die meisten Aufträge nur eine Rechnung haben und diese vorerst standardmäßig mit -00 benannt werden soll:




filename: "{{issue.summary.replace("Auftrag-", "")}}-00_Rechnung.pdf"


