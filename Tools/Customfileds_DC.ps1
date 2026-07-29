$token   = $env:JIRA_API_TOKEN
$baseUrl = "https://vampir.aktivsenioren-bayern.de/jira"

if (-not $token) {
    Write-Error "JIRA_API_TOKEN ist nicht gesetzt."
    exit 1
}

$jiraUrl = "$baseUrl/rest/api/2/field"

$response = Invoke-RestMethod -Uri $jiraUrl -Headers @{ Authorization = "Bearer $token" }

$response | Where-Object { $_.id -like "customfield_*" } |
    Select-Object id, name, schema
