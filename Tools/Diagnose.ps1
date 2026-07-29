# === DIAGNOSE-SKRIPT ===

$Token = $env:JIRA_API_TOKEN_CL
$Email = "reinhold.gritsch@aktivsenioren.de"
$BaseUrl = "https://aktivsenioren-sandbox.atlassian.net"

# Schritt 1: Werte prüfen
Write-Host "=== Eingabe-Prüfung ===" -ForegroundColor Cyan
Write-Host "Email:        '$Email'"
Write-Host "Token-Länge:  $($Token.Length) Zeichen"
Write-Host "Token-Start:  $($Token.Substring(0, [Math]::Min(5, $Token.Length)))..."

# Schritt 2: Leerzeichen entfernen
$Email = $Email.Trim()
$Token = $Token.Trim()
Write-Host "Nach Trim - Token-Länge: $($Token.Length) Zeichen"

# Schritt 3: Base64 kodieren
$RawAuth = "${Email}:${Token}"
$EncodedAuth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($RawAuth))
Write-Host "`n=== Auth-Header ===" -ForegroundColor Cyan
Write-Host "Raw:     '$RawAuth'"
Write-Host "Encoded: $EncodedAuth"

# Schritt 4: API-Aufruf
Write-Host "`n=== API-Aufruf ===" -ForegroundColor Cyan
$Headers = @{
    "Authorization" = "Basic $EncodedAuth"
    "Accept"        = "application/json"
    "Content-Type"  = "application/json"
}

try {
    $Response = Invoke-RestMethod `
        -Uri "$BaseUrl/rest/api/3/myself" `
        -Headers $Headers `
        -Method Get
    Write-Host "ERFOLG!" -ForegroundColor Green
    $Response | ConvertTo-Json -Depth 5
}
catch {
    Write-Host "FEHLER: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
}