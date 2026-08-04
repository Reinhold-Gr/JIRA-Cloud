Was wir wissen:

ScriptRunner Cloud (HAPI) ist installiert und erreichbar

Projects.findAll() → 1 Projekt gefunden, aber Iterator liefert keine Elemente

logger ist in statischem Scope nicht verfügbar → println verwenden

@CompileDynamic muss auf einer Methode/Klasse sitzen, nicht auf Script-Ebene

Nächster Schritt:

Herausfinden unter welchem User ScriptRunner die API-Calls macht (der myself-Endpunkt)

Direkte REST-Calls als Workaround falls HAPI keinen Datenzugriff hat

---

# Basis Test
// BAsisTest SCriptRunner
def response = get("/rest/api/3/myself")
    .header("Accept", "application/json")
    .asObject(Map)

def user = response.body
return "User: ${user.displayName}, Email: ${user.emailAddress}, AccountId: ${user.accountId}"

## Ergbnis:

# Result:
"User: Reinhold Gritsch, Email: null, AccountId: 712020:aef3309b-997f-4a5f-a89e-d06ccd190154"

# Logs:
Serializing object into 'interface java.util.Map'
GET /rest/api/3/myself asObject Request Duration: 219ms

---

Ich glaube, ich muss genauer spezifizieren, was ich eigentlich benötige. Aktuell unter Data center erzeuge ich über ein Listener Script einen QR-Code als PNG-FIle. Das ich auf dem Server zwischenspeichere.
Wenn ich dann anschließend  die Rechnung im PDF erzeuge, hole ich den QR-Code und füge ihn in das PDF-File ein.

---

# Vorschläge QR-Code einfügen in PDF von ROVO

Danke für die Klarstellung! Jetzt verstehe ich den Workflow. Das Problem ist klar: In Jira Cloud gibt es kein Dateisystem, auf dem du Dateien zwischenspeichern kannst. Du brauchst also eine Alternative für den Zwischenspeicher des QR-Code-PNG.
Dein aktueller Workflow (Data Center)
Listener → QR-Code erzeugen → PNG auf Server speichern → PDF erzeugen → PNG vom Server lesen → in PDF einfügen
Cloud-Alternativen für den Zwischenspeicher
Option 1: QR-Code als Attachment am Jira-Issue ⭐ (Empfohlen)
Das ist der natürlichste Ersatz – der QR-Code wird direkt am Issue als Anhang gespeichert.
Schritt A – QR-Code erzeugen und als Attachment anhängen:
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream

def issueKey = "RECHNUNG-8208"
def qrContent = "Dein QR-Inhalt hier"

// QR-Code im Speicher erzeugen (kein Dateisystem nötig!)
def qrWriter = new QRCodeWriter()
def bitMatrix = qrWriter.encode(qrContent, BarcodeFormat.QR_CODE, 250, 250)
def image = MatrixToImageWriter.toBufferedImage(bitMatrix)

// PNG in ByteArray schreiben
def baos = new ByteArrayOutputStream()
ImageIO.write(image, "PNG", baos)
def pngBytes = baos.toByteArray()

// Als Attachment am Issue anhängen
def attachResponse = post("/rest/api/3/issue/${issueKey}/attachments")
    .header("Accept", "application/json")
    .header("X-Atlassian-Token", "no-check")
    .field("file", pngBytes, "application/octet-stream", "qrcode_${issueKey}.png")
    .asObject(List)

if (attachResponse.status == 200) {
    def attachmentId = attachResponse.body[0].id
    return "✅ QR-Code gespeichert als Attachment (ID: ${attachmentId})"
} else {
    return "❌ Fehler: HTTP ${attachResponse.status}"
}
Schritt B – QR-Code-Attachment abrufen (beim PDF-Erzeugen):
def issueKey = "RECHNUNG-8208"

// Alle Attachments des Issues abrufen
def response = get("/rest/api/3/issue/${issueKey}?fields=attachment")
    .header("Accept", "application/json")
    .asObject(Map)

def attachments = response.body.fields.attachment as List

// QR-Code Attachment finden
def qrAttachment = attachments.find { it.filename.startsWith("qrcode_") }

if (qrAttachment) {
    // PNG-Bytes herunterladen
    def imageUrl = qrAttachment.content  // Download-URL
    def conn = new URL(imageUrl).openConnection()
    conn.setRequestProperty("Authorization", "Basic ${authHeader}")
    def pngBytes = conn.inputStream.bytes

    // pngBytes kann jetzt in das PDF eingefügt werden
    return "✅ QR-Code geladen: ${qrAttachment.filename} (${pngBytes.length} Bytes)"
} else {
    return "⚠️ Kein QR-Code Attachment gefunden"
}
Option 2: QR-Code als Base64 in einer Issue Property speichern
Kein sichtbarer Anhang – der QR-Code wird unsichtbar als JSON Property am Issue gespeichert.
Speichern:
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream

// QR-Code erzeugen (wie oben) ...
def pngBytes = baos.toByteArray()
def base64Png = pngBytes.encodeBase64().toString()

// Als Issue Property speichern (unsichtbar für User)
def propBody = [qrcode_base64: base64Png]

put("/rest/api/3/issue/${issueKey}/properties/qrcode")
    .header("Content-Type", "application/json")
    .body(propBody)
    .asObject(Map)
Abrufen:
def propResponse = get("/rest/api/3/issue/${issueKey}/properties/qrcode")
    .header("Accept", "application/json")
    .asObject(Map)

def base64Png = propResponse.body.value.qrcode_base64
def pngBytes = base64Png.decodeBase64()

// pngBytes in PDF einfügen ...
⚠️ Issue Properties haben ein Limit von 32 KB. Für QR-Codes (typisch 1–5 KB) reicht das aus, für größere Bilder nicht.
Option 3: QR-Code nur im Speicher halten (kein Zwischenspeicher)
Falls Listener und PDF-Erzeugung im selben Skript laufen können, brauchst du gar keinen Zwischenspeicher:
// QR-Code erzeugen → direkt als byte[] im RAM
def pngBytes = erzeugQRCode(inhalt)

// Sofort ins PDF einfügen
def pdf = erstellePDF(auftragsdaten, pngBytes)
Vergleich der Optionen
Kriterium
Attachment
Issue Property
Im Speicher
Sichtbar für Benutzer
Ja (als Anhang)
Nein (unsichtbar)
Nein
Größenlimit
250 MB
32 KB
RAM
Zwischen Skripten nutzbar
✅ Ja
✅ Ja
❌ Nur im selben Skript
Aufräumen nötig
Optional
Nein
Nein
Komplexität
Gering
Gering
Am einfachsten
Empfehlung
Für deinen Workflow würde ich Option 1 (Attachment) empfehlen, weil:
Es dem bisherigen Ablauf am nächsten kommt (Datei erzeugen → speichern → später lesen)
Es zwischen verschiedenen Skripten / Zeitpunkten funktioniert
QR-Codes auch für Benutzer sichtbar und nachvollziehbar sind
Du alte QR-Codes per API wieder löschen kannst

┌─────────────────────────────────────────────────┐
│              Ein einziges Skript                │
│                                                 │
│  1. Jira Issue lesen  →  Auftragsdaten (RAM)   │
│           ↓                                     │
│  2. QR-Code erzeugen  →  byte[] PNG  (RAM)     │
│           ↓                                     │
│  3. PDF erzeugen      →  byte[] PDF  (RAM)     │
│     + QR-Code einbetten                         │
│           ↓                                     │
│  4. PDF als Attachment am Issue speichern       │
└─────────────────────────────────────────────────┘

{
  "ZXing": "❌ Nicht verfügbar",
  "OpenPDF (lowagie)": "❌ Nicht verfügbar",
  "iText 5": "❌ Nicht verfügbar",
  "PDFBox": "❌ Nicht verfügbar"
}


----


return [
    "Issue"       : issueKey,
    "Summary"     : summary,
    "Betrag"      : betrag,
    "QR-Bytes"    : qrPngBytes.length,
    "PNG gültig"  : qrPngBytes[0] == (byte)0x89,
    "PDF-Bibliothek verfügbar" : pdfLib
]
79 lines
 
No type checking errors detected
Run code as:
Current User

Run
Result
Logs
{
  "Issue": "RECHNUNG-8208",
  "Summary": "25-100-1730-01",
  "Betrag": "130.0",
  "QR-Bytes": 810,
  "PNG gültig": true,
  "PDF-Bibliothek verfügbar": "none"
}
