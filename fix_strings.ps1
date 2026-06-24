$stringsXmlPath = "app\src\main\res\values\strings.xml"
$stringsXmlContent = Get-Content $stringsXmlPath -Raw

$layoutFiles = Get-ChildItem -Path "app\src\main\res\layout" -Filter "*.xml"
$missingStrings = @()

foreach ($file in $layoutFiles) {
    $content = Get-Content $file.FullName -Raw
    $matches = [regex]::Matches($content, "@string/([a-zA-Z0-9_]+)")
    foreach ($match in $matches) {
        $stringName = $match.Groups[1].Value
        $regexPattern = "name=`"$stringName`""
        if ($stringsXmlContent -notmatch $regexPattern) {
            if ($missingStrings -notcontains $stringName) {
                $missingStrings += $stringName
            }
        }
    }
}

if ($missingStrings.Count -gt 0) {
    $newContent = $stringsXmlContent.Replace("</resources>", "")
    foreach ($missing in $missingStrings) {
        $newContent += "    <string name=`"$missing`">$missing</string>`r`n"
    }
    $newContent += "</resources>`r`n"
    Set-Content -Path $stringsXmlPath -Value $newContent
    Write-Host "Added $($missingStrings.Count) missing strings."
} else {
    Write-Host "No missing strings found."
}
