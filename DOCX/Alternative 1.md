Das ist ein wichtiger Architekturpunkt – denn wenn ihr auf `com.lowagie.text.*` (OpenPDF) umsteigt, ändert sich nicht nur die Library, sondern auch **wo** der Code überhaupt laufen kann. Das ist der eigentliche Knackpunkt für eure Migration.

## Die zentrale Einschränkung

OpenPDF ist eine **reine Java-Bibliothek**. Sie braucht eine JVM. In Jira Cloud gibt es aber keinen Ort mehr, an dem ihr direkten Java-Code mit Zugriff auf beliebige `.jar`-Bibliotheken ausführen könnt:

- **Forge** (Atlassians offizielles Cloud-App-Framework) läuft auf **Node.js**, nicht auf der JVM – `com.lowagie.text.Document` lässt sich dort nicht instanziieren.
- **Automation for Jira** (No-Code-Regeln) kann zwar Webhooks feuern und REST-Calls machen, aber keinen beliebigen Java-Code ausführen.
- **ScriptRunner Cloud** arbeitet fundamental anders als ScriptRunner DC (kein direkter Groovy-Klassenzugriff mehr auf Third-Party-Jars).

Das heißt: Der PDF-Generierungscode muss **außerhalb** von Jira Cloud laufen – als eigener, extern gehosteter Dienst, den ihr über HTTP anspielt.## Die Komponenten im Detail

**1. Trigger (Jira Cloud)**
Ein Workflow-Übergang löst über eine **Automation-Regel** eine Webhook-Aktion aus. Die Regel sammelt die relevanten Issue-Daten (Custom Fields, Beschreibung, verlinkte Issues) und schickt sie als JSON-Payload.

**2. PDF-Service (extern gehostet)**
Ein schlanker Java-Dienst – z. B. Spring Boot auf einer eigenen VM, oder eine AWS-Lambda-Funktion mit Java-Runtime – nimmt die JSON-Daten entgegen und baut mit `com.lowagie.text.Document`, `Paragraph`, `PdfPTable` etc. das PDF zusammen. Pro Template-Typ (Rechnung, Zertifikat, Report) bietet sich eine eigene Builder-Klasse an, damit ihr die Velocity-Logik von früher strukturell nachbildet, nur eben in Java statt in Velocity-Templates.

**3. Rückweg**
Der Service liefert die PDF-Bytes (base64 oder binär) zurück. Die Automation-Regel – oder ein zweiter Webhook-Call vom Service aus – hängt das PDF per **Jira REST API** (`POST /rest/api/3/issue/{key}/attachments`) am Issue an.

**4. Absicherung**
Da der Service öffentlich erreichbar sein muss, braucht er eine Authentifizierung Richtung Jira (API-Token) und umgekehrt eine Prüfung, dass die eingehenden Webhook-Calls tatsächlich von eurer Jira-Instanz kommen (signiertes Secret im Header).

## Praktische Konsequenz für eure Templates

Eure Velocity/XSL-FO-Logik lässt sich nicht 1:1 "reinkopieren" – ihr baut sie als typisierte Java-Klassen nach, die Feldwerte aus dem JSON lesen und OpenPDF-Elemente erzeugen. Das ist mehr Aufwand als reines Template-Mapping, gibt euch aber volle Kontrolle über Layout, Tabellen und bedingte Logik, ohne von Better PDF Cloud abhängig zu sein.

Soll ich dir für einen konkreten Template-Typ (z. B. die Rechnung) skizzieren, wie so eine Java-Builder-Klasse mit OpenPDF aussehen könnte?