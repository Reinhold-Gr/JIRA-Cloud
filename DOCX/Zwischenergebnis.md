Was wir wissen:

ScriptRunner Cloud (HAPI) ist installiert und erreichbar

Projects.findAll() → 1 Projekt gefunden, aber Iterator liefert keine Elemente

logger ist in statischem Scope nicht verfügbar → println verwenden

@CompileDynamic muss auf einer Methode/Klasse sitzen, nicht auf Script-Ebene

Nächster Schritt:

Herausfinden unter welchem User ScriptRunner die API-Calls macht (der myself-Endpunkt)

Direkte REST-Calls als Workaround falls HAPI keinen Datenzugriff hat

---

# BAsis Test
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
