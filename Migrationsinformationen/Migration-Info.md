# Inhalt

[Üblicherweise erwarteter Aufwand 2](#üblicherweise-erwarteter-aufwand)

[ScriptRunner (Groovy, Jira Server/DC)
3](#scriptrunner-groovy-jira-serverdc)

[Nachher: Jira Cloud Automation Rule
4](#nachher-jira-cloud-automation-rule)

[konfiguriere ich Bedingungen in Jira Cloud Automation Rules
5](#konfiguriere-ich-bedingungen-in-jira-cloud-automation-rules)

[Architektur zuerst, dann der Code 6](#architektur-zuerst-dann-der-code)

[1. Jira Cloud Automation Rule — Webhook-Aktion
7](#jira-cloud-automation-rule-webhook-aktion)

[2. Forge Function (TypeScript) — Ersatz für den Groovy-Code
7](#forge-function-typescript-ersatz-für-den-groovy-code)

[Wann lohnt sich Forge vs. reine Cloud-Automation?
9](#wann-lohnt-sich-forge-vs.-reine-cloud-automation)

[Better PDF (Exporter for Jira) 10](#better-pdf-exporter-for-jira)

[Reine Velocity-Syntax läuft in der Cloud-Version identisch:
10](#reine-velocity-syntax-läuft-in-der-cloud-version-identisch)

[kritischer Teil 10](#kritischer-teil)

[Direkte Java-Klassen — nicht verfügbar:
10](#direkte-java-klassen-nicht-verfügbar)

[Konkrete Fallstricke 12](#konkrete-fallstricke)

[**Empfohlenes Vorgehen** 13](#empfohlenes-vorgehen)

[Überblick Cloud-kompatibel 13](#überblick-cloud-kompatibel)

# Üblicherweise erwarteter Aufwand

<img src="media/image1.png" style="width:6.3in;height:8.04653in" />

Die DC-spezifischen Aufgaben sind jeweils mit einem farbigen Balken
links markiert. Die wichtigsten Ergänzungen im Überblick:

**Phase 2** wurde um eine Woche verlängert, weil die LDAP/AD-Anbindung
und die Atlassian Access-Konfiguration eigene Testzyklen braucht — vor
allem wenn Crowd im Spiel ist.

**Phase 4 (Cut-over)** ist die kritischste Änderung: der koordinierte
Cluster-Shutdown und das Einfrieren des Shared Home auf NFS müssen in
der richtigen Reihenfolge passieren, sonst riskiert man inkonsistente
Daten beim Import.

## ScriptRunner (Groovy, Jira Server/DC)

import com.atlassian.jira.component.ComponentAccessor

import com.atlassian.mail.Email

import com.atlassian.mail.MailFactory

def issue = event.issue

def reporter = issue.reporter

def mailServer = MailFactory.serverManager.defaultSMTPMailServer

if (mailServer) {

def email = new Email(reporter.emailAddress)

email.setSubject("Issue \${issue.key} ist jetzt In Review")

email.setBody("""

Hallo \${reporter.displayName},

das Issue \${issue.key} – \${issue.summary}

wurde in den Status 'In Review' verschoben.

Link:
\${ComponentAccessor.applicationProperties.getString('jira.baseurl')}/browse/\${issue.key}

""")

mailServer.send(email)

}

## Nachher: Jira Cloud Automation Rule

<img src="media/image2.png" style="width:6.3in;height:4.80208in" />

Die wichtigsten Unterschiede zwischen Groovy und Cloud Automation im
Überblick:

**Groovy issue.reporter.emailAddress** → Cloud Smart Value
{{issue.reporter.emailAddress}} — die Syntax ist ähnlich, aber mit
doppelten geschweiften Klammern.

**ComponentAccessor.applicationProperties.getString('jira.baseurl') +
'/browse/' + issue.key** entfällt komplett — in Cloud gibt es einfach
{{issue.url}}.

**Feste Empfängeradressen** (z. B. Teamleiter) werden direkt im
"An"-Feld eingetragen, kein Groovy-Lookup mehr nötig. Wenn der
Teamleiter dynamisch aus einem Custom Field kommen soll, geht das mit
{{issue.customfield_XXXXX.emailAddress}}.

**Einschränkung:** Komplexe Logik wie Schleifen, HTTP-Calls oder
bedingte Empfängerlisten aus einer Datenbank sind in Cloud Automation
nicht möglich — dafür braucht man dann einen Webhook auf einen externen
Dienst (z. B. eine Forge-Function oder ein
Automation-for-Jira-Premium-Feature).

## konfiguriere ich Bedingungen in Jira Cloud Automation Rules

Im Rule Builder klickst du nach dem Trigger auf **"+ Neue Komponente
hinzufügen"** → **"Bedingung"**. Bedingungen können auch zwischen zwei
Aktionen gesetzt werden, um den weiteren Ablauf zu steuern.

------------------------------------------------------------------------

**Die wichtigsten Bedingungstypen**

**Issue-Felder-Bedingung** — der häufigste Typ. Du wählst ein Feld und
prüfst seinen Wert:

- Feld: Priorität, Operator: ist gleich, Wert: Hoch

- Feld: Assignee, Operator: ist nicht leer

- Feld: Custom Field XY, Operator: enthält, Wert: Review

Funktioniert mit Standard- und Custom Fields.

**Erweiterte Bedingung (Smart Values / JQL-ähnlich)** — für komplexere
Logik:

{{issue.priority.name == "High"}}

{{issue.assignee != null}}

{{issue.customfield_10050.value == "Genehmigt"}}

Diese Option heißt im UI "Erweiterte Vergleichsbedingung" und ist der
mächtigste Typ.

**Benutzer-Bedingung** — prüft, wer die Aktion ausgelöst hat:

- "Auslöser ist Mitglied der Gruppe: jira-software-users"

- "Auslöser ist nicht der Assignee" (verhindert
  Selbst-Benachrichtigungen)

**Unteraufgaben-/Verknüpfungs-Bedingung** — prüft den Zustand
verknüpfter Issues:

- "Alle Unteraufgaben haben Status: Done"

- Typisch für: übergeordnetes Issue automatisch schließen

**Schichtbedingung (Branch-Condition)** — wenn du mit "Für zugehörige
Issues" arbeitest, kannst du innerhalb des Branches filtern.

------------------------------------------------------------------------

**Mehrere Bedingungen kombinieren**

Du kannst Bedingungen mit **UND / ODER** verknüpfen:

- Standardmäßig gilt UND (alle müssen zutreffen)

- Mit dem Block **"Bedingungsgruppe"** kannst du ODER-Logik bauen:

Gruppe A (ODER):

\- Priorität = Hoch

\- Priorität = Kritisch

UND

\- Assignee ist nicht leer

------------------------------------------------------------------------

**Häufige Fallstricke**

Wenn keine Bedingung zutrifft, wird die Rule nicht abgebrochen — sie
läuft einfach weiter zur nächsten Komponente. Willst du bei
Nicht-Erfüllung stoppen, brauchst du explizit die Bedingung als
"Abbruchbedingung" — das ist in Cloud Automation der Standard, sobald
eine Bedingung **nicht** erfüllt ist, werden alle nachfolgenden Aktionen
übersprungen.

Außerdem: Custom Fields werden im UI mit ihrem Namen angezeigt, intern
aber als customfield_XXXXX referenziert — relevant wenn du Smart Values
in der erweiterten Bedingung nutzt. Die ID findest du über
**Jira-Einstellungen → Felder → Custom Fields → Feld anklicken**, die ID
steht dann in der URL.

## Architektur zuerst, dann der Code

<img src="media/image3.png" style="width:6.3in;height:3.59514in" />

Und hier der konkrete Code, der die Groovy-Logik ersetzt:

### 1. Jira Cloud Automation Rule — Webhook-Aktion

Im Rule Builder nach dem Trigger einfach:

Aktion → **Webhook senden** → URL:
https://your-app.atlassian.net/x/invoke/... (Forge gibt dir die URL nach
dem Deploy)

Payload (JSON):

{

"issueKey": "{{issue.key}}",

"summary": "{{issue.summary}}",

"reporterEmail": "{{issue.reporter.emailAddress}}",

"reporterName": "{{issue.reporter.displayName}}",

"projectKey": "{{issue.project.key}}"

}

### 2. Forge Function (TypeScript) — Ersatz für den Groovy-Code

import Resolver from '@forge/resolver';

import api, { route } from '@forge/api';

const resolver = new Resolver();

resolver.define('handleStatusChange', async ({ payload }) =\> {

const { issueKey, reporterEmail, reporterName, projectKey } = payload;

// Jira REST API: Custom Field "Teamleiter" nachladen

const response = await api.asApp().requestJira(

route\`/rest/api/3/issue/\${issueKey}?fields=customfield_10050,summary\`

);

const issueData = await response.json();

const teamleiterEmail =
issueData.fields.customfield_10050?.emailAddress;

// Empfänger dynamisch zusammenstellen

const recipients = \[reporterEmail\];

if (teamleiterEmail) recipients.push(teamleiterEmail);

// Bedingte Logik: Priorität prüfen

const priority = issueData.fields.priority?.name;

if (priority === 'Kritisch') {

recipients.push('notfall-team@firma.de');

}

// E-Mail via SendGrid senden (fetch ist in Forge erlaubt)

await fetch('https://api.sendgrid.com/v3/mail/send', {

method: 'POST',

headers: {

'Authorization': \`Bearer \${process.env.SENDGRID_API_KEY}\`,

'Content-Type': 'application/json'

},

body: JSON.stringify({

personalizations: \[{ to: recipients.map(e =\> ({ email: e })) }\],

from: { email: 'jira-noreply@firma.de' },

subject: \`\${issueKey} ist jetzt In Review\`,

content: \[{

type: 'text/html',

value: \`\<p\>Hallo \${reporterName},\</p\>

\<p\>das Issue \<strong\>\${issueKey}\</strong\> wurde in \<em\>In
Review\</em\> verschoben.\</p\>

\<p\>\<a href="https://firma.atlassian.net/browse/\${issueKey}"\>Issue
öffnen\</a\>\</p\>\`

}\]

})

});

});

export const handler = resolver.getDefinitions();

## Wann lohnt sich Forge vs. reine Cloud-Automation?

| **Situation**                        | **Lösung**                    |
|--------------------------------------|-------------------------------|
| Feste oder Smart-Value-Empfänger     | Cloud Automation (kein Code)  |
| Empfänger aus Custom Field           | Cloud Automation reicht meist |
| Empfänger aus Gruppe / LDAP-Lookup   | Forge Function nötig          |
| HTML-Template mit Tabellen / Logos   | Forge + SendGrid              |
| Bedingte Logik (if/else, Schleifen)  | Forge Function                |
| Externe Systeme ansprechen (CRM, HR) | Forge Function + fetch        |

## Better PDF (Exporter for Jira)

Das ist eine Marketplace-App von Marten Gajda. Die Templates findest du
nach Installation unter:

**Jira-Einstellungen** (Zahnrad oben rechts) → **Apps** → **Better PDF
Exporter** → **Templates**

Dort kannst du Velocity-basierte Templates verwalten, neue hochladen
oder bestehende bearbeiten. Auf Projektebene gibt es keinen eigenen
Einstiegspunkt — alles läuft über die globale App-Verwaltung.

## Reine Velocity-Syntax läuft in der Cloud-Version identisch:

\#foreach(\$item in \$issues)

\$item.summary

\$item.key

\#end

\#if(\$issue.priority.name == "High")

Dringend!

\#end

Auch Standard-Velocity-Direktiven wie \#set, \#macro, \#include, \#parse
werden unterstützt.

## kritischer Teil

In der Server/DC-Version hattest du direkten Zugriff auf Jira-interne
Java-Objekte. Die sind in Cloud **komplett weg**.

*ComponentAccessor — nicht verfügbar*:

\## Server/DC — funktioniert NICHT in Cloud

\#set(\$userManager = \$componentAccessor.getUserManager())

\#set(\$user = \$userManager.getUserByName("admin"))

### Direkte Java-Klassen — nicht verfügbar:

\## Server/DC — funktioniert NICHT in Cloud

\#set(\$dateFormat = \$dateTool.format("dd.MM.yyyy", \$date))

**<u>Existieren nichtmehr:</u>**

**\$workflowManager,**

**\$permissionManager, \$**

**projectManager**

**<u>Stattdessen verfügbar</u>**

Better PDF Cloud stellt eigene, vereinfachte Kontextvariablen bereit:

**\## Funktioniert in Cloud**

\$issue.key

\$issue.summary

\$issue.status.name

\$issue.priority.name

\$issue.assignee.displayName

\$issue.reporter.emailAddress

\$issue.created \## als String, nicht Java-Date-Objekt

\$issue.customFields.get("Mein Feld")

**\## Für Datum-Formatierung: eigene Helper**

\$dateFormatter.format(\$issue.created, "dd.MM.yyyy")

### \
Konkrete Fallstricke

**Datum-Objekte** sind in Cloud Strings, keine Java-Date-Objekte mehr:

velocity

\## Server: \$date.format("dd.MM.yyyy", \$issue.created) ← Java-Date

\## Cloud: \$dateFormatter.format(\$issue.created, "dd.MM.yyyy") ←
Cloud-Helper

**Custom Fields** werden anders angesprochen:

velocity

\## Server/DC

\$issue.getCustomFieldValue(\$customFieldManager.getCustomFieldObject("customfield_10050"))

\## Cloud

\$issue.customFields.get("Name des Feldes")

\## oder

\$issue.fields.customfield_10050

**Sub-Tasks / verknüpfte Issues:**

velocity

\## Cloud — vereinfacht

\#foreach(\$subtask in \$issue.subtasks)

\$subtask.key — \$subtask.summary

\#end

### **Empfohlenes Vorgehen**

1.  Templates in einer **Cloud-Sandbox** testen, bevor ihr auf
    Produktion geht.

2.  Alle Stellen mit \$componentAccessor, \$customFieldManager,
    \$workflowManager suchen — das sind die Kandidaten zum Umschreiben.

3.  Die offizielle Variablenliste von Better PDF Cloud als Referenz
    nutzen: https://marketplace.atlassian.com/apps/5167 → Dokumentation
    →\
    "Template variables".

### Überblick Cloud-kompatibel

|                                         | **Cloud-kompatibel** |
|-----------------------------------------|----------------------|
| Velocity-Syntax (#foreach, \#if, \#set) | ✓ ja                 |
| Standard-Felder (\$issue.summary etc.)  | ✓ ja                 |
| Custom Fields (neue Syntax)             | ✓ mit Anpassung      |
| \$componentAccessor / Java-Klassen      | ✗ nein               |
| Datum-Formatierung                      | ✓ mit neuem Helper   |
| Direkte Java-API-Aufrufe                | ✗ nein               |

**\**

### Zusammenfassung der Umstellungsaufwände

#### Common-Teil aller PDF Templates\

| **Stelle**                           | **Aufwand**   | **Risiko** |
|--------------------------------------|---------------|------------|
| \$ComponentAccessor entfernen        | gering        | niedrig    |
| \$baseUrl-Erkennung reaktivieren     | minimal       | niedrig    |
| customfield_12602 Syntax             | gering        | niedrig    |
| Issue-Link-Traversierung umschreiben | **mittel**    | mittel     |
| \$pdfContent.linkCollectionByIssue() | **unbekannt** | **hoch**   |
| \$remoteIssueLinkManager             | **hoch**      | hoch       |

**Empfehlung:**\
Zuerst klären, ob \$pdfContent.linkCollectionByIssue() in Better PDF
Cloud existiert — das ist der Dreh- und Angelpunkt des Templates.

Falls nicht, muss die gesamte Link-Logik neu aufgebaut werden. Am
schnellsten geht das mit einer Testinstanz und einem Issue das alle
Verlinkungstypen (Klienten, Region, Abschlussbericht, Rechnungen)
enthält.

#### Beispiel initial-confirmation-fo.vm

Das ist ein sehr umfangreiches Template. Ich analysiere es systematisch
nach Problemkategorien.

------------------------------------------------------------------------

**Kritische Stellen (❌ nicht Cloud-kompatibel)**

**1. \$scripting.execute() — nicht verfügbar**

\## ❌ Server/DC

\$scripting.execute("articles-tool.groovy")

Das ist ein ScriptRunner-Aufruf aus dem Template heraus. In Cloud gibt
es \$scripting nicht. Was macht articles-tool.groovy? Das ist
entscheidend — wenn es Custom-Field-Werte vorberechnet oder Variablen
setzt, muss das in Cloud anders gelöst werden (Forge Function oder
direkt im Template).

------------------------------------------------------------------------

**2. \$customFieldManager.getCustomFieldObject() — durchgängig verwendet
(❌)**

Das ist der größte Aufwand. Das Muster zieht sich durch das gesamte
Template:

\## ❌ überall — ca. 30+ Stellen

\$klient.getCustomFieldValue(\$customFieldManager.getCustomFieldObject("customfield_10207"))

\$region.getCustomFieldValue(\$customFieldManager.getCustomFieldObject("customfield_10303"))

**Cloud-Ersatz:**

\## ✓ Cloud

\$klient.fields.customfield_10207

\$region.fields.customfield_10303

Aber: \$klient und \$region kommen aus der Link-Traversierung im
Common-Elements-File — das muss wie besprochen zuerst repariert sein.

------------------------------------------------------------------------

**3. \$issues.get(0).getCustomFieldValue("customfield_XXXXX") — alle
Stellen**

\## ❌ mehrfach im Template

\$issues.get(0).getCustomFieldValue("customfield_10230") \## Auftrags-Nr

\$issues.get(0).getCustomFieldValue("customfield_11301") \##
Beratungsfall (Multi-Select)

\$issues.get(0).getCustomFieldValue("customfield_10605") \## Fachthemen

\$issues.get(0).getCustomFieldValue("customfield_10604") \## Art des
Unternehmens

\$issues.get(0).getCustomFieldValue("customfield_10606") \## Erstkontakt

\$issues.get(0).getCustomFieldValue("customfield_10602") \## Details
Branche

\$issues.get(0).getCustomFieldValue("customfield_10603") \## Wünsche

\$issues.get(0).getCustomFieldValue("customfield_11456") \##
Rahmenauftrag

**Cloud-Ersatz:**

\## ✓ Textfelder

\$issues.get(0).fields.customfield_10230

\$issues.get(0).fields.customfield_10602

\## ✓ Select-Feld (mit .value)

\$issues.get(0).fields.customfield_11456.value

\## ✓ Multi-Select (Iteration bleibt gleich, aber Zugriff anders)

\#foreach(\$v in \$issues.get(0).fields.customfield_11301)

\$v.value

\#end

------------------------------------------------------------------------

**4. \$cf12602.value — Select-Feld-Zugriff**

\## ❌

\#set(\$cf12602 =
\$issues.get(0).getCustomFieldValue("customfield_12602"))

\#set(\$info = \$cf12602.value)

**Cloud-Ersatz:**

\## ✓

\#set(\$cf12602 = \$issues.get(0).fields.customfield_12602)

\#set(\$info = \$cf12602.value) \## .value bleibt gleich bei
Select-Feldern

**5. \$userDateTimeFormatter / \$dateTimeStyle — prüfen**

\## ⚠️ Prüfen ob in Cloud verfügbar

\$userDateTimeFormatter.withStyle(\$dateTimeStyle.DATE).format(\$currentDate)

Diese Objekte kommen von Better PDF selbst. In der Cloud-Version sind
sie möglicherweise verfügbar, aber die API könnte sich geändert haben.
Ersatz-Option:

\## Alternative falls nicht verfügbar

\$dateFormatter.format(\$currentDate, "dd.MM.yyyy")

------------------------------------------------------------------------

**6. \$filename Setzen**

\## ❌

\#set(\$filename =
"\$issues.get(0).getCustomFieldValue('customfield_10230')\_Auftrag.pdf")

**Cloud-Ersatz:**

\## ✓

\#set(\$auftragsnr = \$issues.get(0).fields.customfield_10230)

\#set(\$filename = "\${auftragsnr}\_Auftrag.pdf")

------------------------------------------------------------------------

**Was problemlos funktioniert ✓**

- Gesamte FO-Struktur (fo:root, fo:table, fo:block etc.) — vollständig
  kompatibel

- \$xmlutils.escape() — verfügbar in Cloud

- \#foreach, \#if, \#set, \#end — Velocity-Syntax unverändert

- \$include.parse() — verfügbar, aber das eingebundene
  Common-Elements-File muss separat angepasst werden

- \$baseUrl — verfügbar

- Logo-URLs (externe PNGs) — funktionieren

- Debug-Blöcke — Velocity-Syntax bleibt gleich

- Der \$col-Zähler für zweispaltige Checkbox-Listen — funktioniert
  identisch

- \#if(\$klient.fields.customfield_11305.value != "Deutschland") —
  funktioniert mit neuer Syntax

**Zusammenfassung Umstellungsaufwand**

| **Problem** | **Anzahl Stellen** | **Aufwand** |
|----|----|----|
| \$customFieldManager.getCustomFieldObject() entfernen | ~30 | mittel — Suchen & Ersetzen |
| .getCustomFieldValue() → .fields. | ~20 | mittel — Suchen & Ersetzen |
| \$scripting.execute() klären | 1 | **hoch — abhängig vom Groovy-Skript** |
| \$userDateTimeFormatter testen | 2 | gering |
| \$filename Syntax | 1 | gering |
| Common-Elements-File (Link-Traversierung) | zentral | **hoch — Voraussetzung für alles** |

------------------------------------------------------------------------

**Empfohlene Vorgehensweise**

1.  Zuerst articles-tool.groovy analysieren — was macht es, wird es
    überhaupt noch gebraucht?

2.  Common-Elements-File reparieren (Link-Traversierung für \$klient und
    \$region)

3.  Dann dieses Template: alle getCustomFieldValue /
    getCustomFieldObject per Suchen & Ersetzen umschreiben

4.  In Sandbox mit einem echten Auftrag testen, Debug-Mode einschalten

Soll ich eine vollständig umgeschriebene Cloud-Version dieses Templates
erstellen?

#### Erste Aufwandsschätzung\

**Voraussetzungen die den Aufwand stark beeinflussen, sind noch zu
klären:**

Es gibt 8 Templates[^1].

 **articles-tool.groovy genau?** [^2]

 **Ist Better PDF Cloud bereits lizenziert und in Sandbox vorhanden?**

**Grundannahmen**

- **2 Templates bereits analysiert (Common-Elements +
  Empfangsbestätigung)**

- **6 weitere Templates, vermutlich ähnlich aufgebaut (FO-Struktur,
  \$customFieldManager, verlinkte Issues)**

- **articles-tool.groovy noch unbekannt**

- **\$pdfContent.linkCollectionByIssue() noch ungeklärt**

**Aufwand nach Phase**

| **Phase** | **Inhalt** | **Aufwand** |
|----|----|----|
| **Analyse** | **Alle 8 Templates + Groovy-Skript sichten, Abhängigkeiten kartieren, Better PDF Cloud Doku prüfen** | **3–4 Tage** |
| **Common-Elements-File** | **Zentrale Basis für alle Templates — Link-Traversierung, \$klient, \$region reparieren** | **2–4 Tage** |
| **Template 1–2** | **Bereits analysiert, Umsetzung** | **2–3 Tage** |
| **Template 3–8** | **6 weitere Templates umschreiben (Lernkurve hilft ab Template 3)** | **6–10 Tage** |
| **articles-tool.groovy** | **Analysieren + ersetzen (Schätzung ohne Kenntnis des Inhalts)** | **1–3 Tage** |
| **Testen** | **Sandbox, je Template mind. 1 echter Testlauf mit allen Verlinkungstypen** | **4–6 Tage** |
| **Fehlerbehebung & Layout** | **FO-Rendering-Abweichungen, Feldwerte die anders kommen als erwartet** | **3–5 Tage** |
| **Abnahme & Dokumentation** | **Fachliche Prüfung durch euch, Änderungen einarbeiten** | **2–3 Tage** |
| **Gesamt** |  | **23–38 Tage** |

**Verdichtet auf drei Szenarien**

| **Szenario** | **Tage** | **Voraussetzung** |
|----|----|----|
| **Best Case** | **~20 Tage** | **\$pdfContent funktioniert in Cloud, Groovy-Skript trivial, keine Layout-Überraschungen** |
| **Wahrscheinlich** | **~28 Tage** | **Normaler Verlauf mit 2–3 Iterationen pro Template** |
| **Worst Case** | **~40 Tage** | **\$pdfContent nicht verfügbar → komplette Link-Logik neu, komplexes Groovy-Skript** |

**Wichtigste Hebel zur Aufwandsreduktion**

**\$pdfContent.linkCollectionByIssue() zuerst klären — das ist die
einzige Frage die den Aufwand um 30–40% verschieben kann. Ein einzelner
Test in der Sandbox mit einem verlinkten Issue beantwortet das in einer
Stunde.**

**Common-Elements-File hat höchste Priorität — es ist die Basis für alle
8 Templates. Wenn es läuft, fallen die Einzeltemplates deutlich
leichter.**

**Parallelisierung — Template-Umschreibung und Sandbox-Setup können
parallel laufen wenn zwei Personen verfügbar sind.**

**Empfehlung für die Projektplanung**

**30 Tage als Planungsgröße — mit einem Puffer von 10 Tagen für den
Worst Case. Entspricht ca. 6–8 Wochen bei 50% Auslastung neben dem
Tagesgeschäft**

**\**

#### Was das Skript articles-tool.groovy macht

**Es ist ein Berechnungs- und Formatierungs-Helfer für Preise in drei
Kontexten:**

**1. Artikel-Parser (parse())\
Liest einen Jira-Textfeld-Inhalt im Wiki-Tabellenformat
(\|\|art\|\|koste\|\| \| first\|100\| \|second\|200\|) und zerlegt ihn
in eine Liste von Artikeln mit Name und Preis. Berechnet dabei den
Gesamtpreis.**

**2. Auftrags-Kalkulation (getTaxesAuftrag, getTotalPriceAuftrag)\
Berechnet MwSt. und Gesamtpreis für ein einzelnes Auftragsfeld.**

**3. Rechnungs-Kalkulation (calculateTotalPriceRechnung,
getTaxesRechnung, getFinalPriceRechnung)\
Summiert bis zu 6 Einzelpreise, berechnet MwSt. und Endbetrag.**

**Zahlenformatierung durchgängig deutsch: Komma als Dezimaltrennzeichen,
Punkt als Tausender (1.234,56). Inklusive Workaround für non-breaking
spaces (ASCII 160) die Jira in Tabellenfeldern einfügt.**

**Die gute Nachricht**

**Das Skript enthält keine Jira-API-Aufrufe — es arbeitet nur mit
Strings und Zahlen. Es ist reine Berechnungslogik ohne
ComponentAccessor, ohne Issue-Zugriff, ohne Manager-Objekte.**

**Das Problem in Cloud**

**\$scripting.execute("articles-tool.groovy") ist ein
ScriptRunner-Feature — in Cloud nicht verfügbar. Das Skript kann nicht
mehr so eingebunden werden.**

**Lösungsoptionen**

**Option A — Velocity-Makros (empfohlen, kein Code nötig)**

**Die Berechnungslogik ist in Velocity gut nachbaubar. Der Parser ist
der komplexeste Teil, aber da der Jira-Wiki-Tabelleninhalt in Cloud als
strukturiertes Objekt verfügbar ist, braucht man den String-Parser
möglicherweise gar nicht mehr:**

**velocity**

**\## Preise direkt aus strukturierten Feldern summieren**

**\#set(\$total = 0)**

**\#foreach(\$artikel in \$issues.get(0).fields.customfield_XXXXX)**

**\#set(\$total = \$total + \$artikel.price)**

**\#end**

**Zahlenformatierung in Velocity ist eingeschränkt — Better PDF Cloud
stellt aber \$numberFormatter bereit:**

**velocity**

**\$numberFormatter.format(\$total, "#0.00", "de_DE")**

**Option B — JavaScript in Forge Function vorberechnen**

**Preise in einer Forge Function berechnen und als aufbereiteten String
per Webhook ins Template übergeben. Aufwändiger, aber sauberste
Trennung.**

**Option C — Velocity-Makro als direkter Ersatz**

**velocity**

**\## Makro für deutsche Zahlenformatierung**

**\#macro(formatDE \$zahl)**

**\## Vorzeichen-Logik + Komma/Punkt tauschen**

**\#set(\$formatted = \$numberFormatter.format(\$zahl, "#,##0.00",
"de_DE"))**

**\$formatted**

**\#end**

**\## MwSt-Berechnung**

**\#macro(calcMwSt \$netto \$satz)**

**\#set(\$mwst = \$netto \* \$satz / 100)**

**\#set(\$brutto = \$netto + \$mwst)**

**\#end**

**Auswirkung auf die Zeitschätzung**

**Das Skript ist kein Showstopper — aber es braucht Aufmerksamkeit:**

| **Aspekt**                       | **Bewertung**                     |
|----------------------------------|-----------------------------------|
| **Komplexität der Logik**        | **mittel**                        |
| **Jira-API-Abhängigkeit**        | **keine — erleichtert Migration** |
| **Ersatz durch Velocity-Makros** | **gut machbar**                   |
| **Aufwand für Ersatz**           | **2–3 Tage**                      |
| **Risiko**                       | **niedrig bis mittel**            |

**Die Gesamtschätzung von 28–30 Tagen bleibt damit realistisch. Das
Skript ist kein Grund zur Sorge — der kritischere Punkt bleibt
\$pdfContent.linkCollectionByIssue().**

#### Aktion Umschreiben articles-tool.groovy

von Velocity-Makro-Version 🡺 als Cloud-Ersatz.

## acceptation-confirmation-fo.vm

Das ist ein **Velocity/XSL-FO-Template** für das Jira PDF View Plugin
(DC). Bei einem Wechsel auf Jira Cloud gibt es erhebliche
Einschränkungen – hier die wichtigsten Punkte:

**1. PDF View Plugin – nicht verfügbar in Cloud**

Das **Jira PDF View Plugin** (von Appfire/Bob Swift) existiert **nicht
für Jira Cloud**. Du brauchst ein alternatives App, z. B.:

- **PDF Export for Jira Cloud** (verschiedene Anbieter im Atlassian
  Marketplace prüfen)

- Oder die PDF-Generierung extern (z. B. via Automation + Webhook +
  externes System)

Das gesamte Template müsste für das neue Tool **komplett neu
geschrieben** werden.

**2. Velocity-Templating entfällt**

Cloud-Apps unterstützen kein Velocity (.vm). Die gesamte Template-Logik
(#set, \#if, \#elseif, Schleifen etc.) müsste in der Templatesprache des
Ersatz-Tools neu implementiert werden.

**3. Custom Field-Zugriff anders**

In DC wird direkt über
\$customFieldManager.getCustomFieldObject("customfield_XXXXX")
zugegriffen. In Cloud:

- Custom Field IDs können sich beim Migration **ändern**

- Der Zugriff erfolgt je nach App über andere Mechanismen (z. B. REST
  API, eigene Template-Variablen)

- Alle customfield_XXXXX-Referenzen müssen **geprüft und neu gemappt**
  werden

**4. \$scripting.execute(...) entfällt**

\$scripting.execute("articles-tool.groovy")

ScriptRunner-Groovy-Skripte laufen zwar auch in Cloud, aber der Aufruf
aus einem PDF-Template heraus ist so **nicht möglich**. Die Logik aus
articles-tool.groovy (Zahlenformatierung, Steuerberechnung, Gesamtpreis)
müsste anderweitig eingebunden werden.

**5. XSL-FO entfällt**

Das Layout ist komplett in **XSL-FO** geschrieben (fo:block, fo:table,
fo:region-body etc.). Cloud-PDF-Tools arbeiten typischerweise mit
**HTML/CSS** oder proprietären Template-Formaten. Das gesamte Layout
(Tabellen, Abstände, Schriftgrößen, Kopf-/Fußzeilen) muss neu gebaut
werden.

**6. Bilder/Logos**

\$logoImageUrl_Banner

\$FZ_ImageUrl

Diese Variablen werden in DC aus dem Plugin-Kontext befüllt. In Cloud
muss geprüft werden, wie das Ersatz-Tool auf Anhänge oder externe
Bild-URLs zugreift.

**7. \$pdfRenderer.asRendered(...) entfällt**

Die Methode rendert Rich-Text-Felder (z. B. customfield_10426,
customfield_10427). In Cloud ist das Textformat **Atlassian Document
Format (ADF)** statt Wiki-Markup – das muss das neue Tool entsprechend
unterstützen.

**Fazit**

| **Komponente**   | **DC**               | **Cloud**             |
|------------------|----------------------|-----------------------|
| PDF-Plugin       | Jira PDF View Plugin | Ersatz-App nötig      |
| Template-Sprache | Velocity + XSL-FO    | HTML/CSS o. ä.        |
| Groovy-Skripte   | direkt einbindbar    | separate Lösung nötig |
| Custom Fields    | direkt per ID        | IDs neu mappen        |
| Rich-Text        | Wiki-Markup          | ADF                   |

Ein **1:1-Wechsel ist nicht möglich** – das Template muss für Cloud von
Grund auf neu entwickelt werden. Die fachliche Logik (Steuerberechnung,
Rahmenauftrag-Unterscheidung, Regionalleiter-Daten) kann aber als
Vorlage dienen.

## Better PDF Exporter for Jira von Appfire

Gibt es auch für Jira Cloud. Das ändert einiges:

------------------------------------------------------------------------

**Was bleibt erhalten**

- **Velocity-Templating** wird von Better PDF auch in Cloud unterstützt

- Die grundsätzliche **Template-Struktur** bleibt ähnlich

- **XSL-FO** wird ebenfalls weiterhin verwendet

------------------------------------------------------------------------

**Was sich trotzdem ändert**

**1. Custom Field IDs**

Die customfield_XXXXX-IDs können sich bei der Migration ändern – alle
Felder müssen nach der Migration **geprüft und ggf. angepasst** werden.

**2. \$scripting.execute("articles-tool.groovy")**

Das ist ein ScriptRunner-Aufruf – dieser funktioniert in Better PDF
Cloud **nicht** so. Die Logik aus articles-tool.groovy (Formatierung,
Steuerberechnung) müsste entweder:

- als **Velocity-Makro** direkt ins Template integriert werden, oder

- über einen anderen Mechanismus bereitgestellt werden

**3. \$pdfRenderer.asRendered(...)**

Diese DC-spezifische Methode existiert in Better PDF Cloud
möglicherweise **nicht oder unter anderem Namen** – hier unbedingt die
Cloud-Dokumentation von Better PDF prüfen.

**4. Rich-Text / ADF**

In Cloud ist das Textformat **ADF** statt Wiki-Markup. Better PDF Cloud
bietet dafür eigene Render-Methoden an, die aber anders heißen können
als in DC.

------------------------------------------------------------------------

### Empfehlung

Den größten Aufwand verursachen die **Groovy-Integration** und der
**Rich-Text-Renderer** – das würde ich als erstes in der
Appfire-Dokumentation für die Cloud-Version prüfen, bevor man mit der
Migration des Templates beginnt.

## **Aufwandsschätzung (**acceptation-confirmation-fo.vm)

**Szenario 1: Better PDF Cloud funktioniert**

| **Aufgabe**                                  | **Aufwand**    |
|----------------------------------------------|----------------|
| Custom Field IDs prüfen & anpassen           | 2–4 Std.       |
| \$scripting.execute / articles-tool ersetzen | 4–8 Std.       |
| \$pdfRenderer.asRendered prüfen & anpassen   | 2–4 Std.       |
| Layout-Tests & Feintuning                    | 4–8 Std.       |
| **Gesamt**                                   | **12–24 Std.** |

Das ist überschaubar – das bestehende Template ist die Basis, es geht
hauptsächlich um **Anpassung und Testen**.

**Szenario 2: Komplette Neuentwicklung**

| **Aufgabe** | **Aufwand** |
|----|----|
| Ersatz-Tool evaluieren & einrichten | 4–8 Std. |
| Template-Sprache einarbeiten (HTML/CSS o. ä.) | 8–16 Std. |
| Layout neu aufbauen (Logo, Kopf, Adresse, Tabellen, Fußzeile) | 16–24 Std. |
| Fachlogik neu implementieren (Steuer, Rahmenauftrag, RL-Daten) | 8–16 Std. |
| Custom Fields neu mappen | 2–4 Std. |
| Rich-Text-Rendering lösen | 4–8 Std. |
| Tests & Feintuning | 8–16 Std. |
| **Gesamt** | **50–92 Std.** |

**Fazit**

|            | **Better PDF Cloud** | **Neuentwicklung** |
|:----------:|:--------------------:|:------------------:|
|  Aufwand   |      ~2–3 Tage       |    ~2–3 Wochen     |
|   Risiko   |        gering        |    mittel–hoch     |
| Empfehlung |  **zuerst prüfen**   |  nur als Fallback  |

Der erste Schritt sollte daher sein, eine **Testinstanz mit Better PDF
Cloud** aufzusetzen und das bestehende Template probeweise einzuspielen
– das zeigt schnell, was funktioniert und was nicht.

[^1]: Die ich kenne!

[^2]: Bis nur genutzt. Nie analysiert.
