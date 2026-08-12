[CmdletBinding()]
param(
    [switch]$SkipPdf,
    [string]$ManualSlug
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repoRoot 'docs\user-guide\modules'
$webRoot = Join-Path $sourceRoot 'web'
$pdfRoot = Join-Path $repoRoot 'docs\output\pdf\manuales-modulos'
$qaRoot = Join-Path $repoRoot '.tools\tmp\manuales-modulos'
$stylePath = Join-Path $sourceRoot 'manuales.css'
$utf8 = New-Object System.Text.UTF8Encoding($false)

$manuals = @(
    @{ Order = 1; Slug = 'administracion-kernel'; Title = 'Administracion segura del kernel'; Screens = 6; Screenshots = 6 },
    @{ Order = 2; Slug = 'datos-referencia'; Title = 'Datos de referencia'; Screens = 1 },
    @{ Order = 3; Slug = 'socios-comerciales'; Title = 'Socios comerciales'; Screens = 2 },
    @{ Order = 4; Slug = 'catalogo-comercial'; Title = 'Catalogo comercial'; Screens = 5 },
    @{ Order = 5; Slug = 'inventario'; Title = 'Inventario'; Screens = 3 },
    @{ Order = 6; Slug = 'panel-demostracion'; Title = 'Panel de demostracion'; Screens = 1 },
    @{ Order = 7; Slug = 'compras'; Title = 'Compras'; Screens = 5 }
)

if (-not [string]::IsNullOrWhiteSpace($ManualSlug)) {
    $manuals = @($manuals | Where-Object { $_.Slug -eq $ManualSlug })
    if ($manuals.Count -eq 0) {
        throw "Manual desconocido: $ManualSlug"
    }
}

foreach ($directory in @($webRoot, $pdfRoot, $qaRoot)) {
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory | Out-Null
    }
}

if (-not (Test-Path -LiteralPath $stylePath)) {
    throw "No existe la hoja de estilos: $stylePath"
}
$css = [IO.File]::ReadAllText($stylePath, $utf8)

$edgeCandidates = @(
    'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
)
$edge = $edgeCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $SkipPdf -and -not $edge) {
    throw 'Microsoft Edge no está disponible para la generación PDF reproducible.'
}

foreach ($manual in $manuals) {
    $sourcePath = Join-Path $sourceRoot ($manual.Slug + '.md')
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "Falta la fuente del manual: $sourcePath"
    }
    $body = [IO.File]::ReadAllText($sourcePath, $utf8)
    $screenCount = ([regex]::Matches($body, '<section class="screen"')).Count
    $wireframeCount = ([regex]::Matches($body, 'Bosquejo orientativo de la pantalla')).Count
    $diagramCount = ([regex]::Matches($body, 'Diagrama de datos y tablas afectadas')).Count
    if ($screenCount -ne $manual.Screens -or $wireframeCount -ne $manual.Screens -or $diagramCount -ne $manual.Screens) {
        throw "Cobertura incompleta en $($manual.Slug): pantallas=$screenCount, bosquejos=$wireframeCount, diagramas=$diagramCount, esperado=$($manual.Screens)."
    }
    if ($manual.ContainsKey('Screenshots')) {
        $screenshotCount = ([regex]::Matches($body, 'Captura real de la pantalla')).Count
        if ($screenshotCount -ne $manual.Screenshots) {
            throw "Cobertura visual incompleta en $($manual.Slug): capturas=$screenshotCount, esperado=$($manual.Screenshots)."
        }
    }
    # Las fuentes se leen correctamente desde modules/. Los HTML generados viven
    # un nivel mas abajo, por lo que sus imagenes necesitan un prefijo adicional.
    $body = $body.Replace('src="images/', 'src="../images/')
    $html = @"
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="author" content="Proyecto LogixOne / Smart ERP">
  <meta name="description" content="Manual de usuario del modulo $($manual.Title)">
  <title>Manual de usuario - $($manual.Title)</title>
  <style>$css</style>
</head>
<body>
$body
</body>
</html>
"@
    $webPath = Join-Path $webRoot ($manual.Slug + '.html')
    [IO.File]::WriteAllText($webPath, $html, $utf8)

    if (-not $SkipPdf) {
        $pdfName = ('{0:d2}-manual-{1}.pdf' -f $manual.Order, $manual.Slug)
        $pdfPath = Join-Path $pdfRoot $pdfName
        $fileUri = ([Uri]$webPath).AbsoluteUri
        $arguments = @(
            '--headless=new',
            '--disable-gpu',
            '--run-all-compositor-stages-before-draw',
            '--virtual-time-budget=3000',
            '--no-pdf-header-footer',
            "--print-to-pdf=$pdfPath",
            $fileUri
        )
        $process = Start-Process -FilePath $edge -ArgumentList $arguments -PassThru -Wait -WindowStyle Hidden
        if ($process.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $pdfPath)) {
            throw "Falló la generación PDF de $($manual.Title). Código: $($process.ExitCode)"
        }
        $pdfInfo = Get-Item -LiteralPath $pdfPath
        if ($pdfInfo.Length -lt 10000) {
            throw "El PDF de $($manual.Title) es anormalmente pequeño: $($pdfInfo.Length) bytes."
        }
    }
}

Write-Host "Generados $($manuals.Count) manuales web$($(if ($SkipPdf) { '' } else { ' y PDF' }))."
