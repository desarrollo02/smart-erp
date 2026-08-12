[CmdletBinding()]
param(
    [string]$ManualSlug
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing

$storageFileType = [Windows.Storage.StorageFile, Windows.Storage, ContentType=WindowsRuntime]
$pdfDocumentType = [Windows.Data.Pdf.PdfDocument, Windows.Data.Pdf, ContentType=WindowsRuntime]
$pdfRenderOptionsType = [Windows.Data.Pdf.PdfPageRenderOptions, Windows.Data.Pdf, ContentType=WindowsRuntime]
$memoryStreamType = [Windows.Storage.Streams.InMemoryRandomAccessStream, Windows.Storage.Streams, ContentType=WindowsRuntime]
$dataReaderType = [Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType=WindowsRuntime]

function Wait-WinRtOperation {
    param(
        [Parameter(Mandatory)]$Operation,
        [Parameter(Mandatory)][Type]$ResultType
    )
    $method = [System.WindowsRuntimeSystemExtensions].GetMethods() |
        Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 } |
        Select-Object -First 1
    $task = $method.MakeGenericMethod($ResultType).Invoke($null, @($Operation))
    $task.Wait()
    return $task.Result
}

function Wait-WinRtAction {
    param([Parameter(Mandatory)]$Action)
    $method = [System.WindowsRuntimeSystemExtensions].GetMethods() |
        Where-Object { $_.Name -eq 'AsTask' -and -not $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 } |
        Select-Object -First 1
    $task = $method.Invoke($null, @($Action))
    $task.Wait()
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$pdfRoot = Join-Path $repoRoot 'docs\output\pdf\manuales-modulos'
$qaRoot = Join-Path $repoRoot '.tools\tmp\manuales-modulos\rendered'
$contactRoot = Join-Path $repoRoot '.tools\tmp\manuales-modulos\contact-sheets'
foreach ($directory in @($qaRoot, $contactRoot)) {
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory | Out-Null
    }
}

$pdfs = Get-ChildItem -LiteralPath $pdfRoot -Filter '*.pdf' | Sort-Object Name
if (-not [string]::IsNullOrWhiteSpace($ManualSlug)) {
    $pdfs = @($pdfs | Where-Object { $_.Name -like "*-manual-$ManualSlug.pdf" })
}
$expectedCount = if ([string]::IsNullOrWhiteSpace($ManualSlug)) { 7 } else { 1 }
if ($pdfs.Count -ne $expectedCount) {
    throw "Se esperaban $expectedCount PDF y se encontraron $($pdfs.Count)."
}

$renderSummary = @()
foreach ($pdf in $pdfs) {
    $slug = [IO.Path]::GetFileNameWithoutExtension($pdf.Name)
    $manualRenderRoot = Join-Path $qaRoot $slug
    if (-not (Test-Path -LiteralPath $manualRenderRoot)) {
        New-Item -ItemType Directory -Path $manualRenderRoot | Out-Null
    }
    $resolvedQaRoot = [IO.Path]::GetFullPath($qaRoot + [IO.Path]::DirectorySeparatorChar)
    $resolvedManualRenderRoot = [IO.Path]::GetFullPath($manualRenderRoot + [IO.Path]::DirectorySeparatorChar)
    if (-not $resolvedManualRenderRoot.StartsWith($resolvedQaRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "La carpeta de render queda fuera del directorio temporal permitido: $resolvedManualRenderRoot"
    }
    Get-ChildItem -LiteralPath $manualRenderRoot -Filter 'page-*.png' -File |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }

    $storageFile = Wait-WinRtOperation ($storageFileType::GetFileFromPathAsync($pdf.FullName)) $storageFileType
    $document = Wait-WinRtOperation ($pdfDocumentType::LoadFromFileAsync($storageFile)) $pdfDocumentType
    $pagePaths = @()
    for ($index = 0; $index -lt $document.PageCount; $index++) {
        $page = $document.GetPage([uint32]$index)
        try {
            $width = [uint32]1240
            $options = $pdfRenderOptionsType::new()
            $options.DestinationWidth = $width
            $stream = $memoryStreamType::new()
            try {
                Wait-WinRtAction ($page.RenderToStreamAsync($stream, $options))
                $stream.Seek(0)
                $inputStream = $stream.GetInputStreamAt(0)
                $reader = $dataReaderType::new($inputStream)
                try {
                    $size = [uint32]$stream.Size
                    [void](Wait-WinRtOperation ($reader.LoadAsync($size)) ([uint32]))
                    $bytes = New-Object byte[] $size
                    $reader.ReadBytes($bytes)
                    $pagePath = Join-Path $manualRenderRoot ('page-{0:d3}.png' -f ($index + 1))
                    [IO.File]::WriteAllBytes($pagePath, $bytes)
                    $pagePaths += $pagePath
                }
                finally {
                    $reader.Dispose()
                    $inputStream.Dispose()
                }
            }
            finally {
                $stream.Dispose()
            }
        }
        finally {
            $page.Dispose()
        }
    }

    $columns = 3
    $thumbWidth = 360
    $thumbHeight = 510
    $captionHeight = 30
    $gap = 18
    $rows = [Math]::Ceiling($pagePaths.Count / $columns)
    $sheetWidth = $columns * ($thumbWidth + $gap) + $gap
    $sheetHeight = $rows * ($thumbHeight + $captionHeight + $gap) + $gap
    $sheet = New-Object System.Drawing.Bitmap($sheetWidth, $sheetHeight)
    $graphics = [System.Drawing.Graphics]::FromImage($sheet)
    try {
        $graphics.Clear([System.Drawing.Color]::FromArgb(235, 237, 242))
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $font = New-Object System.Drawing.Font('Segoe UI', 12, [System.Drawing.FontStyle]::Bold)
        $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 48, 64))
        try {
            for ($i = 0; $i -lt $pagePaths.Count; $i++) {
                $row = [Math]::Floor($i / $columns)
                $column = $i % $columns
                $x = $gap + $column * ($thumbWidth + $gap)
                $y = $gap + $row * ($thumbHeight + $captionHeight + $gap)
                $pageImage = [System.Drawing.Image]::FromFile($pagePaths[$i])
                try {
                    $ratio = [Math]::Min($thumbWidth / $pageImage.Width, $thumbHeight / $pageImage.Height)
                    $drawWidth = [int][Math]::Round($pageImage.Width * $ratio)
                    $drawHeight = [int][Math]::Round($pageImage.Height * $ratio)
                    $drawX = $x + [int](($thumbWidth - $drawWidth) / 2)
                    $drawY = $y + [int](($thumbHeight - $drawHeight) / 2)
                    $graphics.FillRectangle([System.Drawing.Brushes]::White, $x, $y, $thumbWidth, $thumbHeight)
                    $graphics.DrawImage($pageImage, $drawX, $drawY, $drawWidth, $drawHeight)
                    $graphics.DrawString(('Pagina {0}' -f ($i + 1)), $font, $brush, $x, $y + $thumbHeight + 4)
                }
                finally {
                    $pageImage.Dispose()
                }
            }
        }
        finally {
            $font.Dispose()
            $brush.Dispose()
        }
        $contactPath = Join-Path $contactRoot ($slug + '-contact-sheet.png')
        $sheet.Save($contactPath, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $graphics.Dispose()
        $sheet.Dispose()
    }

    $renderSummary += [pscustomobject]@{
        Manual = $pdf.Name
        Pages = $pagePaths.Count
        ContactSheet = $contactPath
    }
}

$renderSummary | Format-Table -AutoSize
