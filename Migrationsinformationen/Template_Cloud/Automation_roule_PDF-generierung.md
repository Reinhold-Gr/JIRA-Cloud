# Auftragsbestätigung (Acceptation) PDF generieren

*Web-Anfrage senden

Durch diese Aktion wird eine HTTP-Anfrage an die angegebene URL gesendet.

# URL der Web-Anfrage* (erforderlich)
https://jpdfc.midori.systems/rest/v1/automation

Die Anfrageparameter müssen für URLs kodiert sein. 
Für Smart-Values sollte Folgendes verwendet werden: {{value.urlEncode}}.

## Benutzerdefinierte Daten:
client: "{{rule.name}}"
export:
  view: "AS_acceptation-confirmation"
  viewContext: SINGLE_ISSUE_VIEW
  columnConfiguration: USER_DEFAULT
  jql: "key = {{issue.key}}"
  filename: "{{issue.key}}_Auftragsbestätigung.pdf"
actions:
  - name: Attach PDF to issue
    type: ATTACH
    toIssueKey: "{{issue.key}}"
  - name: Call back to Jira Cloud Automation
    type: CALL
    url: https://api-private.atlassian.com/automation/webhooks/jira/a/101e4b0b-81f1-417e-842e-7a47405e08f8/01a010ac-ae62-7f86-941c-19b06cad6c10
    secret:  f55

