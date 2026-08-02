def issueKey = "AUFTRAG-26376"

def response = get("/rest/api/3/issue/${issueKey}")
    .header("Accept", "application/json")
    .asObject(Map)

if (response.status != 200) {
    return "Fehler: HTTP ${response.status}"
}

def fields = response.body.fields as Map

return [
    "Issue"            : response.body.key,
    "Summary"          : fields.summary,
    "customfield_10200": fields["customfield_10200"]?.toString() ?: "leer",
    "Betrag 1"         : fields["customfield_10255"]?.toString() ?: "leer"
]