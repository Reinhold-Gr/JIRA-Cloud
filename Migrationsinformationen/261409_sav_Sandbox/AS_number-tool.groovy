import java.text.NumberFormat
import java.util.Locale

// 1. Deutsches Format mit Tausendertrennzeichen (z. B. 1.200,00 €)
def DE = Locale.GERMANY
def fmt = NumberFormat.getNumberInstance(DE)
fmt.setMinimumFractionDigits(2)
fmt.setMaximumFractionDigits(2)
fmt.setGroupingUsed(true) // Aktiviert Tausendertrennpunkt (1.200,00 €)

// 2. Aktuelles Ticket holen
def currentIssue = (binding.hasVariable("issues") && issues) ? issues.get(0) : (binding.hasVariable("issue") ? issue : null)

// 3. Hilfsfunktion zum sicheren Auslesen & Parsen deutscher Zahlen
def getBigDecimalValue = { fieldId ->
    if (!currentIssue) return 0G
    def val = currentIssue.getCustomFieldValue(fieldId)
    if (val == null) return 0G

    // Falls Select-Feld (Jira Cloud Map)
    if (val instanceof Map) {
        val = val.value != null ? val.value : val
    }
    try {
        if (val.metaClass.hasProperty(val, "value")) {
            val = val.value
        }
    } catch (Exception e) {}

    if (val == null) return 0G

    // Falls bereits eine Java Number (Double, Long etc.)
    if (val instanceof Number) {
        return new BigDecimal(val.toString())
    }

    def str = val.toString().trim()

    // Tausender-Punkte entfernen, falls Komma vorhanden (z. B. "1.200,00" -> "1200,00")
    if (str.contains(",") && str.contains(".")) {
        str = str.replace(".", "")
    }

    // Komma in Punkt umwandeln für BigDecimal
    str = str.replace(",", ".").replaceAll("[^0-9.-]", "")

    return str.isNumber() ? str.toBigDecimal() : 0G
}

// 4. Werte auslesen (customfield_10255 = Netto, customfield_10249 = MwSt)
def nettoVal    = getBigDecimalValue("customfield_10255")
def mwstSatzVal = getBigDecimalValue("customfield_10249")

// 5. Berechnen
def mwstBetragVal = (nettoVal * mwstSatzVal / 100G).setScale(2, BigDecimal.ROUND_HALF_UP)
def gesamtVal     = (nettoVal + mwstBetragVal).setScale(2, BigDecimal.ROUND_HALF_UP)

// 6. Fertig formatierte Strings für Velocity bereitstellen
nettoFmt      = fmt.format(nettoVal) + " €"
mwstSatzFmt   = fmt.format(mwstSatzVal) + " %"
mwstBetragFmt = fmt.format(mwstBetragVal) + " €"
gesamtFmt     = fmt.format(gesamtVal) + " €"