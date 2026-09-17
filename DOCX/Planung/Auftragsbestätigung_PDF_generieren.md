# Jira Automationsregel: „Auftragsbestätigung (Acceptation) PDF generieren“

## Übersicht

| Feld | Wert |
|---|---|
| **Regelname** | Auftragsbestätigung (Acceptation) PDF generieren |
| **Status** | 🔴 **DISABLED** (deaktiviert) |
| **Rule-ID** | 5900712 |
| **Beschreibung** | Hängt bei einem „Issue commented"-Event ein PDF-Dokument an ein Jira-Issue an |
| **Erstellt** | 03.07.2026 (Timestamp 1788523818753) |
| **Zuletzt geändert** | 13.07.2026 (Timestamp 1789466083671) |
| **Autor (Account-ID)** | 712020:aef3309b-997f-4a5f-a89e-d06ccd190154 |
| **Scope** | Site-weit, keine Projekt-Einschränkung |
| **Zugriff** | UNRESTRICTED (Lesen & Schreiben) |

---

## 1. Trigger

**Typ:** `jira.issue.event.trigger:commented`
→ Die Regel feuert, sobald ein **Kommentar zu einem Issue** hinzugefügt wird (site-weit, keine weiteren Filter/Bedingungen).

---

## 2. Aktion 1 – Webhook an Midori (PDF-Generator)

Ruft den externen Dienst **jpdfc.midori.systems** auf, um ein PDF zu erzeugen:

- **URL:** `https://jpdfc.midori.systems/rest/v1/automation`
- **Methode:** POST
- **Content-Type:** `application/x-yaml`

**Header:**
- `X-Midori-Base-Url`: `https://aktivsenioren-sandbox.atlassian.net`
- `X-Midori-User-Email`: `reinhold.gritsch@aktivsenioren.de`
- `X-Midori-Api-Token`: *(als „secure" markiert, Wert nicht sichtbar)*

**YAML-Body (Export-Konfiguration):**
```yaml
client: "{{rule.name}}"
export:
  view: "AS_acceptation-confirmation"
  viewContext: SINGLE_ISSUE_VIEW
  columnConfiguration: USER_DEFAULT
  jql: "key = {{issue.key}}"
  filename: "{{issue.key}}_Auftragsbestaetigung.pdf"
actions:
  - name: Attach PDF to issue
    type: ATTACH
    toIssueKey: "{{issue.key}}"
  - name: Call back to Jira Cloud Automation
    type: CALL
    url: https://api-private.atlassian.com/automation/webhooks/jira/a/101e4b0b-81f1-417e-842e-7a47405e08f8/01a010ac-ae62-7f86-941c-19b06cad6c10
    secret: f55...

**Ablauf:** Midori exportiert die View **„AS_acceptation-confirmation"** für genau das auslösende Issue, benennt das PDF nach dem Issue-Key und hängt es ans Issue an. Danach meldet sich Midori per Callback an die Jira-Automation zurück (asynchrones Muster: Webhook raus → Callback rein, `responseEnabled: true`, `continueOnErrorEnabled: false`).

---

## 3. Aktion 2 – Log-Eintrag

Protokolliert den Start des PDF-Jobs:
```
PDF automation started. (ID: {{webResponse.body.messageId}} Log command: {{webResponse.body.logCommand}})
```

---

## 4. Sonstiges

- **Fehlerbenachrichtigung:** `FIRSTERROR` (nur beim ersten Fehler wird benachrichtigt)
- **Labels:** ID 153053
- **Tags:** markiert als kürzlich aktualisiert (`IS_RULE_UPDATED`)
- **Billing/Lifecycle-Home:** Jira Software, Site `079915c2-8a53-4ec6-a41b-cdb5495bceb5`

---

⚠️ **Sicherheitshinweis:** Im JSON steht der Callback-**Secret-Key im Klartext** (`f550067136151815a8ab886fd15a02b47202683b`). Falls dieser Export irgendwo geteilt oder abgelegt wird, solltest du den Key in der Automationsregel rotieren lassen, da er sonst kompromittiert ist.

Möchtest du, dass ich das Ganze noch als Diagramm (Trigger → Webhook → Callback → Log) visualisiere, oder reicht dir die Tabellen-/Textform?