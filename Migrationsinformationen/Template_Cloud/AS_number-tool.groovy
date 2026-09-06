// Allgemeines Number-Tool laden
numberTool = new NumberTool()

class NumberTool {

    BigDecimal toNumber(issue, String customFieldId) {

        def value = issue.getCustomFieldValue(customFieldId)

        if (value == null) {
            return null
        }

        // Jira Select-Feld
        if (value.hasProperty("value")) {
            value = value.value
        }

        return new BigDecimal(
            value.toString().trim().replace(",", ".")
        )
    }
}