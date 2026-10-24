$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$checks = [ordered]@{
    CubeMxExtra = 'FB920200BD0FC8975681354E26B59CA71560BE8E7F8EFE3988B9CD83767FC18D'
    CubeIdeDictionary = '0B7DF6B2182B2D5C398CEA6FE5FAE4755F97F59974BD3B7D0E191DD7AFC02FB8'
    SwtAgent = '6B8D7CBBCE2A307343453EF4585C0FA7EE8B82E111677F2DB9A78BDF7D0C378F'
    TrustedPackageCreatorDictionary = '631FD10EE54527BA638E1DD176F0070838350D777BA0AFEDE02D45E41569DF00'
}
$files = [ordered]@{
    CubeMxExtra = Join-Path $root 'src\cubemx\cubemx-extra-zh.tsv'
    CubeIdeDictionary = Join-Path $root 'src\cubeide\ide-extra-zh.tsv'
    SwtAgent = Join-Path $root 'dist\swt-tooltip-fix\swt-tooltip-fix.jar'
    TrustedPackageCreatorDictionary = Join-Path $root 'src\trusted-package-creator\zh-CN.json'
}
$result = foreach ($name in $files.Keys) {
    $actual = (Get-FileHash -LiteralPath $files[$name] -Algorithm SHA256).Hash
    [pscustomobject]@{ Name = $name; Path = $files[$name]; SHA256 = $actual; Passed = ($actual -eq $checks[$name]) }
}
if (@($result | Where-Object { -not $_.Passed }).Count) { $result | Format-Table -AutoSize; throw 'Package integrity check failed.' }
$jarCount = @(Get-ChildItem -LiteralPath (Join-Path $root 'dist\cubeide-supplement-repository\plugins') -Filter '*.jar').Count
if ($jarCount -ne 11) { throw "Expected 11 CubeIDE supplement fragments; found $jarCount." }
$result | Format-Table -AutoSize
Write-Output 'PASS: hashes and 11 CubeIDE supplement fragments verified.'

