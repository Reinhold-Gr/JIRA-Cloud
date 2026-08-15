// =====================================================================
// PDF-Generierung für AS_initial-confirmation-fo.vm (Cloud-Version)
// =====================================================================
// ScriptRunner for Jira Cloud – Script Console
// Datei: Test_confirmation.groovy
//
// Zweck:
//   1. Diagnose: Prüfen ob alle Daten vorhanden sind
//   2. PDF generieren via Better PDF Exporter REST API
//   3. PDF als Attachment am Auftrag-Issue speichern
//
// Voraussetzung:
//   - Better PDF Exporter Cloud (Midori) installiert
//   - Template "initial-confirmation-fo.vm" eingerichtet
//   - Gemeinsames Template "AS_auftrag-common-elements-fo-cloud.vm" vorhanden
//
// =====================================================================

// ===== KONFIGURATION =====
def issueKey        = "AUFTRAG-26376"
def templateName    = "initial-confirmation-fo.vm"
def outputFilename  = "26376_Auftrag.pdf"
def runDiagnostics  = true    // Diagnose vorschalten (true/false)
def attachToIssue   = true    // PDF als Attachment anhängen
// =========================

def separator = "=" * 70
def warnings = []

// =====================================================================
// SCHRITT 1: Issue laden und validieren
// =====================================================================
logger.info(separator)
logger.info("🚀 PDF-Generierung: ${issueKey} → ${outputFilename}")
logger.info(separator)

def auftragResp = get("/rest/api/3/issue/${issueKey}")
        .queryString("expand", "names")
        .queryString("fields", "*all")
        .asObject(Map)

if (auftragResp.status != 200) {
    logger.error("❌ Issue ${issueKey} nicht gefunden (HTTP ${auftragResp.status})")
    return "FEHLER: Issue nicht gefunden"
}

def auftrag = auftragResp.body
def fields = auftrag.fields
def issueId = auftrag.id   // Numeric ID – wird für PDF-API benötigt

logger.info("✅ Issue geladen: ${issueKey} (ID: ${issueId})")
logger.info("   Summary: ${fields.summary}")
logger.info("   Projekt: ${fields.project?.name} (${fields.project?.key})")

// =====================================================================
// SCHRITT 2: Verlinkte Issues prüfen (Klient + Region sind Pflicht)
// =====================================================================
def klientKey = null
def regionKey = null

def issueLinks = fields.issuelinks ?: []
issueLinks.each { link ->
    def linkedIssue = link.inwardIssue ?: link.outwardIssue
    if (linkedIssue) {
        def projName = linkedIssue.fields?.project?.name ?: ""
        def projKey = linkedIssue.fields?.project?.key ?: ""
        
        if (projName == "Klienten" || projKey.startsWith("KL")) {
            klientKey = linkedIssue.key
        }
        if (projName == "Region" || projKey.startsWith("REG")) {
            regionKey = linkedIssue.key
        }
    }
}

logger.info("   Klient: ${klientKey ?: '❌ NICHT VERLINKT'}")
logger.info("   Region: ${regionKey ?: '❌ NICHT VERLINKT'}")

if (!klientKey) warnings.add("Kein Klient verlinkt – Adressblock wird leer!")
if (!regionKey) warnings.add("Keine Region verlinkt – Absender wird leer!")

// =====================================================================
// SCHRITT 3: Diagnose (optional) – Kurzform
// =====================================================================
if (runDiagnostics) {
    logger.info("")
    logger.info("🔍 DIAGNOSE – Pflichtfelder:")
    logger.info("-" * 50)
    
    // Auftrags-Pflichtfelder
    def pflichtfelderAuftrag = [
        "customfield_10200": "Auftragsnummer",
        "customfield_10263": "Info zusenden",
        "customfield_10379": "Beratungsfall",
        "customfield_10372": "Fachthemen"
    ]
    
    pflichtfelderAuftrag.each { cfId, label ->
        def val = fields[cfId]
        def status = val ? "✅" : "⚠️"
        if (!val) warnings.add("${label} (${cfId}) ist leer!")
        logger.info("   ${status} ${label}: ${formatFieldValue(val)}")
    }
    
    // Klient-Pflichtfelder
    if (klientKey) {
        def klientResp = get("/rest/api/3/issue/${klientKey}")
                .queryString("fields", "*all")
                .asObject(Map)
        
        if (klientResp.status == 200) {
            def kf = klientResp.body.fields
            logger.info("")
            logger.info("   👤 Klient ${klientKey}:")
            
            def pflichtfelderKlient = [
                "customfield_10392": "Vorname",
                "customfield_10395": "Nachname",
                "customfield_10399": "Straße",
                "customfield_10389": "PLZ",
                "customfield_10391": "Ort",
                "customfield_10400": "E-Mail"
            ]
            
            pflichtfelderKlient.each { cfId, label ->
                def val = kf[cfId]
                def status = val ? "✅" : "⚠️"
                if (!val) warnings.add("Klient: ${label} (${cfId}) ist leer!")
                logger.info("   ${status} ${label}: ${formatFieldValue(val)}")
            }
        }
    }
    
    // Region-Pflichtfelder
    if (regionKey) {
        def regionResp = get("/rest/api/3/issue/${regionKey}")
                .queryString("fields", "*all")
                .asObject(Map)
        
        if (regionResp.status == 200) {
            def rf = regionResp.body.fields
            logger.info("")
            logger.info("   🗺️ Region ${regionKey}:")
            
            def pflichtfelderRegion = [
                "customfield_10432": "Regionsname",
                "customfield_10306": "Name RL",
                "customfield_10298": "E-Mail RL"
            ]
            
            pflichtfelderRegion.each { cfId, label ->
                def val = rf[cfId]
                def status = val ? "✅" : "⚠️"
                if (!val) warnings.add("Region: ${label} (${cfId}) ist leer!")
                logger.info("   ${status} ${label}: ${formatFieldValue(val)}")
            }
        }
    }
    
    // Warnungen ausgeben
    logger.info("")
    if (warnings.isEmpty()) {
        logger.info("✅ Alle Pflichtfelder vorhanden – PDF-Generierung möglich")
    } else {
        logger.warn("⚠️ ${warnings.size()} Warnung(en):")
        warnings.each { w -> logger.warn("   • ${w}") }
        logger.warn("   PDF wird trotzdem generiert – betroffene Felder bleiben leer.")
    }
}

// =====================================================================
// SCHRITT 4: PDF generieren via Better PDF Exporter REST API
// =====================================================================
logger.info("")
logger.info(separator)
logger.info("📄 PDF-GENERIERUNG")
logger.info(separator)

// --- Variante A: Standard-Endpunkt Better PDF Exporter Cloud ---
// HINWEIS: Der genaue Endpunkt hängt von der Midori-App-Version ab.
// Falls dieser nicht funktioniert → Variante B versuchen.

def pdfUrl = "/rest/pdf-exporter/1.0/pdf"

logger.info("   Endpoint: ${pdfUrl}")
logger.info("   Issue-ID: ${issueId}")
logger.info("   Template: ${templateName}")

def pdfResp = get(pdfUrl)
        .queryString("issueId", issueId)
        .queryString("templateName", templateName)
        .header("Accept", "application/pdf")
        .asBytes()

// --- Variante B (falls A nicht funktioniert): ---
// Auskommentieren und stattdessen verwenden:
//
// def pdfUrl = "/rest/pdf-exporter/1.0/export"
// def pdfResp = post(pdfUrl)
//         .header("Content-Type", "application/json")
//         .header("Accept", "application/pdf")
//         .body([
//             issueIds: [issueId],
//             templateName: templateName
//         ])
//         .asBytes()
//
// --- Variante C: Falls über Connect-App-Proxy ---
// def addonKey = "com.midori.jira.plugin.pdfview"
// def pdfUrl = "/rest/atlassian-connect/1/addons/${addonKey}/pdf"
// ...

if (pdfResp.status != 200) {
    logger.error("❌ PDF-Generierung fehlgeschlagen!")
    logger.error("   HTTP Status: ${pdfResp.status}")
    logger.error("   Response: ${pdfResp.statusText}")
    logger.error("")
    logger.error("   Mögliche Ursachen:")
    logger.error("   • Template '${templateName}' existiert nicht in Better PDF")
    logger.error("   • Endpunkt ist anders – Variante B/C in Script probieren")
    logger.error("   • App-Berechtigung fehlt")
    logger.error("")
    logger.error("   Tipp: Öffne im Browser:")
    logger.error("   https://aktivsenioren-sandbox.atlassian.net${pdfUrl}?issueId=${issueId}&templateName=${templateName}")
    return "FEHLER: PDF konnte nicht generiert werden (HTTP ${pdfResp.status})"
}

def pdfBytes = pdfResp.body
logger.info("✅ PDF erfolgreich generiert!")
logger.info("   Größe: ${pdfBytes.length} Bytes (${(pdfBytes.length / 1024).intValue()} KB)")

// =====================================================================
// SCHRITT 5: PDF als Attachment anhängen
// =====================================================================
if (attachToIssue && pdfBytes.length > 0) {
    logger.info("")
    logger.info("📎 Attachment hochladen: ${outputFilename}")
    
    // Multipart-Upload an Issue
    def attachResp = post("/rest/api/3/issue/${issueKey}/attachments")
            .header("X-Atlassian-Token", "no-check")
            .field("file", pdfBytes, "application/pdf", outputFilename)
            .asObject(List)
    
    if (attachResp.status == 200) {
        def attachment = attachResp.body?.first()
        logger.info("✅ Attachment erfolgreich!")
        logger.info("   ID:       ${attachment?.id}")
        logger.info("   Filename: ${attachment?.filename}")
        logger.info("   Size:     ${attachment?.size} Bytes")
        logger.info("   URL:      ${attachment?.content}")
    } else {
        logger.error("❌ Attachment fehlgeschlagen (HTTP ${attachResp.status})")
        logger.error("   ${attachResp.statusText}")
    }
}

// =====================================================================
// ERGEBNIS
// =====================================================================
logger.info("")
logger.info(separator)
if (pdfResp.status == 200) {
    logger.info("🎉 FERTIG: ${outputFilename} wurde generiert und angehängt.")
} else {
    logger.info("❌ PDF-Generierung fehlgeschlagen – siehe Fehlerausgabe oben.")
}
logger.info(separator)

return "PDF-Generierung ${pdfResp.status == 200 ? 'erfolgreich' : 'fehlgeschlagen'} – siehe Log"

// =====================================================================
// Hilfsfunktionen
// =====================================================================
def formatFieldValue(val) {
    if (val == null) return "(leer/null)"
    if (val instanceof Map && val.containsKey("value")) return val.value
    if (val instanceof List) {
        if (val.isEmpty()) return "(leere Liste)"
        return val.collect { item ->
            (item instanceof Map && item.containsKey("value")) ? item.value : item.toString()
        }.join(", ")
    }
    if (val instanceof Map && val.containsKey("content")) return "(ADF-Inhalt vorhanden)"
    return val.toString()
}