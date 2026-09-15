Hier ist eine **präzise, gut verwertbare Zusammenfassung** der wichtigsten Punkte aus Ferenc’ Antwort, 
was für die Migration wirklich gebraucht:

---

## **🔹 1. Feature Parity (DC → Cloud)**
**QR‑Codes:**  
- **Nicht** als native Funktion in Better PDF Exporter für Jira Cloud verfügbar.  
- Workaround: QR‑Code extern generieren und per REST‑Call einbinden.

**PDF‑Export:**  
- Die Kernfunktionen kommen weiterhin von **Better PDF Exporter**.  
- **Better PDF Automation** ist nur die Automations-Schnittstelle, kein Funktionsmodul.

**Wichtig:**  
- Wenn QR‑Code‑Support für euch wichtig ist → Feature Request voten, kommentieren und folgen.

---

## **🔹 2. Template-Migration (Velocity/Groovy → Cloud)**
- Cloud nutzt **keine Velocity-Templates** und **keine Groovy-Skripte**.  
- Migration erfordert **Neuerstellung der Templates** mit den Cloud‑Mechanismen.  
- Midori stellt eine **Cloud Migration Guide** bereit (Link im Originaltext).  
- Bei Detailfragen kannst du direkt nachhaken.

---

## **🔹 3. Cloud-spezifische Einschränkungen**
Der beschriebene Prozess (Rechnungserstellung, PDF, QR‑Code, Automationen) ist **grundsätzlich auch in der Cloud möglich**.

**Aber:**  
- Cloud-Version hat ein **Quota-System**:
  - Max. **200 Exports** oder **60 Minuten Ausführungszeit** pro 60‑Minuten‑Fenster  
  - Ein einzelner Export darf max. **10 Minuten** dauern  
- Bei sehr komplexen oder massenhaften PDF-Erstellungen kann das relevant werden.

---

## **🔹 Kurzfazit**
- Dein aktueller Prozess ist **prinzipiell Cloud‑fähig**.  
- **QR‑Codes** musst du extern erzeugen.  
- **Templates müssen neu gebaut** werden, da Velocity/Groovy nicht unterstützt wird.  
- **Quota‑Limits** im Blick behalten, falls ihr viele PDFs generiert.

---

## Sonderzeichen direkt im Template

## =====================================================================
## Sonderzeichen-Makro direkt im Template
## =====================================================================
#macro(decodeSonderzeichen $issue $customfield)
#set($wert = $issue.getCustomFieldValue($customfield))
#if($wert)
  #set($wert = $wert.toString())
  ## Kleinbuchstaben
  #set($wert = $wert.replace("ä", "ä"))
  #set($wert = $wert.replace("ö", "ö"))
  #set($wert = $wert.replace("ü", "ü"))
  #set($wert = $wert.replace("ß", "ß"))
  ## Großbuchstaben
  #set($wert = $wert.replace("Ä", "Ä"))
  #set($wert = $wert.replace("Ö", "Ö"))
  #set($wert = $wert.replace("Ü", "Ü"))
  #set($wert = $wert.replace("ẞ", "ẞ"))
  $wert
#end
#end

