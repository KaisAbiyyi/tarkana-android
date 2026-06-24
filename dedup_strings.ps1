$xmlPath = "app/src/main/res/values/strings.xml"
$xml = [xml](Get-Content $xmlPath)
$seen = @{}
$nodesToRemove = @()

foreach ($node in $xml.resources.string) {
    $name = $node.name
    if ($seen.ContainsKey($name)) {
        $nodesToRemove += $node
    } else {
        $seen[$name] = $true
    }
}

foreach ($node in $nodesToRemove) {
    $node.ParentNode.RemoveChild($node) | Out-Null
}

$xml.Save((Resolve-Path $xmlPath).Path)
