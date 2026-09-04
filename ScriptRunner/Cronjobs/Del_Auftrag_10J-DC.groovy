/**
 * ScriptRunner for Jira Cloud - Scheduled Job (Cronjob)
 * 
 * Zweck: Automatische Löschung von Aufträgen (Projekt AUFTRAG), die älter als 10 Jahre sind,
 * inkl. verknüpfter Issues (Folgeaufträge, Rechnungen, Dokumente) und Schutz für Klienten.
 */

// -------------------------------------------------------------
// KONFIGURATION
// -------------------------------------------------------------
boolean DRY_RUN = true             // Auf 'false' setzen, wenn real gelöscht werden soll!
int MAX_ISSUES_PER_RUN = 25        // Batch-Größe pro Durchlauf (Schutz vor Timeouts)
int YEARS_THRESHOLD = 10           // Schwellenwert in Jahren (520 Wochen)

// Verknüpfte Projekte, deren verknüpfte Issues mitgelöscht werden sollen
List<String> LINKED_PROJECTS_TO_DELETE = ["RECHNUNG", "AUFDO", "AB", "BEZ"]

// Sollen Klienten gelöscht werden (nur wenn keine weiteren Aufträge am Klienten hängen)?
boolean DELETE_KLIENTEN = true

logger.info("=== START CRONJOB: AUFTRAG-LÖSCHUNG (> ${YEARS_THRESHOLD} Jahre) | DRY_RUN = ${DRY_RUN} ===")

// 1. JQL-Abfrage für Aufträge, die älter als 10 Jahre (520 Wochen) sind
String jql = "project = 'AUFTRAG' AND created <= '-520w' ORDER BY created ASC"

def searchResp = get('/rest/api/3/search')
        .queryString([
            jql: jql,
            maxResults: MAX_ISSUES_PER_RUN,
            fields: "summary,created,issuelinks,subtasks,project"
        ])
        .header('Accept', 'application/json')
        .asJson()

if (!searchResp.body || searchResp.status != 200) {
    logger.error("Fehler bei der JQL-Suche: ${searchResp.status} ${searchResp.body}")
    return
}

def issues = searchResp.body.object.issues
logger.info("Gefundene alte Aufträge: ${issues.size()}")

issues.each { issue ->
    String issueKey = issue.key
    String summary = issue.fields.summary
    String created = issue.fields.created
    
    logger.info("--> Verarbeite Auftrag: ${issueKey} ('${summary}', Erstellt: ${created})")
    
    Set<String> issuesToDelete = [] as Set
    issuesToDelete.add(issueKey)
    
    // a) Unteraufgaben (Subtasks) erfassen
    if (issue.fields.subtasks) {
        issue.fields.subtasks.each { sub ->
            issuesToDelete.add(sub.key as String)
        }
    }
    
    // b) Verknüpfte Issues erfassen (Rechnungen, Folgeaufträge, Berichte, Klienten)
    if (issue.fields.issuelinks) {
        issue.fields.issuelinks.each { link ->
            def linkedIssue = link.inwardIssue ?: link.outwardIssue
            if (linkedIssue) {
                String linkedKey = linkedIssue.key
                String linkedProjectKey = linkedKey.split("-")[0]
                
                // Folgeaufträge oder verknüpfte Rechnungen/Dokumente
                if (linkedProjectKey == "AUFTRAG" || LINKED_PROJECTS_TO_DELETE.contains(linkedProjectKey)) {
                    issuesToDelete.add(linkedKey)
                } 
                // Klienten prüfen
                else if (linkedProjectKey == "KLIENTEN") {
                    if (DELETE_KLIENTEN) {
                        boolean hasOtherOrders = checkClientHasOtherOrders(linkedKey, issueKey)
                        if (!hasOtherOrders) {
                            issuesToDelete.add(linkedKey)
                        } else {
                            logger.info("   [INFO] Klient ${linkedKey} wird NICHT gelöscht, da noch weitere Aufträge existieren.")
                        }
                    }
                }
            }
        }
    }
    
    // c) Löschvorgang ausführen
    logger.info("   Löschliste für ${issueKey}: ${issuesToDelete}")
    
    issuesToDelete.each { targetKey ->
        if (DRY_RUN) {
            logger.info("   [DRY-RUN] Würde Issue löschen: ${targetKey} (inkl. Anhänge)")
        } else {
            def deleteResp = delete("/rest/api/3/issue/${targetKey}")
                    .queryString([deleteSubtasks: "true"])
                    .asJson()
            if (deleteResp.status == 204) {
                logger.info("   [ERFOLG] Gelöscht: ${targetKey}")
            } else {
                logger.error("   [FEHLER] Fehler beim Löschen von ${targetKey}: ${deleteResp.status} ${deleteResp.body}")
            }
        }
    }
}

logger.info("=== ENDE CRONJOB: AUFTRAG-LÖSCHUNG ===")

/**
 * Hilfsfunktion: Prüft, ob ein Klient noch mit anderen Aufträgen verknüpft ist.
 */
boolean checkClientHasOtherOrders(String klientKey, String currentAuftragKey) {
    def resp = get('/rest/api/3/issue/' + klientKey)
            .queryString([fields: "issuelinks"])
            .header('Accept', 'application/json')
            .asJson()
            
    if (resp.status != 200 || !resp.body) return true // Sicherhaltshalber behalten
    
    def links = resp.body.object.fields.issuelinks
    if (!links) return false
    
    boolean otherFound = false
    links.each { link ->
        def other = link.inwardIssue ?: link.outwardIssue
        if (other && other.key != currentAuftragKey && (other.key as String).startsWith("AUFTRAG-")) {
            otherFound = true
        }
    }
    return otherFound
}