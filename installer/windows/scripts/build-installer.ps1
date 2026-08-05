[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$installerRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $installerRoot '..\..')).Path
$manifestSource = Join-Path $installerRoot 'manifest\installer-manifest.json'
$manifest = Get-Content -LiteralPath $manifestSource -Raw -Encoding UTF8 | ConvertFrom-Json
$version = [string]$manifest.installer.version
$setupName = 'Logixone-Setup-' + $version + '.exe'
$buildRoot = Join-Path $installerRoot 'build'
$current = Join-Path $installerRoot 'current'
$nl = [Environment]::NewLine

if ([IO.Path]::GetFullPath($current) -ne (Join-Path $installerRoot 'current')) {
    throw 'La ruta current no coincide con el directorio exclusivo aprobado.'
}
if (-not (Test-Path -LiteralPath $buildRoot -PathType Container)) {
    New-Item -Path $buildRoot -ItemType Directory | Out-Null
}

& (Join-Path $PSScriptRoot 'build-bootstrapper.ps1') -Test
if ($LASTEXITCODE -ne 0) {
    throw 'El bootstrapper o sus pruebas no quedaron verdes.'
}

$expectedApp = [string]$manifest.baseline.applicationDigest
$expectedMigrator = [string]$manifest.baseline.migratorDigest
$actualApp = (& docker image inspect --format '{{.Id}}' ([string]$manifest.baseline.applicationImage)).Trim()
if ($LASTEXITCODE -ne 0 -or $actualApp -ne $expectedApp) {
    throw 'La imagen local de aplicación no coincide con el baseline congelado.'
}
$actualMigrator = (& docker image inspect --format '{{.Id}}' ([string]$manifest.baseline.migratorImage)).Trim()
if ($LASTEXITCODE -ne 0 -or $actualMigrator -ne $expectedMigrator) {
    throw 'La imagen local del migrador no coincide con el baseline congelado.'
}

$staging = Join-Path $buildRoot ('staging-' + [guid]::NewGuid().ToString('N'))
New-Item -Path $staging -ItemType Directory | Out-Null
$payload = Join-Path $staging 'payload.zip'

$payloadEntries = @(
    '.dockerignore',
    '.mvn',
    'mvnw',
    'mvnw.cmd',
    'pom.xml',
    'platform-bom',
    'plugin-api',
    'kernel-api',
    'kernel-domain',
    'kernel-application',
    'kernel-infrastructure-jakarta',
    'web-shell',
    'migrator',
    'plugins',
    'distribution',
    'tests',
    'tools/plugin-scaffold',
    'infra'
)
$tarArguments = @(
    '-a',
    '-cf',
    $payload,
    '--exclude=target',
    '--exclude=.tools',
    '--exclude=tmp',
    '--exclude=compose.env.local'
) + $payloadEntries

Push-Location $repositoryRoot
try {
    & tar.exe $tarArguments
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudo construir payload.zip.'
    }
}
finally {
    Pop-Location
}

$entries = & tar.exe -tf $payload
if ($LASTEXITCODE -ne 0) {
    throw 'No se pudo inspeccionar payload.zip.'
}
$requiredPayload = @(
    'pom.xml',
    'infra/compose/compose.yaml',
    'infra/docker/Dockerfile',
    'infra/docker/Dockerfile.migrator'
)
foreach ($required in $requiredPayload) {
    if ($entries -notcontains $required) {
        throw ('El payload no contiene ' + $required)
    }
}
$forbidden = $entries | Where-Object {
    $_ -match '(^|/)(target|\.tools|tmp)(/|$)' -or
    $_ -match '(^|/)compose\.env\.local$'
}
if ($forbidden) {
    throw 'El payload contiene caché, secretos, artefactos o configuración local.'
}

Copy-Item -LiteralPath (Join-Path $installerRoot 'bin\Logixone-Setup.exe') -Destination (Join-Path $staging $setupName)
Copy-Item -LiteralPath (Join-Path $installerRoot 'bin\Logixone-Installer.Cli.exe') -Destination (Join-Path $staging 'Logixone-Installer.Cli.exe')
Copy-Item -LiteralPath $manifestSource -Destination (Join-Path $staging 'installer-manifest.json')
Copy-Item -LiteralPath (Join-Path $installerRoot 'payload\THIRD-PARTY-NOTICES.txt') -Destination (Join-Path $staging 'THIRD-PARTY-NOTICES.txt')
Copy-Item -LiteralPath (Join-Path $installerRoot 'payload\INSTALLER-README.txt') -Destination (Join-Path $staging 'INSTALLER-README.txt')

$signature = Get-AuthenticodeSignature -LiteralPath (Join-Path $staging $setupName)
$buildInfo = [ordered]@{
    schemaVersion = 1
    installerVersion = $version
    sprint = [string]$manifest.installer.sprint
    story = [string]$manifest.installer.story
    profile = [string]$manifest.installer.profile
    releaseChannel = [string]$manifest.installer.releaseChannel
    builtAt = [DateTimeOffset]::Now.ToString('o')
    applicationImage = [string]$manifest.baseline.applicationImage
    applicationDigest = $expectedApp
    migratorImage = [string]$manifest.baseline.migratorImage
    migratorDigest = $expectedMigrator
    payloadEntries = @($entries).Count
    signatureStatus = [string]$signature.Status
    externalDistributionAllowed = $false
}
[IO.File]::WriteAllText(
    (Join-Path $staging 'BUILD-INFO.json'),
    (($buildInfo | ConvertTo-Json -Depth 5) + $nl),
    [Text.UTF8Encoding]::new($false)
)

$sumFiles = @(
    $setupName,
    'Logixone-Installer.Cli.exe',
    'installer-manifest.json',
    'payload.zip',
    'THIRD-PARTY-NOTICES.txt',
    'INSTALLER-README.txt',
    'BUILD-INFO.json'
)
$sumLines = foreach ($name in $sumFiles) {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $staging $name)).Hash.ToLowerInvariant()
    $hash + '  ' + $name
}
[IO.File]::WriteAllText(
    (Join-Path $staging 'SHA256SUMS.txt'),
    (($sumLines -join $nl) + $nl),
    [Text.Encoding]::ASCII
)

foreach ($name in @($manifest.generatedFiles)) {
    if (-not (Test-Path -LiteralPath (Join-Path $staging ([string]$name)) -PathType Leaf)) {
        throw ('Falta el archivo generado declarado: ' + $name)
    }
}
if (@(Get-ChildItem -LiteralPath $staging -File).Count -ne @($manifest.generatedFiles).Count) {
    throw 'La edición temporal contiene archivos no declarados.'
}

$previous = $null
if (Test-Path -LiteralPath $current -PathType Container) {
    $oldManifestPath = Join-Path $current 'installer-manifest.json'
    if (-not (Test-Path -LiteralPath $oldManifestPath -PathType Leaf)) {
        throw 'current existe pero no tiene un manifiesto verificable.'
    }
    $oldManifest = Get-Content -LiteralPath $oldManifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $allowedOld = @($oldManifest.generatedFiles | ForEach-Object { [string]$_ })
    $unexpectedOld = Get-ChildItem -LiteralPath $current -File | Where-Object { $allowedOld -notcontains $_.Name }
    if ($unexpectedOld) {
        throw 'current contiene archivos no declarados; no se reemplazará automáticamente.'
    }
    $previous = Join-Path $buildRoot ('previous-' + [guid]::NewGuid().ToString('N'))
    Move-Item -LiteralPath $current -Destination $previous
}

try {
    Move-Item -LiteralPath $staging -Destination $current
}
catch {
    if ($previous -and -not (Test-Path -LiteralPath $current)) {
        Move-Item -LiteralPath $previous -Destination $current
    }
    throw
}

if ($previous) {
    $previousManifest = Get-Content -LiteralPath (Join-Path $previous 'installer-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($name in @($previousManifest.generatedFiles)) {
        $file = Join-Path $previous ([string]$name)
        if (Test-Path -LiteralPath $file -PathType Leaf) {
            Remove-Item -LiteralPath $file -Force
        }
    }
    if (@(Get-ChildItem -LiteralPath $previous -Force).Count -ne 0) {
        throw 'El directorio anterior contiene elementos no declarados y se conservó para diagnóstico.'
    }
    Remove-Item -LiteralPath $previous -Force
}

$currentSetup = Join-Path $current $setupName
$currentHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $currentSetup).Hash
$currentBytes = (Get-Item -LiteralPath $currentSetup).Length
$currentSignature = Get-AuthenticodeSignature -LiteralPath $currentSetup
[pscustomobject]@{
    Current = $current
    Version = $version
    Files = @(Get-ChildItem -LiteralPath $current -File).Count
    Setup = $setupName
    Bytes = $currentBytes
    SHA256 = $currentHash
    Signature = [string]$currentSignature.Status
    Channel = [string]$manifest.installer.releaseChannel
    PayloadEntries = @($entries).Count
} | ConvertTo-Json -Compress

