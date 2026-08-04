// ============================================================
// SCHRITT 1: Issue-Daten lesen
// ============================================================
def issueKey = "RECHNUNG-8208"

def response = get("/rest/api/3/issue/${issueKey}")
    .header("Accept", "application/json")
    .asObject(Map)

if (response.status != 200) {
    return "❌ Fehler beim Lesen: HTTP ${response.status}"
}

def fields         = response.body.fields as Map
def empfaenger     = "Aktivsenioren Bayern e.V."
def iban           = "DE89370400440532013000"    // ← anpassen
def bic            = "GENODEF1S04"              // ← anpassen
def betrag         = fields["customfield_10255"]?.toString() ?: "0.00"
def rechnungsNr    = fields["customfield_10377"]?.toString() ?: issueKey
def summary        = fields.summary?.toString() ?: ""

// ============================================================
// SCHRITT 2: GiroCode-Inhalt aufbauen
// ============================================================
def epcContent = [
    "BCD",
    "002",
    "1",
    "SCT",
    bic,
    empfaenger,
    iban,
    "EUR${betrag}",
    "",
    "",
    "Rechnung ${rechnungsNr}",
    ""
].join("\n")

// ============================================================
// SCHRITT 3: QR-Code als PNG-Bytes holen (im RAM)
// ============================================================
def encodedContent = URLEncoder.encode(epcContent, "UTF-8")
def qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=M&data=${encodedContent}"

def qrConn = new URL(qrApiUrl).openConnection() as HttpURLConnection
qrConn.setConnectTimeout(5000)
qrConn.setReadTimeout(5000)

if (qrConn.responseCode != 200) {
    return "❌ QR-Code API Fehler: HTTP ${qrConn.responseCode}"
}

def qrPngBytes = qrConn.inputStream.bytes  // ← QR-Code im RAM

// ============================================================
// SCHRITT 4: Bibliothekstest für PDF
// ============================================================
def pdfLib = ""
try {
    Class.forName("com.lowagie.text.Document")
    pdfLib = "lowagie"
} catch (ClassNotFoundException e1) {
    try {
        Class.forName("com.itextpdf.text.Document")
        pdfLib = "itext5"
    } catch (ClassNotFoundException e2) {
        pdfLib = "none"
    }
}

return [
    "Issue"       : issueKey,
    "Summary"     : summary,
    "Betrag"      : betrag,
    "QR-Bytes"    : qrPngBytes.length,
    "PNG gültig"  : qrPngBytes[0] == (byte)0x89,
    "PDF-Bibliothek verfügbar" : pdfLib
]
g
---

{
  "Apache PDFBox": "❌",
  "iText 7": "❌",
  "OpenHTMLtoPDF": "❌",
  "JasperReports": "❌",
  "Apache FOP": "❌"
}

---