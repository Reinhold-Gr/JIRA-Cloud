<#
.SYNOPSIS
    Wrapper fuer ExtractCFs.groovy: setzt die JIRA-Sandbox-Credentials
    als Umgebungsvariablen und ruft das Groovy-Script mit Datei- und Issue-Key auf.

.PARAMETER InputFile
    Pfad zur Datei, die nach customfield-nnnnn Referenzen durchsucht wird.

.PARAMETER IssueKey
    JIRA Issue-Key (z.B. PROJ-123), aus dem die tatsaechlichen Feldwerte gelesen werden.

.EXAMPLE
    .\Run-ExtractCustomFields.ps1 -InputFile "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Templates\rechnung-in-issue-fo.vm" -IssueKey "PROJ-123"
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$InputFile,

    [Parameter(Mandatory = $true)]
    [string]$IssueKey,

    [string]$GroovyScript = (Join-Path $PSScriptRoot "ExtractCFs.groovy")
)

# --- Voraussetzungen pruefen ---
if (-not (Test-Path $InputFile)) {
    Write-Error "Eingabedatei nicht gefunden: $InputFile"
    exit 1
}
if (-not (Test-Path $GroovyScript)) {
    Write-Error "Groovy-Script nicht gefunden: $GroovyScript"
    exit 1
}
if (-not (Get-Command groovy -ErrorAction SilentlyContinue)) {
    Write-Error "groovy.exe wurde im PATH nicht gefunden. Bitte Groovy installieren bzw. PATH pruefen."
    exit 1
}

# --- Credentials aus Umgebungsvariablen ---
if (-not $env:Sandbox_USER) {
    Write-Error "Umgebungsvariable Sandbox_USER ist nicht gesetzt."
    exit 1
}
if (-not $env:JIRA_API_TOKEN_CL) {
    Write-Error "Umgebungsvariable JIRA_API_TOKEN_CL ist nicht gesetzt."
    exit 1
}

# Base-URL optional ueberschreibbar, sonst nutzt das Groovy-Script seinen Sandbox-Default
if (-not $env:JIRA_BASE_URL) {
    $env:JIRA_BASE_URL = "https://aktivsenioren-sandbox.atlassian.net"
}

Write-Host "Starte ExtractCustomFields.groovy"
Write-Host "  Eingabedatei : $InputFile"
Write-Host "  Issue-Key    : $IssueKey"
Write-Host "  Base-URL     : $($env:JIRA_BASE_URL)"
Write-Host ""

# --- Aufruf ---
& groovy $GroovyScript $InputFile $IssueKey
$exitCode = $LASTEXITCODE

if ($exitCode -eq 0) {
    $outputFile = Join-Path (Split-Path $InputFile -Parent) ("CF_" + (Split-Path $InputFile -Leaf) + ".txt")
    Write-Host ""
    if (Test-Path $outputFile) {
        Write-Host "Fertig. Ausgabedatei: $outputFile" -ForegroundColor Green
    } else {
        Write-Host "Script erfolgreich beendet, aber keine Ausgabedatei erzeugt (vermutlich keine customfield-Referenzen gefunden)." -ForegroundColor Yellow
    }
} else {
    Write-Warning "Groovy-Script wurde mit Exit-Code $exitCode beendet."
}

exit $exitCode
