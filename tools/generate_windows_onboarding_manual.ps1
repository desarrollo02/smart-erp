[CmdletBinding()]
param(
    [switch]$SkipPdf
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$operationsRoot = Join-Path $repoRoot 'docs\user-guide\operations'
$sourcePath = Join-Path $operationsRoot 'instalador-windows-puesta-en-marcha.md'
$webRoot = Join-Path $operationsRoot 'web'
$webPath = Join-Path $webRoot 'instalador-windows-puesta-en-marcha.html'
$stylePath = Join-Path $repoRoot 'docs\user-guide\modules\manuales.css'
$modulePdfRoot = Join-Path $repoRoot 'docs\output\pdf\manuales-modulos'
$outputPath = Join-Path $repoRoot 'docs\output\pdf\02-manual-instalador-windows-y-puesta-en-marcha.pdf'
$qaRoot = Join-Path $repoRoot '.tools\tmp\windows-onboarding-manual'
$frontPdf = Join-Path $qaRoot 'front.pdf'
$python = Join-Path $repoRoot '.tools\python\3.13.14\python.exe'
$mergeScript = Join-Path $repoRoot 'tools\merge_windows_onboarding_pdf.py'
$utf8 = New-Object System.Text.UTF8Encoding($false)

foreach ($directory in @($webRoot, $qaRoot, (Split-Path -Parent $outputPath))) {
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory | Out-Null
    }
}

foreach ($required in @($sourcePath, $stylePath)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Falta un insumo del manual: $required"
    }
}

$body = [IO.File]::ReadAllText($sourcePath, $utf8)
$screenCount = ([regex]::Matches($body, '<section class="screen"')).Count
$wireframeCount = ([regex]::Matches($body, 'Bosquejo orientativo de la pantalla')).Count
$diagramCount = ([regex]::Matches($body, 'Diagrama de datos y tablas afectadas')).Count
if ($screenCount -ne 4 -or $wireframeCount -ne 4 -or $diagramCount -ne 4) {
    throw "Cobertura incompleta: pantallas=$screenCount, bosquejos=$wireframeCount, diagramas=$diagramCount; se esperaban 4 de cada uno."
}

$requiredFragments = @(
    '0.9.0-internal.1',
    'INTERNAL_UNSIGNED',
    'payload.zip',
    'COMPATIBLE_CON_ADVERTENCIAS',
    'kernel.security.manage',
    'reference_data',
    'purchasing',
    'demo.empresas.ab',
    'docker compose down --volumes'
)
foreach ($fragment in $requiredFragments) {
    if (-not $body.Contains($fragment)) {
        throw "Falta contenido obligatorio en el manual: $fragment"
    }
}

$css = [IO.File]::ReadAllText($stylePath, $utf8)
$webBody = $body.Replace('src="../../evidence/', 'src="../../../evidence/')
$webBody = $webBody.Replace('href="../modules/', 'href="../../modules/')
$html = @"
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="author" content="Proyecto LogixOne / Smart ERP">
  <meta name="description" content="Manual del instalador Windows y puesta en marcha completa de LogixOne">
  <title>LogixOne - Instalador Windows y puesta en marcha</title>
  <style>
$css
@page {
  @bottom-center {
    content: "LogixOne \00b7 Instalador Windows y puesta en marcha \00b7 p\00e1gina " counter(page) " de " counter(pages);
  }
}
  </style>
</head>
<body>
$webBody
</body>
</html>
"@
[IO.File]::WriteAllText($webPath, $html, $utf8)

if (-not $SkipPdf) {
    if (-not (Test-Path -LiteralPath $python)) {
        throw "Falta el Python aislado del proyecto: $python"
    }
    if (-not (Test-Path -LiteralPath $mergeScript)) {
        throw "Falta el compilador PDF: $mergeScript"
    }
    $edgeCandidates = @(
        'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
        'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
    )
    $edge = $edgeCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $edge) {
        throw 'Microsoft Edge no está disponible para la generación PDF reproducible.'
    }
    $fileUri = ([Uri]$webPath).AbsoluteUri
    $arguments = @(
        '--headless=new',
        '--disable-gpu',
        '--run-all-compositor-stages-before-draw',
        '--virtual-time-budget=4000',
        '--no-pdf-header-footer',
        "--print-to-pdf=$frontPdf",
        $fileUri
    )
    $process = Start-Process -FilePath $edge -ArgumentList $arguments -PassThru -Wait -WindowStyle Hidden
    if ($process.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $frontPdf)) {
        throw "Falló la generación del PDF inicial. Código: $($process.ExitCode)"
    }
    if ((Get-Item -LiteralPath $frontPdf).Length -lt 20000) {
        throw 'El PDF inicial es anormalmente pequeño.'
    }
    & $python $mergeScript --front $frontPdf --modules $modulePdfRoot --output $outputPath
    if ($LASTEXITCODE -ne 0) {
        throw "Falló la compilación del volumen PDF. Código: $LASTEXITCODE"
    }
}

Write-Host "Ayuda web generada: $webPath"
if (-not $SkipPdf) {
    Write-Host "PDF generado: $outputPath"
}
