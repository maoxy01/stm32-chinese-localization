param(
    [Parameter(Mandatory = $true)][string]$CubeIdeRoot,
    [string]$JavaHome = 'D:\JDK17'
)
$ErrorActionPreference = 'Stop'
$install = (Resolve-Path -LiteralPath $CubeIdeRoot).Path
$vendor = Join-Path $install 'plugins\org.eclipse.swt.win32.win32.x86_64_3.132.0.v20251124-0642.jar'
if ((Get-FileHash -LiteralPath $vendor -Algorithm SHA256).Hash -ne 'E7D7325C06B6EC5820D669E1F66515CCA88A17F9522EF1E8BD1DE65FF69EEF9B') {
    throw 'Vendor SWT changed; review the repair before rebuilding.'
}
$build = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Path $build -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($vendor)
try {
    $stream = $archive.GetEntry('org/eclipse/swt/widgets/Table.class').Open()
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $classHash = [Convert]::ToHexString($sha.ComputeHash($stream)).ToLowerInvariant() }
    finally { $stream.Dispose(); $sha.Dispose() }
} finally { $archive.Dispose() }
$source = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'TooltipBoundsAgent.java') -Raw).Replace('TABLE_SHA256', $classHash)
[IO.File]::WriteAllText((Join-Path $build 'TooltipBoundsAgent.java'), $source)
$asm = Join-Path $install 'plugins\org.objectweb.asm_9.9.0.jar'
$tree = Join-Path $install 'plugins\org.objectweb.asm.tree_9.9.0.jar'
$javac = Join-Path $JavaHome 'bin\javac.exe'
$jarTool = Join-Path $JavaHome 'bin\jar.exe'
if (-not (Test-Path -LiteralPath $javac) -or -not (Test-Path -LiteralPath $jarTool)) { throw 'JDK javac/jar tools not found.' }
& $javac -encoding UTF-8 -cp "$asm;$tree" -d $build (Join-Path $build 'TooltipBoundsAgent.java')
if ($LASTEXITCODE) { throw 'Agent compilation failed' }
foreach ($dependency in @($asm, $tree)) {
    $archive = [IO.Compression.ZipFile]::OpenRead($dependency)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.StartsWith('org/objectweb/asm/') -and $entry.FullName.EndsWith('.class')) {
                $target = Join-Path $build $entry.FullName
                New-Item -ItemType Directory -Path (Split-Path $target) -Force | Out-Null
                [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
            }
        }
    } finally { $archive.Dispose() }
}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'NOTICE.txt') -Destination (Join-Path $build 'NOTICE.txt') -Force
[IO.File]::WriteAllText((Join-Path $build 'MANIFEST.MF'), "Manifest-Version: 1.0`r`nPremain-Class: TooltipBoundsAgent`r`n`r`n")
& $jarTool cfm (Join-Path $PSScriptRoot 'swt-tooltip-fix.jar') (Join-Path $build 'MANIFEST.MF') -C $build 'TooltipBoundsAgent.class' -C $build 'TooltipBoundsAgent$1.class' -C $build org -C $build NOTICE.txt
if ($LASTEXITCODE) { throw 'Agent packaging failed' }
Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'swt-tooltip-fix.jar')
