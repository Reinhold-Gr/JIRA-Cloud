<# 
    Replace-CustomFields.ps1
    Zweck:
        - Findet Strings coustomfield_nnnnn (nnnnn = 5 Ziffern)
        - Liest Mapping aus txt (Spalte 1 = Original, Spalte 2 = Ersatz)
        - Ersetzt gemäß Mapping
        - Wenn kein Ersatz vorhanden → *coustomfield_nnnnn*
        - Outputfile = Originalname + "CF" + gleiche Extension
#>
param(
    [Parameter(Mandatory=$true)]
    [string]$InputFile
)

# --- CONFIG ------------------------------------------------------------
$MappingFile = "C:\GitHub\Reinhold-Gr\JIRA-Cloud\Tools\CustomFieldMapping.txt"

# --- FUNCTIONS ---------------------------------------------------------

function Load-Mapping {
    param([string]$TxtPath)

    if (-not (Test-Path $TxtPath)) {
        throw "MappingFile '$TxtPath' nicht gefunden."
    }

    $map = @{}

    foreach ($line in Get-Content $TxtPath) {

        if ($line -match "^\s*$") { continue }

        # Zerlege Zeile anhand von TAB
        $parts = $line -split "`t"

        # Spalte 1 = Originalwert
        $key = $parts[0].Trim()

        # Spalte 2 = Ersatzwert
        $value = $parts[1].Trim()

        # Debug-Ausgabe
        # Write-Host "Mapping: KEY='$key' VALUE='$value'"

        $map[$key] = $value
    }

    return $map
}

function Resolve-Replacement {
    param(
        [string]$FoundValue,
        [hashtable]$MapTable
    )

    if (-not $MapTable.ContainsKey($FoundValue)) {
        return "*$FoundValue*"
    }

    $replacement = $MapTable[$FoundValue]

    if ([string]::IsNullOrWhiteSpace($replacement)) {
        return "*$FoundValue*"
    }

    return $replacement
}
function Process-Content {
    param(
        [string]$Content,
        [hashtable]$MapTable
    )

    # KORREKT: jetzt 5-stellig
    $pattern = "customfield_\d{5}"
    $regex   = [regex]$pattern

    $newContent = $regex.Replace($Content, {
        param($match)
        Resolve-Replacement -FoundValue $match.Value -MapTable $MapTable
    })

    return $newContent
}


function Write-OutputFile {
    param(
        [string]$InputFile,
        [string]$Content
    )

    $dir  = Split-Path $InputFile -Parent
    $name = Split-Path $InputFile -LeafBase
    $ext  = Split-Path $InputFile -Leaf | Split-Path -Extension   # ".vm" oder ".groovy"

    $outFile = Join-Path $dir "${name}_CF$ext"

    Set-Content -Path $outFile -Value $Content -Encoding UTF8

    return $outFile
}

# --- MAIN --------------------------------------------------------------

Write-Host "Lade Mapping-Tabelle..."
$map = Load-Mapping -TxtPath $MappingFile

Write-Host "Lese Inputfile..."
$content = Get-Content -Path $InputFile -Raw

Write-Host "Verarbeite Inhalt..."
$newContent = Process-Content -Content $content -MapTable $map

Write-Host "Schreibe Outputfile..."
$out = Write-OutputFile -InputFile $InputFile -Content $newContent

Write-Host "Fertig. Outputfile: $out"
