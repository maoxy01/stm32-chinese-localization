param(
    [Parameter(Mandatory = $true)][string]$CubeIdeRoot,
    [switch]$Remove
)

$ErrorActionPreference = 'Stop'
$CubeIdeRoot = (Resolve-Path -LiteralPath $CubeIdeRoot).Path
if (Get-Process -Name stm32cubeide -ErrorAction SilentlyContinue) { throw 'Close STM32CubeIDE before changing its startup configuration.' }

$ini = Join-Path $CubeIdeRoot 'stm32cubeide.ini'
$destination = Join-Path $CubeIdeRoot 'localization\swt-tooltip-fix'
$agent = Join-Path $destination 'swt-tooltip-fix.jar'
$log = Join-Path $destination 'runtime.log'
$argument = '-javaagent:' + $agent + '=' + $log
if (-not (Test-Path -LiteralPath $ini)) { throw "Missing startup configuration: $ini" }
$lines = [IO.File]::ReadAllLines($ini)
$matching = @($lines | Where-Object { $_ -like '-javaagent:*swt-tooltip-fix.jar*' })

if ($Remove) {
    if ($matching.Count) {
        [IO.File]::WriteAllText($ini, (($lines | Where-Object { $_ -notlike '-javaagent:*swt-tooltip-fix.jar*' }) -join "`r`n") + "`r`n", [Text.UTF8Encoding]::new($false))
    }
    Write-Output 'Tooltip repair startup argument removed. Original SWT and language settings retained.'
    return
}

$source = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\dist\swt-tooltip-fix\swt-tooltip-fix.jar')).Path
$notice = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\dist\swt-tooltip-fix\NOTICE.txt')).Path
$vendor = Join-Path $CubeIdeRoot 'plugins\org.eclipse.swt.win32.win32.x86_64_3.132.0.v20251124-0642.jar'
$expectedVendor = 'E7D7325C06B6EC5820D669E1F66515CCA88A17F9522EF1E8BD1DE65FF69EEF9B'
if (-not (Test-Path -LiteralPath $vendor) -or (Get-FileHash -LiteralPath $vendor -Algorithm SHA256).Hash -ne $expectedVendor) {
    throw 'Unsupported SWT version or hash. Repair not installed.'
}
if ($matching.Count -gt 1 -or ($matching.Count -eq 1 -and $matching[0] -ne $argument)) { throw 'Conflicting tooltip repair configuration.' }
if (-not ($lines -contains '-vmargs')) { throw 'Missing JVM argument section.' }

New-Item -ItemType Directory -Path $destination -Force | Out-Null
$backup = Join-Path $destination ('stm32cubeide.ini.before-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
Copy-Item -LiteralPath $ini -Destination $backup
Copy-Item -LiteralPath $source -Destination $agent -Force
Copy-Item -LiteralPath $notice -Destination $destination -Force
if (-not $matching.Count) {
    [IO.File]::WriteAllText($ini, ($lines -join "`r`n") + "`r`n" + $argument + "`r`n", [Text.UTF8Encoding]::new($false))
}
if ((Get-FileHash -LiteralPath $agent -Algorithm SHA256).Hash -ne '6B8D7CBBCE2A307343453EF4585C0FA7EE8B82E111677F2DB9A78BDF7D0C378F') {
    throw 'Installed agent differs from the tested artifact.'
}
[pscustomobject]@{ Agent = $agent; SHA256 = (Get-FileHash -LiteralPath $agent).Hash; Backup = $backup; Log = $log } | ConvertTo-Json

