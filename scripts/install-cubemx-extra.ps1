param(
    [Parameter(Mandatory = $true)][string]$CubeMxRoot,
    [switch]$Remove
)

$ErrorActionPreference = 'Stop'
$CubeMxRoot = (Resolve-Path -LiteralPath $CubeMxRoot).Path
$target = Join-Path $CubeMxRoot 'localization\zh-CN\translations.tsv'
$receipt = Join-Path $CubeMxRoot 'localization\zh-CN\stm32-localization-pack-receipt.json'
$extra = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\src\cubemx\cubemx-extra-zh.tsv')).Path

$running = Get-Process -Name javaw -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "$CubeMxRoot*" }
if ($running) { throw 'Close STM32CubeMX first.' }
if (-not (Test-Path -LiteralPath $target)) { throw 'Base Chinese dictionary not found. Install the upstream CubeMX Chinese patcher first.' }

if ($Remove) {
    if (-not (Test-Path -LiteralPath $receipt)) { throw 'Installation receipt not found; refusing an untracked rollback.' }
    $record = Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
    $currentHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
    if ($currentHash -ne $record.InstalledSHA256) { throw 'Dictionary changed after installation; inspect it before reverting.' }
    if (-not (Test-Path -LiteralPath $record.Backup)) { throw 'Recorded backup is missing.' }
    Copy-Item -LiteralPath $record.Backup -Destination $target -Force
    Remove-Item -LiteralPath $receipt
    Write-Output "CubeMX dictionary restored from $($record.Backup)"
    return
}

if (Test-Path -LiteralPath $receipt) { throw 'An installation receipt already exists; remove or inspect the current supplement first.' }
$backup = $target + '.before-stm32-localization-pack-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
Copy-Item -LiteralPath $target -Destination $backup

$rows = [Collections.Specialized.OrderedDictionary]::new([StringComparer]::Ordinal)
foreach ($line in Get-Content -LiteralPath $target) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) { continue }
    $parts = $line -split "`t", 2
    if ($parts.Count -ne 2) { throw "Invalid base dictionary row: $line" }
    $rows[$parts[0]] = $parts[1]
}
foreach ($line in Get-Content -LiteralPath $extra) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) { continue }
    $parts = $line -split "`t", 2
    if ($parts.Count -ne 2) { throw "Invalid supplement row: $line" }
    $rows[$parts[0]] = $parts[1]
}

$output = @($rows.GetEnumerator() | ForEach-Object { $_.Key + "`t" + $_.Value })
[IO.File]::WriteAllText($target, ($output -join "`r`n") + "`r`n", [Text.UTF8Encoding]::new($false))
$installedHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
$record = [pscustomobject]@{
    Backup = $backup
    BackupSHA256 = (Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash
    InstalledSHA256 = $installedHash
    SupplementSHA256 = (Get-FileHash -LiteralPath $extra -Algorithm SHA256).Hash
    InstalledAt = (Get-Date).ToString('o')
}
$record | ConvertTo-Json | Set-Content -LiteralPath $receipt -Encoding utf8
$record | ConvertTo-Json

