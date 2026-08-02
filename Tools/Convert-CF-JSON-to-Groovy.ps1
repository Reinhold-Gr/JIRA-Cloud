# Konvertiert eine JSON-Datei mit Custom-Field-Mappings (id_DC, id_CL, Feldname)
# in eine TXT-Datei mit Groovy-Feldzuweisungen im Format:
#     "Feldname"         : fields["id_CL"]?.toString() ?: "leer"
# Eintraege mit id_CL == null werden uebersprungen.

param(
    [Parameter(Mandatory = $false)]
    [string]$InputJson = "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\CustomFieldMapping.json",

    [Parameter(Mandatory = $false)]
    [string]$OutputTxt = "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\CF_Groovy.txt"
)

if (-not (Test-Path $InputJson)) {
    Write-Error "Eingabedatei nicht gefunden: $InputJson"
    exit 1
}

$json = Get-Content -Path $InputJson -Raw -Encoding UTF8 | ConvertFrom-Json

$valid = $json | Where-Object { $null -ne $_.id_CL -and $_.id_CL -ne "" }

if ($valid.Count -eq 0) {
    Write-Warning "Keine gueltigen Eintraege (id_CL nicht null) gefunden."
}

$maxLen = ($valid | ForEach-Object { ('"' + $_.Feldname + '"').Length } | Measure-Object -Maximum).Maximum
if (-not $maxLen) { $maxLen = 0 }

$colonColumn = $maxLen + 1

$lines = foreach ($entry in $valid) {
    $quotedName = '"' + $entry.Feldname + '"'
    $padding    = ' ' * ($colonColumn - $quotedName.Length)
    "    $quotedName${padding}: fields[`"$($entry.id_CL)`"]?.toString() ?: `"leer`""
}

$lines | Set-Content -Path $OutputTxt -Encoding UTF8

Write-Host "Fertig: $($valid.Count) Zeilen geschrieben nach '$OutputTxt'."
if ($json.Count -ne $valid.Count) {
    $skipped = $json.Count - $valid.Count
    Write-Host "$skipped Eintrag(e) uebersprungen (id_CL = null)."
}