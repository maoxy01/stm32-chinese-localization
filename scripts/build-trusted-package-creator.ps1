param(
    [Parameter(Mandatory = $true)][string]$CubeProgrammerRoot,
    [Parameter(Mandatory = $true)][string]$Python
)

$ErrorActionPreference = 'Stop'
$CubeProgrammerRoot = (Resolve-Path -LiteralPath $CubeProgrammerRoot).Path
$source = Join-Path $CubeProgrammerRoot 'bin\STM32TrustedPackageCreator.exe'
$output = Join-Path $CubeProgrammerRoot 'bin\STM32TrustedPackageCreator_zh_CN.exe'
$builder = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\src\trusted-package-creator\build_zh.py')).Path
$dictionary = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\src\trusted-package-creator\zh-CN.json')).Path
if (-not (Test-Path -LiteralPath $source)) { throw "Vendor executable not found: $source" }
if (-not (Test-Path -LiteralPath $Python)) { throw "Python interpreter not found: $Python" }
if (Get-Process -Name STM32TrustedPackageCreator,STM32TrustedPackageCreator_zh_CN -ErrorAction SilentlyContinue) {
    throw 'Close STM32 Trusted Package Creator first.'
}
& $Python $builder --source $source --output $output --dictionary $dictionary
if ($LASTEXITCODE -ne 0) { throw 'Localized copy build failed.' }
$hash = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash
if ($hash -ne '87698B2FE57C7B2CFF49904907A19D79EC40E66133CC1C5F2C5E73C8B4070CC3') { throw 'Unexpected localized output hash.' }
[pscustomobject]@{ Original = $source; LocalizedCopy = $output; SHA256 = $hash; OriginalPreserved = $true } | ConvertTo-Json

