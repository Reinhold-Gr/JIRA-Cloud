Die Erstellung und das Anhängen einer PDF-Datei über Better PDF Exporter for Jira (via Jira Automation / Midori Cloud Service) dauert in der Regel ca. 5 bis 15 Sekunden. Bei sehr komplexen Vorlagen oder temporärer Systemauslastung kann der Vorgang vereinzelnd bis zu 20–30 Sekunden in Anspruch nehmen.
Aufgrund der kleinen PDFs liegen wir im Bereich < 5 sec. (Bisher beobachtet!)

---

# Ablauf des automatisierten Prozesses
## Triggering (Jira Automation):
Die Automatisierungsregel wird ausgelöst und sendet einen asynchronen Webhook/HTTP-POST-Request an den Midori-Service (https://jpdfc.midori.systems/rest/v1/automation).
## Datenabruf & Rendering (Midori Cloud):
Der externe Dienst ruft die benötigten Vorgangsdaten ab, rendert das entsprechende PDF-Template (z. B. Auftragsbestätigung oder Rechnung) und erzeugt das Dokument.
## Anhängen der Datei (Jira REST API):
Das fertig erstellte PDF wird über die Jira Attachment REST API direkt an den jeweiligen Vorgang zurückübertragen und dort angehängt.
## Callback / Rückmeldung:
Falls in der Automatisierungsregel ein Callback definiert ist, meldet Midori den Abschluss der Aktion zurück.

# Wesentliche Einflussfaktoren auf die Ausführungszeit
## Komplexität des PDF-Templates:
Umfangreiche Layouts, viele Felddaten oder eingebettete Bilder erhöhen die Rendering-Zeit.
## Warteschlange der Jira Automation: 
Asynchrone Regeln in Jira Cloud werden in einer Ausführungswarteschlange verarbeitet; je nach Last kann es hier zu leichten Verzögerungen kommen.
## Netzwerk- und API-Latenz: 
Die Kommunikation zwischen den Atlassian Cloud-Servern und dem Midori-Dienst.