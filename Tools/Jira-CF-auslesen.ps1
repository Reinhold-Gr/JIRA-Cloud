# =============================================================
# Jira Cloud – Issue Custom Fields auslesen
# Authentifizierung via Umgebungsvariablen:
#   $env:JIRA_USER  = "deine-email@example.com"
#   $env:JIRA_TOKEN = "dein-api-token"
#
# Die auszulesenden Felder kommen aus einer Mapping-Datei (JSON)
# mit Eintraegen der Form:
#   { "id_DC": "...", "id_CL": "customfield_10380", "Feldname": "Bezahldatum" }
# Eintraege mit id_CL == null werden ignoriert.
# =============================================================

param(
    [string]$IssueKey     = "AUFTRAG-26376",
    [string]$BaseUrl      = "https://aktivsenioren-sandbox.atlassian.net",
    [string]$OutputFile   = ".\jira_output.json",
    [string]$MappingFile  = "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\CustomFieldMapping.json"
)

# --- Mapping-Datei laden ---
if (-not (Test-Path $MappingFile)) {
    Write-Error "Mapping-Datei nicht gefunden: $MappingFile"
    exit 1
}

$Mapping = Get-Content -Path $MappingFile -Raw -Encoding UTF8 | ConvertFrom-Json
$FieldMap = $Mapping | Where-Object { $null -ne $_.id_CL -and $_.id_CL -ne "" }

if ($FieldMap.Count -eq 0) {
    Write-Error "Keine gueltigen Feld-Mappings (id_CL nicht null) in $MappingFile gefunden."
    exit 1
}

# --- Credentials aus Umgebungsvariablen ---
$JiraUser  = $env:Sandbox_USER
$JiraToken = $env:JIRA_API_TOKEN_CL

if (-not $JiraUser -or -not $JiraToken) {
    Write-Error "Bitte Umgebungsvariablen setzen: `$env:JIRA_USER und `$env:JIRA_TOKEN"
    exit 1
}

# --- Basic Auth Header erstellen ---
$Credentials = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes("${JiraUser}:${JiraToken}")
)

$Headers = @{
    "Authorization" = "Basic $Credentials"
    "Accept"        = "application/json"
}

# --- REST-Call ---
$Url = "$BaseUrl/rest/api/3/issue/$IssueKey"

try {
    $Response = Invoke-RestMethod -Uri $Url -Headers $Headers -Method Get
} catch {
    Write-Error "Fehler beim Abrufen von ${IssueKey}: $($_.Exception.Message)"
    exit 1
}

# --- Felder auslesen ---
$Fields = $Response.fields

$Result = [ordered]@{
    "Issue"   = $Response.key
    "Summary" = $Fields.summary
}

foreach ($entry in $FieldMap) {
    $fieldId = $entry.id_CL
    $value   = $Fields.$fieldId
    $Result[$entry.Feldname] = if ($value) { $value.ToString() } else { "leer" }
}

# --- Ausgabe in JSON-Datei ---
$Result | ConvertTo-Json -Depth 5 | Out-File -FilePath $OutputFile -Encoding utf8

Write-Host "✅ Ergebnis gespeichert in: $OutputFile"
Write-Host ($Result | ConvertTo-Json -Depth 5)

# =============================================================
# Aufteilung in "Werte" (echter Inhalt) und "leer" (leer/"leer")
# =============================================================

$ValuesResult = [ordered]@{}
$EmptyResult  = [ordered]@{}

foreach ($key in $Result.Keys) {
    $value = $Result[$key]
    if ($value -eq "" -or $value -eq "leer" -or $null -eq $value) {
        $EmptyResult[$key] = $value
    } else {
        $ValuesResult[$key] = $value
    }
}

# --- Zielverzeichnis aus $OutputFile ableiten ---
$OutputDir = Split-Path -Path $OutputFile -Parent
if ([string]::IsNullOrEmpty($OutputDir)) { $OutputDir = "." }

$WerteFile = Join-Path $OutputDir "${IssueKey}_CF_Werte.json"
$LeerFile  = Join-Path $OutputDir "${IssueKey}_CF_leer.json"

$ValuesResult | ConvertTo-Json -Depth 5 | Out-File -FilePath $WerteFile -Encoding utf8
$EmptyResult  | ConvertTo-Json -Depth 5 | Out-File -FilePath $LeerFile  -Encoding utf8

Write-Host "✅ Felder mit Werten ($($ValuesResult.Count)) gespeichert in: $WerteFile"
Write-Host "✅ Leere Felder ($($EmptyResult.Count)) gespeichert in: $LeerFile"