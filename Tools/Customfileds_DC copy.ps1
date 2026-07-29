$token    = $env:JIRA_API_TOKEN
$username = $env:JIRA_USER
$baseUrl  = "https://vampir.aktivsenioren-bayern.de/jira"

if (-not $token -or -not $username) {
    Write-Error "JIRA_API_TOKEN oder JIRA_USER ist nicht gesetzt."
    exit 1
}

$authHeader = "Basic " + [Convert]::ToBase64String(
    [Text.Encoding]::ASCII.GetBytes("${username}:${token}")
)

# KORREKT: baseUrl existiert jetzt
$jiraUrl = "$baseUrl/rest/api/2/field"

$response = Invoke-RestMethod -Uri $jiraUrl -Headers @{ Authorization = $authHeader }

$response | Where-Object { $_.id -like "customfield_*" } |
    Select-Object id, name, schema
