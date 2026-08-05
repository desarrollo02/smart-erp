[CmdletBinding()]
param(
    [switch]$Test
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$installerRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$sourceRoot = Join-Path $installerRoot 'src'
$testRoot = Join-Path $installerRoot 'tests'
$binRoot = Join-Path $installerRoot 'bin'
$compiler = 'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe'

if (-not (Test-Path -LiteralPath $compiler -PathType Leaf)) {
    throw 'No se encontró el compilador .NET Framework x64 aprobado.'
}

if (-not (Test-Path -LiteralPath $binRoot -PathType Container)) {
    New-Item -Path $binRoot -ItemType Directory | Out-Null
}

$references = @(
    'System.dll',
    'System.Core.dll',
    'System.Drawing.dll',
    'System.IO.Compression.dll',
    'System.IO.Compression.FileSystem.dll',
    'System.Management.dll',
    'System.Web.Extensions.dll',
    'System.Windows.Forms.dll'
)
$referenceArguments = $references | ForEach-Object { '/reference:' + $_ }
$coreSource = Join-Path $sourceRoot 'Installer.Core.cs'
$planSource = Join-Path $sourceRoot 'Installer.Plan.cs'
$formSource = Join-Path $sourceRoot 'Installer.Form.cs'
$executionSource = Join-Path $sourceRoot 'Installer.Execution.cs'
$operationsSource = Join-Path $sourceRoot 'Windows.InstallationOperations.cs'
$programSource = Join-Path $sourceRoot 'Installer.Program.cs'
$cliOutput = Join-Path $binRoot 'Logixone-Installer.Cli.exe'
$output = Join-Path $binRoot 'Logixone-Setup.exe'

$commonArguments = @(
    '/nologo',
    '/platform:x64',
    '/optimize+',
    '/warnaserror+',
    '/utf8output'
) + $referenceArguments + @(
    $coreSource,
    $planSource,
    $executionSource,
    $operationsSource,
    $formSource,
    $programSource
)

& $compiler (@('/target:exe', ('/out:' + $cliOutput)) + $commonArguments)
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $cliOutput -PathType Leaf)) {
    throw 'Falló la compilación del CLI de diagnóstico.'
}

& $compiler (@('/target:winexe', ('/out:' + $output)) + $commonArguments)
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $output -PathType Leaf)) {
    throw 'Falló la compilación del bootstrapper.'
}

Write-Output ('BOOTSTRAPPER=' + $output)
Write-Output ('CLI=' + $cliOutput)

if ($Test) {
    $testOutput = Join-Path $binRoot 'PreflightEvaluatorTests.exe'
    $testSource = Join-Path $testRoot 'PreflightEvaluatorTests.cs'
    $testArguments = @(
        '/nologo',
        '/target:exe',
        '/platform:x64',
        '/optimize+',
        '/warnaserror+',
        '/utf8output',
        '/main:PreflightEvaluatorTests',
        ('/out:' + $testOutput)
    ) + $referenceArguments + @(
        $coreSource,
        $planSource,
        $executionSource,
        $operationsSource,
        $testSource
    )
    & $compiler $testArguments
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $testOutput -PathType Leaf)) {
        throw 'Falló la compilación de pruebas del preflight.'
    }
    & $testOutput
    if ($LASTEXITCODE -ne 0) {
        throw 'Fallaron las pruebas del preflight.'
    }
}
