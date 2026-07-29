# JSON laden
$data = Get-Content "Tools/CustomFieldMapping.json" | ConvertFrom-Json

# Excel starten
$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$workbook = $excel.Workbooks.Add()
$sheet = $workbook.Worksheets.Item(1)

# Header schreiben
$headers = $data[0].psobject.Properties.Name
for ($i = 0; $i -lt $headers.Count; $i++) {
    $sheet.Cells.Item(1, $i + 1).Value2 = $headers[$i]
}

# Daten schreiben
$row = 2
foreach ($item in $data) {
    $col = 1
    foreach ($prop in $item.psobject.Properties) {
        $sheet.Cells.Item($row, $col).Value2 = $prop.Value
        $col++
    }
    $row++
}

# Speichern
$workbook.SaveAs("CustomFieldMapping.xlsx")
$excel.Quit()
