#!/usr/bin/env groovy
/*
 * ExtractCFs.groovy
 * ---------------------------------------------------------------
 * 1. Liest eine Eingabedatei (Pfad als Argument oder interaktive Abfrage)
 * 2. Durchsucht den Inhalt nach allen Vorkommen von "customfield-nnnnn" (5-stellig)
 * 3. Fragt für jede gefundene Field-ID die Metadaten (Name, Typ, Schema) über die
 *    JIRA Cloud REST API (Sandbox) ab, UND den tatsächlichen Wert aus einem
 *    konkreten Issue (Issue-Key als 2. Argument oder interaktive Abfrage)
 * 4. Schreibt das Ergebnis nach CF_<Name der Eingabedatei>.txt (gleiches Verzeichnis)
 *
 * Aufruf:
 *   groovy ExtractCFs.groovy <Pfad-zur-Datei> <ISSUE-KEY>
 *   (fehlende Argumente werden interaktiv abgefragt, z.B. groovy ExtractCFs.groovy)
 *
 * Base-URL ist fest auf die Sandbox voreingestellt (via JIRA_BASE_URL überschreibbar).
 * Benötigte Umgebungsvariablen (alternativ interaktive Abfrage, falls nicht gesetzt):
 *   Sandbox_USER        Login-E-Mail des API-Users
 *   JIRA_API_TOKEN_CL   API-Token von https://id.atlassian.com/manage-profile/security/api-tokens
 *   JIRA_BASE_URL       optional, überschreibt die Sandbox-Default-URL
 *
 * Keine externen Dependencies nötig (nur JDK: java.net.http.HttpClient, groovy.json).
 * ---------------------------------------------------------------
 */

import groovy.json.JsonSlurper
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Base64

// -----------------------------------------------------------------
// 1. Eingabedatei ermitteln und einlesen
// -----------------------------------------------------------------

String inputPathArg = (args && args.length > 0) ? args[0] : null

if (!inputPathArg) {
    print "Pfad zur Eingabedatei: "
    inputPathArg = System.console() ? System.console().readLine() : new BufferedReader(new InputStreamReader(System.in)).readLine()
}

Path inputPath = Paths.get(inputPathArg.trim())

if (!Files.exists(inputPath)) {
    System.err.println("FEHLER: Datei nicht gefunden: ${inputPath.toAbsolutePath()}")
    System.exit(1)
}

String issueKeyArg = (args && args.length > 1) ? args[1] : null
if (!issueKeyArg) {
    print "Issue-Key (z.B. PROJ-123): "
    issueKeyArg = System.console() ? System.console().readLine() : new BufferedReader(new InputStreamReader(System.in)).readLine()
}
String issueKey = issueKeyArg?.trim()
if (!issueKey) {
    System.err.println("FEHLER: Es wird ein Issue-Key benötigt.")
    System.exit(1)
}

String content = new String(Files.readAllBytes(inputPath), StandardCharsets.UTF_8)

// -----------------------------------------------------------------
// 2. Alle customfield-nnnnn (5-stellig) extrahieren, eindeutig + sortiert
// -----------------------------------------------------------------

def matcher = (content =~ /customfield[_-](\d{5})/)
Set<String> fieldIds = new TreeSet<>()
matcher.each { fullMatch, id -> fieldIds << id }

if (fieldIds.isEmpty()) {
    println "Keine customfield_nnnnn / customfield-nnnnn Referenzen in ${inputPath.getFileName()} gefunden. Es wird keine Ausgabedatei erzeugt."
    System.exit(0)
}

println "Gefundene Custom-Field-IDs (${fieldIds.size()}): ${fieldIds.join(', ')}"

// -----------------------------------------------------------------
// 3. JIRA-Zugangsdaten ermitteln (ENV, sonst interaktiv)
// -----------------------------------------------------------------

String baseUrl = System.getenv("JIRA_BASE_URL") ?: "https://aktivsenioren-sandbox.atlassian.net"
String email = System.getenv("Sandbox_USER")
String apiToken = System.getenv("JIRA_API_TOKEN_CL")

def askIfMissing = { String current, String prompt ->
    if (current) return current
    print prompt
    return System.console() ? System.console().readLine() : new BufferedReader(new InputStreamReader(System.in)).readLine()
}

baseUrl = askIfMissing(baseUrl, "JIRA Base-URL [${baseUrl}]: ")?.trim() ?: baseUrl
email = askIfMissing(email, "JIRA E-Mail (Sandbox_USER): ")?.trim()
apiToken = askIfMissing(apiToken, "JIRA API-Token (JIRA_API_TOKEN_CL): ")?.trim()

if (!baseUrl || !email || !apiToken) {
    System.err.println("FEHLER: Base-URL, E-Mail (Sandbox_USER) und API-Token (JIRA_API_TOKEN_CL) werden benötigt.")
    System.exit(1)
}
if (baseUrl.endsWith("/")) {
    baseUrl = baseUrl[0..-2]
}

String authHeader = "Basic " + Base64.getEncoder().encodeToString("${email}:${apiToken}".getBytes(StandardCharsets.UTF_8))

// -----------------------------------------------------------------
// Alle Felder EINMAL abrufen (/rest/api/3/field) statt pro ID einzeln
// -----------------------------------------------------------------

HttpClient client = HttpClient.newHttpClient()
HttpRequest fieldRequest = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/rest/api/3/field"))
        .header("Authorization", authHeader)
        .header("Accept", "application/json")
        .GET()
        .build()

HttpResponse<String> fieldResponse
try {
    fieldResponse = client.send(fieldRequest, HttpResponse.BodyHandlers.ofString())
} catch (Exception e) {
    System.err.println("FEHLER beim Zugriff auf JIRA: ${e.message}")
    System.exit(1)
    return
}

if (fieldResponse.statusCode() != 200) {
    System.err.println("FEHLER: JIRA API antwortete mit Status ${fieldResponse.statusCode()}")
    System.err.println(fieldResponse.body())
    System.exit(1)
}

def allFields = new JsonSlurper().parseText(fieldResponse.body())

// Map: "customfield-12345" -> Field-JSON-Objekt
Map<String, Object> fieldsById = [:]
allFields.each { f ->
    if (f.id?.startsWith("customfield_") || f.id?.startsWith("customfield-")) {
        String normalizedId = f.id.replace("customfield_", "customfield-")
        fieldsById[normalizedId] = f
    }
}

// -----------------------------------------------------------------
// Issue abrufen (/rest/api/3/issue/{key}) um die tatsächlichen Feldwerte zu holen
// -----------------------------------------------------------------

HttpRequest issueRequest = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/rest/api/3/issue/${issueKey}"))
        .header("Authorization", authHeader)
        .header("Accept", "application/json")
        .GET()
        .build()

HttpResponse<String> issueResponse
try {
    issueResponse = client.send(issueRequest, HttpResponse.BodyHandlers.ofString())
} catch (Exception e) {
    System.err.println("FEHLER beim Zugriff auf Issue ${issueKey}: ${e.message}")
    System.exit(1)
    return
}

if (issueResponse.statusCode() != 200) {
    System.err.println("FEHLER: JIRA API antwortete für Issue ${issueKey} mit Status ${issueResponse.statusCode()}")
    System.err.println(issueResponse.body())
    System.exit(1)
}

def issueJson = new JsonSlurper().parseText(issueResponse.body())
def issueFields = issueJson.fields ?: [:]

// Formatiert einen Feldwert lesbar (Objekte/Arrays -> kompaktes JSON statt Groovy toString)
def formatValue = { value ->
    if (value == null) return "(leer)"
    if (value instanceof String || value instanceof Number || value instanceof Boolean) return value.toString()
    return groovy.json.JsonOutput.toJson(value)
}

// -----------------------------------------------------------------
// 4. Ausgabedatei zusammenstellen: CF_<Name der Eingabedatei>.txt
// -----------------------------------------------------------------

String inputFileName = inputPath.getFileName().toString()
Path outputPath = inputPath.resolveSibling("CF_${inputFileName}.txt")

StringBuilder sb = new StringBuilder()
sb << "Custom-Field-Übersicht für: ${inputFileName}\n"
sb << "Issue:  ${issueKey}\n"
sb << "Quelle: ${baseUrl}\n"
sb << "Erzeugt am: ${new Date().format('yyyy-MM-dd HH:mm:ss')}\n"
sb << ("=" * 70) << "\n\n"

int notFoundCount = 0
fieldIds.each { id ->
    String key = "customfield-${id}"
    String apiKey = "customfield_${id}" // JIRA REST API nutzt "_" statt "-"
    def field = fieldsById[key]
    sb << "ID:     ${key}\n"
    if (field) {
        sb << "Name:   ${field.name}\n"
        sb << "Typ:    ${field.schema?.type ?: '(kein schema.type)'}\n"
        sb << "Custom: ${field.schema?.custom ?: '(kein schema.custom)'}\n"
        sb << "CustomId: ${field.schema?.customId ?: '-'}\n"
        sb << "Items:  ${field.schema?.items ?: '-'}\n"
        boolean hasValue = issueFields.containsKey(apiKey)
        sb << "Wert (${issueKey}): ${hasValue ? formatValue(issueFields[apiKey]) : '(Feld nicht im Issue-Response enthalten)'}\n"
    } else {
        sb << "Name:   *** NICHT GEFUNDEN in ${baseUrl} ***\n"
        notFoundCount++
    }
    sb << ("-" * 70) << "\n"
}

sb << "\nGesamt: ${fieldIds.size()} Felder referenziert, davon ${fieldIds.size() - notFoundCount} in JIRA gefunden, ${notFoundCount} nicht gefunden.\n"

Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8))

println "Ausgabedatei geschrieben: ${outputPath.toAbsolutePath()}"
if (notFoundCount > 0) {
    println "HINWEIS: ${notFoundCount} Feld(er) wurden in dieser JIRA-Instanz nicht gefunden."
}