// =====================================================================
// Template-Diagnose für initial-confirmation-fo.vm (Cloud-Version)
// =====================================================================
// ScriptRunner for Jira Cloud – Script Console
//
// Zweck: Alle Custom-Field-Werte ausgeben, die das PDF-Template
//        "Empfangsbestätigung" benötigt, um Fehler schnell zu finden.
//
// Verwendung: Issue-Key unten anpassen und im Script Console ausführen.
// =====================================================================

// ===== KONFIGURATION =====
def issueKey = "AUFTRAG-26376"   // <<< Hier den Auftrags-Key eintragen
// =========================

def separator = "=" * 70

// --- 1. Auftrags-Issue laden ---
def auftragResp = get("/rest/api/3/issue/${issueKey}")
        .queryString("expand", "names")
        .queryString("fields", "*all")
        .asObject(Map)

if (auftragResp.status != 200) {
    logger.error("❌ Issue ${issueKey} konnte nicht geladen werden (HTTP ${auftragResp.status})")
    return "FEHLER: Issue nicht gefunden"
}

def auftrag = auftragResp.body
def fields = auftrag.fields
def fieldNames = auftrag.names  // ID → Name Mapping

logger.info(separator)
logger.info("📋 AUFTRAG: ${issueKey}")
logger.info("   Summary: ${fields.summary}")
logger.info("   Status:  ${fields.status?.name}")
logger.info("   Projekt: ${fields.project?.name}")
logger.info(separator)

// --- Auftrags-Felder ---
logger.info("")
logger.info("📄 AUFTRAGS-FELDER (für Template):")
logger.info("-" * 50)

def auftragsFelder = [
    "customfield_10200": "Auftragsnummer",
    "customfield_10263": "Info zusenden (Ja/Nein)",
    "customfield_10379": "Beratungsfall (Multi-Select)",
    "customfield_10372": "Fachthemen (Multi-Select)",
    "customfield_10393": "Art des Unternehmens (Multi-Select)",
    "customfield_10461": "Branche Details",
    "customfield_10464": "Wünsche / Erläuterung",
    "customfield_10421": "Erstkontakt (Multi-Select)",
    "customfield_10453": "Auftragsart (Rahmenauftrag?)"
]

auftragsFelder.each { cfId, label ->
    def val = fields[cfId]
    def displayVal = formatFieldValue(val)
    def jiraName = fieldNames?."${cfId}" ?: "(unbekannt)"
    logger.info("   ${cfId} [${jiraName}]")
    logger.info("   └─ ${label}: ${displayVal}")
    logger.info("")
}

// --- 2. Verlinkte Issues auflösen ---
logger.info(separator)
logger.info("🔗 VERLINKTE ISSUES:")
logger.info("-" * 50)

def klientKey = null
def regionKey = null
def rechnungKey = null
def abschlussberichtKey = null

def issueLinks = fields.issuelinks ?: []

issueLinks.each { link ->
    def linkedIssue = link.inwardIssue ?: link.outwardIssue
    if (linkedIssue) {
        def projectName = linkedIssue.fields?.issuetype?.name ?: "?"
        def projKey = linkedIssue.key?.split("-")?.first() ?: ""
        
        // Projekt über den Issue-Key oder Summary identifizieren
        logger.info("   ${link.type?.name}: ${linkedIssue.key} (${linkedIssue.fields?.summary})")
        
        // Verlinkte Issues nach Projekt-Präfix zuordnen
        // HINWEIS: Projekt-Keys ggf. anpassen!
        if (linkedIssue.key?.startsWith("KL") || linkedIssue.fields?.project?.name == "Klienten") {
            klientKey = linkedIssue.key
        }
        if (linkedIssue.key?.startsWith("REG") || linkedIssue.fields?.project?.name == "Region") {
            regionKey = linkedIssue.key
        }
        if (linkedIssue.key?.startsWith("RE") || linkedIssue.fields?.project?.name == "Rechnungen") {
            rechnungKey = linkedIssue.key
        }
        if (linkedIssue.key?.startsWith("AB") || linkedIssue.fields?.project?.name == "Abschlussbericht") {
            abschlussberichtKey = linkedIssue.key
        }
    }
}

logger.info("")
logger.info("   Zuordnung:")
logger.info("   • Klient:           ${klientKey ?: '⚠️ NICHT GEFUNDEN'}")
logger.info("   • Region:           ${regionKey ?: '⚠️ NICHT GEFUNDEN'}")
logger.info("   • Rechnung:         ${rechnungKey ?: '(nicht verlinkt)'}")
logger.info("   • Abschlussbericht: ${abschlussberichtKey ?: '(nicht verlinkt)'}")

// --- 3. Klient-Issue laden ---
if (klientKey) {
    logger.info("")
    logger.info(separator)
    logger.info("👤 KLIENT: ${klientKey}")
    logger.info("-" * 50)

    def klientResp = get("/rest/api/3/issue/${klientKey}")
            .queryString("fields", "*all")
            .asObject(Map)

    if (klientResp.status == 200) {
        def kf = klientResp.body.fields

        def klientFelder = [
            "customfield_10388": "Firmenname",
            "customfield_10392": "Vorname",
            "customfield_10395": "Nachname",
            "customfield_10399": "Straße, Hausnummer",
            "customfield_10389": "PLZ",
            "customfield_10391": "Ort",
            "customfield_10405": "Land (Select)",
            "customfield_10390": "Rechtsform",
            "customfield_10387": "Telefon",
            "customfield_10398": "Weitere Telefonnummer",
            "customfield_10396": "Fax",
            "customfield_10400": "E-Mail",
            "customfield_10329": "Internet / Website"
        ]

        klientFelder.each { cfId, label ->
            def val = kf[cfId]
            def displayVal = formatFieldValue(val)
            logger.info("   ${cfId}: ${label}")
            logger.info("   └─ ${displayVal}")
            logger.info("")
        }

        // Adressblock wie im Template
        logger.info("   📬 Adresse (wie im PDF):")
        logger.info("   ${formatFieldValue(kf['customfield_10388'])}")
        logger.info("   ${formatFieldValue(kf['customfield_10392'])} ${formatFieldValue(kf['customfield_10395'])}")
        logger.info("   ${formatFieldValue(kf['customfield_10399'])}")
        logger.info("   ${formatFieldValue(kf['customfield_10389'])} ${formatFieldValue(kf['customfield_10391'])}")
        def land = kf['customfield_10405']
        if (land && extractValue(land) != "Deutschland") {
            logger.info("   ${extractValue(land)}")
        }
    } else {
        logger.warn("   ⚠️ Klient-Issue konnte nicht geladen werden (HTTP ${klientResp.status})")
    }
}

// --- 4. Region-Issue laden ---
if (regionKey) {
    logger.info("")
    logger.info(separator)
    logger.info("🗺️ REGION: ${regionKey}")
    logger.info("-" * 50)

    def regionResp = get("/rest/api/3/issue/${regionKey}")
            .queryString("fields", "*all")
            .asObject(Map)

    if (regionResp.status == 200) {
        def rf = regionResp.body.fields

        def regionFelder = [
            "customfield_10432": "Regionsname",
            "customfield_10306": "Name Regionalleiter",
            "customfield_10344": "Straße RL (entfällt seit V2.4)",
            "customfield_10305": "Ort RL (entfällt seit V2.4)",
            "customfield_10302": "PLZ RL (entfällt seit V2.4)",
            "customfield_10310": "Telefon RL (entfällt seit TIC-371)",
            "customfield_10323": "Fax RL (entfällt seit TIC-371)",
            "customfield_10298": "E-Mail RL"
        ]

        regionFelder.each { cfId, label ->
            def val = rf[cfId]
            def displayVal = formatFieldValue(val)
            logger.info("   ${cfId}: ${label}")
            logger.info("   └─ ${displayVal}")
            logger.info("")
        }

        // Absenderblock wie im Template
        logger.info("   📮 Absender (wie im PDF):")
        logger.info("   Regionalleitung ${formatFieldValue(rf['customfield_10432'])}")
        logger.info("   ${formatFieldValue(rf['customfield_10306'])}")
        logger.info("   M: ${formatFieldValue(rf['customfield_10298'])}")
    } else {
        logger.warn("   ⚠️ Region-Issue konnte nicht geladen werden (HTTP ${regionResp.status})")
    }
}

// --- 5. Zusammenfassung / Warnungen ---
logger.info("")
logger.info(separator)
logger.info("✅ ZUSAMMENFASSUNG")
logger.info(separator)

def warnungen = []

if (!klientKey) warnungen.add("Kein Klient verlinkt – Adressblock wird leer!")
if (!regionKey) warnungen.add("Keine Region verlinkt – Absender wird leer!")
if (!fields["customfield_10200"]) warnungen.add("Auftragsnummer (CF10230) ist leer!")
if (!fields["customfield_10263"]) warnungen.add("Info zusenden (CF12602) ist leer – Abschnitt 'Weitere Informationen' kann fehlschlagen!")
if (!fields["customfield_10379"]) warnungen.add("Beratungsfall (CF11301) ist leer – Abschnitt wird leer dargestellt")

if (warnungen.isEmpty()) {
    logger.info("   ✅ Alle template-relevanten Felder sind vorhanden.")
    logger.info("   Das PDF sollte korrekt generiert werden können.")
} else {
    logger.warn("   ⚠️ ${warnungen.size()} Warnung(en):")
    warnungen.each { w ->
        logger.warn("   • ${w}")
    }
}

logger.info("")
logger.info("   Umgebung (common-elements): Wird automatisch über \$baseUrl erkannt")
logger.info("   Debug-Modus im Template: Wird aus Umgebung abgeleitet (TEST=ein, PROD=aus)")
logger.info(separator)

return "Diagnose abgeschlossen – siehe Log-Ausgabe oben"

// =====================================================================
// Hilfsfunktionen
// =====================================================================

def formatFieldValue(val) {
    if (val == null) return "(leer/null)"
    
    // Select-Feld (einzeln)
    if (val instanceof Map && val.containsKey("value")) {
        return val.value
    }
    
    // Multi-Select oder Cascading
    if (val instanceof List) {
        if (val.isEmpty()) return "(leere Liste)"
        return val.collect { item ->
            if (item instanceof Map && item.containsKey("value")) {
                return item.value
            }
            return item.toString()
        }.join(", ")
    }
    
    // ADF-Content (Rich Text)
    if (val instanceof Map && val.containsKey("content")) {
        return extractTextFromAdf(val)
    }
    
    return val.toString()
}

def extractValue(val) {
    if (val instanceof Map && val.containsKey("value")) return val.value
    return val?.toString() ?: ""
}

def extractTextFromAdf(adf) {
    if (adf == null) return ""
    def text = new StringBuilder()
    
    if (adf.type == "text") {
        text.append(adf.text ?: "")
    }
    
    if (adf.content) {
        adf.content.each { node ->
            text.append(extractTextFromAdf(node))
        }
    }
    
    return text.toString() ?: "(ADF-Inhalt vorhanden)"
}

