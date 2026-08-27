import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.mail.Email
import com.atlassian.mail.server.SMTPMailServer

// CONFIGURATION
def issueKey = "AUFTRAG-26495"
def empfaengerEmail = "reinhold.gritsch@aktivsenioren.de"

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager
def issue = issueManager.getIssueObject(issueKey)

if (!issue) {
    log.error("Issue ${issueKey} wurde nicht gefunden!")
    return
}

List<CustomField> customFields = customFieldManager.customFieldObjects

StringBuilder sb = new StringBuilder()
sb.append("Customfield ID\tFeldname\tWert\n")
sb.append("------------------------------------------------------------\n")

customFields.each { CustomField cf ->
    def value = issue.getCustomFieldValue(cf)
    if (value != null) {
        sb.append("${cf.id}\t${cf.name}\t${value.toString()}\n")
    }
}

String resultText = sb.toString()
log.info(resultText)

// E-Mail über den Jira-internen SMTP-Server versenden
SMTPMailServer mailServer = ComponentAccessor.mailServerManager.defaultSMTPMailServer
if (mailServer) {
    Email email = new Email(empfaengerEmail)
    email.setSubject("Customfields Übersicht für ${issueKey}")
    email.setBody(resultText)
    mailServer.send(email)
    log.info("E-Mail gesendet an ${empfaengerEmail}")
} else {
    log.error("Kein SMTP Mail-Server in Jira konfiguriert!")
}

return resultText