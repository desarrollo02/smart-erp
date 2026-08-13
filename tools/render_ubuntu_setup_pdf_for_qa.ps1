[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing
$storageFileType = [Windows.Storage.StorageFile,Windows.Storage,ContentType=WindowsRuntime]
$pdfDocumentType = [Windows.Data.Pdf.PdfDocument,Windows.Data.Pdf,ContentType=WindowsRuntime]
$pdfRenderOptionsType = [Windows.Data.Pdf.PdfPageRenderOptions,Windows.Data.Pdf,ContentType=WindowsRuntime]
$memoryStreamType = [Windows.Storage.Streams.InMemoryRandomAccessStream,Windows.Storage.Streams,ContentType=WindowsRuntime]
$dataReaderType = [Windows.Storage.Streams.DataReader,Windows.Storage.Streams,ContentType=WindowsRuntime]

function Wait-Operation {
    param([Parameter(Mandatory)]$Operation, [Parameter(Mandatory)][Type]$ResultType)
    $method = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1
    $task = $method.MakeGenericMethod($ResultType).Invoke($null, @($Operation))
    $task.Wait()
    return $task.Result
}

function Wait-Action {
    param([Parameter(Mandatory)]$Action)
    $method = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and -not $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1
    $task = $method.Invoke($null, @($Action))
    $task.Wait()
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$pdfPath = Join-Path $repoRoot 'docs\output\pdf\01-manual-levantar-smart-erp-ubuntu.pdf'
$qaBase = Join-Path $repoRoot '.tools\tmp\pdfs\ubuntu-setup'
$qaRoot = Join-Path $qaBase 'rendered'
$contactPath = Join-Path $qaBase 'contact-sheet.png'
if (-not (Test-Path -LiteralPath $pdfPath)) { throw "No existe el PDF: $pdfPath" }
if (-not (Test-Path -LiteralPath $qaRoot)) { New-Item -ItemType Directory -Path $qaRoot | Out-Null }

$resolvedRepo = (Resolve-Path -LiteralPath $repoRoot).Path
$resolvedQa = (Resolve-Path -LiteralPath $qaRoot).Path
$expectedPrefix = Join-Path $resolvedRepo '.tools\tmp'
if (-not $resolvedQa.StartsWith($expectedPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "La carpeta de QA quedo fuera de .tools/tmp: $resolvedQa"
}
Get-ChildItem -LiteralPath $resolvedQa -Filter 'page-*.png' -File | ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }

$storageFile = Wait-Operation ($storageFileType::GetFileFromPathAsync($pdfPath)) $storageFileType
$document = Wait-Operation ($pdfDocumentType::LoadFromFileAsync($storageFile)) $pdfDocumentType
$pagePaths = @()
for ($index = 0; $index -lt $document.PageCount; $index++) {
    $page = $document.GetPage([uint32]$index)
    try {
        $options = $pdfRenderOptionsType::new()
        $options.DestinationWidth = [uint32]1400
        $stream = $memoryStreamType::new()
        try {
            Wait-Action ($page.RenderToStreamAsync($stream, $options))
            $stream.Seek(0)
            $input = $stream.GetInputStreamAt(0)
            $reader = $dataReaderType::new($input)
            try {
                $size = [uint32]$stream.Size
                [void](Wait-Operation ($reader.LoadAsync($size)) ([uint32]))
                $bytes = New-Object byte[] $size
                $reader.ReadBytes($bytes)
                $path = Join-Path $resolvedQa ('page-{0:d2}.png' -f ($index + 1))
                [IO.File]::WriteAllBytes($path, $bytes)
                $pagePaths += $path
            }
            finally { $reader.Dispose(); $input.Dispose() }
        }
        finally { $stream.Dispose() }
    }
    finally { $page.Dispose() }
}

$columns = 4
$thumbWidth = 280
$thumbHeight = 396
$captionHeight = 24
$gap = 12
$rows = [Math]::Ceiling($pagePaths.Count / $columns)
$sheet = New-Object Drawing.Bitmap(($columns * ($thumbWidth + $gap) + $gap), ($rows * ($thumbHeight + $captionHeight + $gap) + $gap))
$graphics = [Drawing.Graphics]::FromImage($sheet)
try {
    $graphics.Clear([Drawing.Color]::FromArgb(235, 239, 245))
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $font = New-Object Drawing.Font('Segoe UI', 9, [Drawing.FontStyle]::Bold)
    $brush = New-Object Drawing.SolidBrush([Drawing.Color]::FromArgb(36, 50, 72))
    try {
        for ($i = 0; $i -lt $pagePaths.Count; $i++) {
            $row = [Math]::Floor($i / $columns)
            $column = $i % $columns
            $x = $gap + $column * ($thumbWidth + $gap)
            $y = $gap + $row * ($thumbHeight + $captionHeight + $gap)
            $image = [Drawing.Image]::FromFile($pagePaths[$i])
            try {
                $ratio = [Math]::Min($thumbWidth / $image.Width, $thumbHeight / $image.Height)
                $width = [int][Math]::Round($image.Width * $ratio)
                $height = [int][Math]::Round($image.Height * $ratio)
                $drawX = $x + [int](($thumbWidth - $width) / 2)
                $drawY = $y + [int](($thumbHeight - $height) / 2)
                $graphics.FillRectangle([Drawing.Brushes]::White, $x, $y, $thumbWidth, $thumbHeight)
                $graphics.DrawImage($image, $drawX, $drawY, $width, $height)
                $graphics.DrawString(('Pagina {0}' -f ($i + 1)), $font, $brush, $x, $y + $thumbHeight + 3)
            }
            finally { $image.Dispose() }
        }
    }
    finally { $font.Dispose(); $brush.Dispose() }
    $sheet.Save($contactPath, [Drawing.Imaging.ImageFormat]::Png)
}
finally { $graphics.Dispose(); $sheet.Dispose() }

Write-Host ("Render finalizado: paginas={0} | contacto={1}" -f $pagePaths.Count, $contactPath)
