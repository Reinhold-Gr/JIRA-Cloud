InputJson<#
.SYNOPSIS
    Konvertiert eine JSON-Datei mit Custom-Field-Mappings (id_DC, id_CL, Feldname)
    in eine TXT-Datei mit Groovy-Feldzuweisungen im Format:

        "Feldname"         : fields["id_CL"]?.toString() ?: "leer"

    Einträge mit id_CL == null werden übersprungen.

.PARAMETER InputJson
    Pfad zur JSON-Quelldatei.

.PARAMETER OutputTxt
    Pfad zur zu erzeugenden TXT-Datei.

.EXAMPLE
    .\Convert-CF-JSON-to-Groovy.ps1 -InputJson .\fields.json -OutputTxt .\fields.txt
#>

param(
    [Parameter(Mandatory = $false)]
    [string]$InputJson = "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\CustomFieldMapping.json",

    [Parameter(Mandatory = $false)]
    [string]$OutputTxt = "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\CF_Groovy.txt"
)

# --- JSON einlesen ---
if (-not (Test-Path $InputJson)) {
    Write-Error "Eingabedatei nicht gefunden: $InputJson"
    exit 1
}

$json = Get-Content -Path $InputJson -Raw -Encoding UTF8 | ConvertFrom-Json

# --- Nur Einträge mit gesetztem id_CL behalten ---
$valid = $json | Where-Object { $null -ne $_.id_CL -and $_.id_CL -ne "" }

if ($valid.Count -eq 0) {
    Write-Warning "Keine gueltigen Eintraege (id_CL nicht null) gefunden."
}

# --- Ausrichtung berechnen: laengster "Feldname" (inkl. Anfuehrungszeichen) bestimmt Spaltenbreite ---
$maxLen = ($valid | ForEach-Object { ('"' + $_.Feldname + '"').Length } | Measure-Object -Maximum).Maximum
if (-not $maxLen) { $maxLen = 0 }

# Mindestens ein Leerzeichen Abstand vor dem Doppelpunkt
$colonColumn = $maxLen + 1

# --- Zeilen erzeugen ---
$lines = foreach ($entry in $valid) {
    $quotedName = '"' + $entry.Feldname + '"'
    $padding    = ' ' * ($colonColumn - $quotedName.Length)
    "    $quotedName${padding}: fields[`"$($entry.id_CL)`"]?.toString() ?: `"leer`""
}

# --- Ausgabe schreiben ---
$lines | Set-Content -Path $OutputTxt -Encoding UTF8

Write-Host "Fertig: $($valid.Count) Zeilen geschrieben nach '$OutputTxt'."
if ($json.Count -ne $valid.Count) {
    $skipped = $json.Count - $valid.Count
    Write-Host "$skipped Eintrag(e) uebersprungen (id_CL = null)."
}