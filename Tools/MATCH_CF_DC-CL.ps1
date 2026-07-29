<#
.SYNOPSIS
    Gleicht Jira-Custom-Fields zwischen Data Center (Format-Table-Textexport)
    und Cloud (REST-API-JSON-Export) anhand des Feldnamens ab.

.PARAMETER DcPath
    Pfad zur DC-Textdatei (Format-Table-Output von "id name schema").

.PARAMETER CloudPath
    Pfad zur Cloud-JSON-Datei (Array von Field-Objekten aus /rest/api/2/field).

.PARAMETER OutPath
    Pfad zur Ausgabedatei (JSON) mit dem vollständigen Mapping.

.PARAMETER UnmatchedDcPath
    Pfad zur Ausgabedatei (JSON) mit DC-Feldern, die keinen Cloud-Treffer haben.

.PARAMETER UnmatchedClPath
    Pfad zur Ausgabedatei (JSON) mit Cloud-Feldern, die keinen DC-Treffer haben.

.EXAMPLE
    .\Match-CustomFields.ps1 `
        -DcPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\Customfileds_DC.txt" `
        -CloudPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\Customfilelds_CL.json" `
        -OutPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\CustomFieldMapping.json" `
        -UnmatchedDcPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\Unmatched_DC.json" `
        -UnmatchedClPath "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\Unmatched_CL.json"
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$DcPath,

    [Parameter(Mandatory = $true)]
    [string]$CloudPath,

    [Parameter(Mandatory = $false)]
    [string]$OutPath = "CustomFieldMapping.json",

    [Parameter(Mandatory = $false)]
    [string]$UnmatchedDcPath = "Unmatched_DC.json",

    [Parameter(Mandatory = $false)]
    [string]$UnmatchedClPath = "Unmatched_CL.json"
)

if (-not (Test-Path $DcPath)) {
    Write-Error "DC-Datei nicht gefunden: $DcPath"
    exit 1
}
if (-not (Test-Path $CloudPath)) {
    Write-Error "Cloud-Datei nicht gefunden: $CloudPath"
    exit 1
}

# ---------------------------------------------------------------------------
# 1. DC-Textdatei parsen
#    Format-Table-Output à la:
#    id                name                        schema
#    --                ----                        ------
#    customfield_13100 Bezahldatum                  @{type=date; custom=...}
#
#    Wir extrahieren id + name per Regex: id beginnt mit "customfield_",
#    danach folgt der Name bis zum Beginn der Schema-Spalte ("@{" oder Zeilenende).
# ---------------------------------------------------------------------------
$dcFields = @()

Get-Content -Path $DcPath | ForEach-Object {
    $line = $_

    # Header- und Trennzeilen überspringen
    if ($line -match '^\s*$') { return }
    if ($line -match '^id\s+name\s+schema') { return }
    if ($line -match '^--\s+----\s+------') { return }

    # customfield_<Zahl>  <Name...>  @{...}   ODER  customfield_<Zahl>  <Name...>  (ohne Schema-Rest)
    if ($line -match '^(customfield_\d+)\s+(.+?)\s{2,}(@\{.*)?$' -or
        $line -match '^(customfield_\d+)\s+(.+?)\s*$') {

        $id   = $Matches[1]
        $name = $Matches[2].TrimEnd()

        # Falls durch Regex-Fallback noch ein Schema-Rest im Namen hängt (Sicherheitsnetz)
        $name = ($name -replace '\s+@\{.*$', '').Trim()

        if ($id -and $name) {
            $dcFields += [PSCustomObject]@{
                id   = $id
                name = $name
            }
        }
    }
}

Write-Host "DC-Felder gelesen: $($dcFields.Count)"

# ---------------------------------------------------------------------------
# 2. Cloud-JSON parsen (nur custom fields, also id beginnt mit "customfield_")
# ---------------------------------------------------------------------------
$cloudRaw = Get-Content -Path $CloudPath -Raw | ConvertFrom-Json

$cloudFields = $cloudRaw | Where-Object { $_.custom -eq $true } | ForEach-Object {
    [PSCustomObject]@{
        id   = $_.id
        name = $_.name
    }
}

Write-Host "Cloud-Custom-Felder gelesen: $($cloudFields.Count)"

# Lookup-Tabelle: Name (getrimmt, case-insensitive) -> Cloud-ID
# Bei doppelten Namen in Cloud wird die erste Übereinstimmung genommen und gewarnt.
$cloudLookup = @{}
$cloudDuplicates = @{}

foreach ($cf in $cloudFields) {
    $key = $cf.name.Trim().ToLowerInvariant()
    if ($cloudLookup.ContainsKey($key)) {
        if (-not $cloudDuplicates.ContainsKey($key)) {
            $cloudDuplicates[$key] = @($cloudLookup[$key])
        }
        $cloudDuplicates[$key] += $cf.id
    } else {
        $cloudLookup[$key] = $cf.id
    }
}

if ($cloudDuplicates.Count -gt 0) {
    Write-Warning "Achtung: Mehrere Cloud-Felder mit demselben Namen gefunden (nur das erste wird gemappt):"
    foreach ($dupKey in $cloudDuplicates.Keys) {
        Write-Warning "  '$dupKey' -> $($cloudDuplicates[$dupKey] -join ', ')"
    }
}

# ---------------------------------------------------------------------------
# 3. Matching DC -> Cloud über den Feldnamen
# ---------------------------------------------------------------------------

# Lookup-Tabelle DC (Name -> DC-ID), analog zur Cloud-Lookup-Tabelle,
# wird für die Ermittlung der "nur in Cloud vorhandenen" Felder gebraucht.
$dcLookup = @{}
$dcDuplicates = @{}

foreach ($dc in $dcFields) {
    $key = $dc.name.Trim().ToLowerInvariant()
    if ($dcLookup.ContainsKey($key)) {
        if (-not $dcDuplicates.ContainsKey($key)) {
            $dcDuplicates[$key] = @($dcLookup[$key])
        }
        $dcDuplicates[$key] += $dc.id
    } else {
        $dcLookup[$key] = $dc.id
    }
}

if ($dcDuplicates.Count -gt 0) {
    Write-Warning "Achtung: Mehrere DC-Felder mit demselben Namen gefunden (nur das erste wird für den CL-Abgleich verwendet):"
    foreach ($dupKey in $dcDuplicates.Keys) {
        Write-Warning "  '$dupKey' -> $($dcDuplicates[$dupKey] -join ', ')"
    }
}

$mapping       = @()
$unmatchedDc   = @()   # DC-Felder ohne Cloud-Treffer: id + Name
$unmatchedCl   = @()   # Cloud-Felder ohne DC-Treffer: id + Name

foreach ($dc in $dcFields) {
    $key = $dc.name.Trim().ToLowerInvariant()
    $cloudId = $null

    if ($cloudLookup.ContainsKey($key)) {
        $cloudId = $cloudLookup[$key]
    } else {
        $unmatchedDc += [PSCustomObject]@{
            id_DC    = $dc.id
            Feldname = $dc.name
        }
    }

    $mapping += [PSCustomObject]@{
        id_DC    = $dc.id
        id_CL    = $cloudId
        Feldname = $dc.name
    }
}

foreach ($cf in $cloudFields) {
    $key = $cf.name.Trim().ToLowerInvariant()
    if (-not $dcLookup.ContainsKey($key)) {
        $unmatchedCl += [PSCustomObject]@{
            id_CL    = $cf.id
            Feldname = $cf.name
        }
    }
}

# ---------------------------------------------------------------------------
# 4. Ausgaben schreiben
# ---------------------------------------------------------------------------
$mapping     | ConvertTo-Json -Depth 3 | Set-Content -Path $OutPath -Encoding UTF8
$unmatchedDc | ConvertTo-Json -Depth 3 | Set-Content -Path $UnmatchedDcPath -Encoding UTF8
$unmatchedCl | ConvertTo-Json -Depth 3 | Set-Content -Path $UnmatchedClPath -Encoding UTF8

Write-Host ""
Write-Host "Mapping geschrieben nach:               $OutPath"
Write-Host "Treffer: $($mapping.Count - $unmatchedDc.Count) / $($mapping.Count)"
Write-Host ""
Write-Host "DC-Felder ohne Cloud-Treffer ($($unmatchedDc.Count)) geschrieben nach: $UnmatchedDcPath"
if ($unmatchedDc.Count -gt 0) {
    $unmatchedDc | ForEach-Object { Write-Host "  - $($_.id_DC)  $($_.Feldname)" }
}

Write-Host ""
Write-Host "Cloud-Felder ohne DC-Treffer ($($unmatchedCl.Count)) geschrieben nach: $UnmatchedClPath"
if ($unmatchedCl.Count -gt 0) {
    $unmatchedCl | ForEach-Object { Write-Host "  - $($_.id_CL)  $($_.Feldname)" }
}