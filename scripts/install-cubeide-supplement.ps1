param(
    [Parameter(Mandatory = $true)][string]$CubeIdeRoot,
    [switch]$Remove
)

$ErrorActionPreference = 'Stop'
$CubeIdeRoot = (Resolve-Path -LiteralPath $CubeIdeRoot).Path
$cli = Join-Path $CubeIdeRoot 'stm32cubeidec.exe'
if (-not (Test-Path -LiteralPath $cli)) { throw "STM32CubeIDE CLI not found: $cli" }
if (Get-Process -Name stm32cubeide -ErrorAction SilentlyContinue) { throw 'Close STM32CubeIDE first.' }

$repo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\dist\cubeide-supplement-repository')).Path
$unitsFile = Join-Path $repo 'units.txt'
if (-not (Test-Path -LiteralPath (Join-Path $repo 'content.jar'))) { throw 'Invalid p2 repository: content.jar is missing.' }
$ids = @(Get-Content -LiteralPath $unitsFile | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
if ($ids.Count -ne 11 -or @($ids | Where-Object { $_ -notlike 'local.xinda.zh.*' }).Count) { throw 'Unexpected install unit selection.' }

$common = @('-nosplash', '-application', 'org.eclipse.equinox.p2.director', '-destination', $CubeIdeRoot, '-profile', 'STM32CubeIDE')
if ($Remove) {
    & $cli @common -uninstallIU ($ids -join ',')
    if ($LASTEXITCODE -ne 0) { throw 'p2 uninstall failed.' }
    Write-Output 'CubeIDE Chinese supplement removed; Babel packages and ST modules retained.'
    return
}

$repoUri = ([uri]$repo).AbsoluteUri
& $cli @common -repository $repoUri -installIU ($ids -join ',') -tag 'STM32-Chinese-Supplement-v1.0.0'
if ($LASTEXITCODE -ne 0) { throw 'p2 install failed.' }

$bundles = Join-Path $CubeIdeRoot 'configuration\org.eclipse.equinox.simpleconfigurator\bundles.info'
$registered = @(Get-Content -LiteralPath $bundles | Where-Object { $_ -like 'local.xinda.zh.*' })
if ($registered.Count -ne $ids.Count) { throw "Expected $($ids.Count) registered fragments; found $($registered.Count)." }
[pscustomobject]@{ InstalledFragments = $registered.Count; Repository = $repo; Version = '1.0.0' } | ConvertTo-Json

