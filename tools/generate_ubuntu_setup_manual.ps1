[CmdletBinding()]
param([switch]$SkipPdf)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $repoRoot 'docs\runbooks\levantar-logixone-ubuntu.md'
$webRoot = Join-Path $repoRoot 'docs\user-guide\operations\web'
$webPath = Join-Path $webRoot 'levantar-logixone-ubuntu.html'
$cssPath = Join-Path $repoRoot 'docs\user-guide\operations\manual-ubuntu.css'
$pdfRoot = Join-Path $repoRoot 'docs\output\pdf'
$pdfPath = Join-Path $pdfRoot '01-manual-levantar-smart-erp-ubuntu.pdf'
$qaRoot = Join-Path $repoRoot '.tools\tmp\pdfs\ubuntu-setup'
$edgeCandidates = @(
    'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
)
$browser = $edgeCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
$utf8 = New-Object System.Text.UTF8Encoding($false)

foreach ($directory in @($webRoot, $pdfRoot, $qaRoot)) {
    if (-not (Test-Path -LiteralPath $directory)) { New-Item -ItemType Directory -Path $directory | Out-Null }
}
foreach ($required in @($sourcePath, $cssPath)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Falta el archivo requerido: $required" }
}
if (-not $SkipPdf -and -not $browser) { throw 'Microsoft Edge no esta disponible como puente de plataforma para generar el PDF.' }

$source = [IO.File]::ReadAllText($sourcePath, $utf8)
if ($source.Contains([char]0xFFFD)) { throw 'La fuente contiene U+FFFD.' }
$wireframes = ([regex]::Matches($source, '```wireframe')).Count
$diagrams = ([regex]::Matches($source, '```diagram')).Count
$requiredTerms = @('Ubuntu 24.04 LTS', 'with-purchasing-demo', 'LOGIXONE_APP_IMAGE', 'docker compose down', 'http://localhost:8080/logixone/', '2. T', '16. Validaci')
if ($wireframes -lt 7 -or $diagrams -lt 8) { throw "Cobertura visual insuficiente: wireframes=$wireframes, diagramas=$diagrams." }
foreach ($term in $requiredTerms) { if (-not $source.Contains($term)) { throw "La fuente no contiene el termino obligatorio: $term" } }

function Convert-Inline([string]$text) {
    $encoded = [Net.WebUtility]::HtmlEncode($text.TrimEnd())
    $encoded = [regex]::Replace($encoded, '`([^`]+)`', '<code>$1</code>')
    $encoded = [regex]::Replace($encoded, '\*\*([^*]+)\*\*', '<strong>$1</strong>')
    $encoded = [regex]::Replace($encoded, '\[([^\]]+)\]\(([^)]+)\)', '<a href="$2">$1</a>')
    $encoded = $encoded.Replace('&lt;br&gt;', '<br>')
    return $encoded
}

function Get-TableCells([string]$line) {
    $trimmed = $line.Trim()
    if ($trimmed.StartsWith('|')) { $trimmed = $trimmed.Substring(1) }
    if ($trimmed.EndsWith('|')) { $trimmed = $trimmed.Substring(0, $trimmed.Length - 1) }
    return @($trimmed.Split('|') | ForEach-Object { $_.Trim() })
}

$lines = @($source -split "`r?`n")
$sectionHeadings = @()
foreach ($line in $lines) { if ($line -match '^##\s+(.+)$') { $sectionHeadings += $Matches[1] } }

$html = New-Object Text.StringBuilder
[void]$html.AppendLine('<!doctype html>')
[void]$html.AppendLine('<html lang="es"><head><meta charset="utf-8">')
[void]$html.AppendLine('<meta name="viewport" content="width=device-width, initial-scale=1">')
[void]$html.AppendLine('<meta name="author" content="Proyecto LogixOne / Smart ERP">')
[void]$html.AppendLine('<meta name="description" content="Manual para instalar y levantar Smart ERP en Ubuntu">')
[void]$html.AppendLine('<title>Manual de instalaci&oacute;n y puesta en marcha de Smart ERP en Ubuntu</title>')
[void]$html.AppendLine('<link rel="stylesheet" href="../manual-ubuntu.css"></head><body><main>')
[void]$html.AppendLine('<section class="cover">')

$inCover = $true
$tocWritten = $false
$inCode = $false
$codeLanguage = ''
$codeBuffer = New-Object Collections.Generic.List[string]
$paragraph = New-Object Collections.Generic.List[string]
$listType = ''
$sectionNumber = 0

function Close-Paragraph {
    if ($script:paragraph.Count -gt 0) {
        [void]$script:html.AppendLine('<p>' + ($script:paragraph -join ' ') + '</p>')
        $script:paragraph.Clear()
    }
}

function Close-List {
    if ($script:listType) {
        [void]$script:html.AppendLine("</$($script:listType)>")
        $script:listType = ''
    }
}

function Write-Toc {
    if ($script:tocWritten) { return }
    [void]$script:html.AppendLine('<nav class="toc" aria-label="Contenido"><h2>Contenido</h2><ol>')
    for ($tocIndex = 0; $tocIndex -lt $script:sectionHeadings.Count; $tocIndex++) {
        $tocText = Convert-Inline $script:sectionHeadings[$tocIndex]
        [void]$script:html.AppendLine(('<li><a href="#section-{0}">{1}</a></li>' -f ($tocIndex + 1), $tocText))
    }
    [void]$script:html.AppendLine('</ol></nav>')
    $script:tocWritten = $true
}

for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($inCode) {
        if ($line -match '^```\s*$') {
            $class = if ($codeLanguage -in @('diagram', 'wireframe')) { (' class="{0}"' -f $codeLanguage) } else { '' }
            $codeText = [Net.WebUtility]::HtmlEncode(($codeBuffer -join "`n"))
            [void]$html.AppendLine("<pre$class><code>$codeText</code></pre>")
            $codeBuffer.Clear(); $inCode = $false; $codeLanguage = ''
        } else { $codeBuffer.Add($line) }
        continue
    }
    if ($line -match '^```\s*([A-Za-z0-9_-]*)\s*$') {
        Close-Paragraph; Close-List
        $inCode = $true; $codeLanguage = $Matches[1]; continue
    }
    if ($line -eq '<!-- endcover -->') {
        Close-Paragraph; Close-List
        [void]$html.AppendLine('</section>'); $inCover = $false; Write-Toc; continue
    }
    if ($line -match '^#\s+(.+)$') {
        Close-Paragraph
        [void]$html.AppendLine('<h1>' + (Convert-Inline $Matches[1]) + '</h1>'); continue
    }
    if ($line -match '^##\s+(.+)$') {
        Close-Paragraph; Close-List
        if ($inCover) { [void]$html.AppendLine('</section>'); $inCover = $false; Write-Toc }
        $sectionNumber++
        [void]$html.AppendLine(('<h2 id="section-{0}">{1}</h2>' -f $sectionNumber, (Convert-Inline $Matches[1]))); continue
    }
    if ($line -match '^###\s+(.+)$') {
        Close-Paragraph; Close-List
        [void]$html.AppendLine('<h3>' + (Convert-Inline $Matches[1]) + '</h3>'); continue
    }
    if ($line -match '^####\s+(.+)$') {
        Close-Paragraph; Close-List
        [void]$html.AppendLine('<h4>' + (Convert-Inline $Matches[1]) + '</h4>'); continue
    }
    if ($line -match '^>\s*(.+)$') {
        Close-Paragraph; Close-List
        $calloutText = Convert-Inline $Matches[1]
        $calloutClass = if ($Matches[1] -match '(Advertencia|Seguridad|Regla)') { 'callout warning' } else { 'callout' }
        [void]$html.AppendLine(('<div class="{0}">{1}</div>' -f $calloutClass, $calloutText)); continue
    }
    if ($line.TrimStart().StartsWith('|') -and $i + 1 -lt $lines.Count -and $lines[$i + 1] -match '^\s*\|?\s*:?-{3,}') {
        Close-Paragraph; Close-List
        $headers = Get-TableCells $line
        [void]$html.AppendLine('<table><thead><tr>')
        foreach ($cell in $headers) { [void]$html.AppendLine('<th>' + (Convert-Inline $cell) + '</th>') }
        [void]$html.AppendLine('</tr></thead><tbody>'); $i += 2
        while ($i -lt $lines.Count -and $lines[$i].TrimStart().StartsWith('|')) {
            $cells = Get-TableCells $lines[$i]; [void]$html.AppendLine('<tr>')
            foreach ($cell in $cells) { [void]$html.AppendLine('<td>' + (Convert-Inline $cell) + '</td>') }
            [void]$html.AppendLine('</tr>'); $i++
        }
        [void]$html.AppendLine('</tbody></table>'); $i--; continue
    }
    if ($line -match '^\s*-\s+\[ \]\s+(.+)$') {
        Close-Paragraph
        if ($listType -ne 'ul') { Close-List; [void]$html.AppendLine('<ul class="check-list">'); $listType = 'ul' }
        [void]$html.AppendLine('<li><span class="check-box" aria-hidden="true"></span>' + (Convert-Inline $Matches[1]) + '</li>'); continue
    }
    if ($line -match '^\s*-\s+(.+)$') {
        Close-Paragraph
        if ($listType -ne 'ul') { Close-List; [void]$html.AppendLine('<ul>'); $listType = 'ul' }
        [void]$html.AppendLine('<li>' + (Convert-Inline $Matches[1]) + '</li>'); continue
    }
    if ($line -match '^\s*\d+\.\s+(.+)$') {
        Close-Paragraph
        if ($listType -ne 'ol') { Close-List; [void]$html.AppendLine('<ol>'); $listType = 'ol' }
        [void]$html.AppendLine('<li>' + (Convert-Inline $Matches[1]) + '</li>'); continue
    }
    if ([string]::IsNullOrWhiteSpace($line)) { Close-Paragraph; Close-List; continue }
    Close-List
    $inline = Convert-Inline $line
    if ($line.EndsWith('  ')) { $inline += '<br>' }
    $paragraph.Add($inline)
}

if ($inCode) { throw 'La fuente termina dentro de un bloque de codigo.' }
Close-Paragraph; Close-List
if ($inCover) { [void]$html.AppendLine('</section>'); Write-Toc }
[void]$html.AppendLine('</main>')
[void]$html.AppendLine('<footer class="page-footer">LogixOne - Manual de instalaci&oacute;n en Ubuntu - Edici&oacute;n 1.0</footer>')
[void]$html.AppendLine('</body></html>')
$htmlText = $html.ToString()
if ($htmlText.Contains([char]0xFFFD)) { throw 'El HTML generado contiene U+FFFD.' }
[IO.File]::WriteAllText($webPath, $htmlText, $utf8)

if (-not $SkipPdf) {
    $fileUri = ([Uri]$webPath).AbsoluteUri
    $arguments = @('--headless=new', '--disable-gpu', '--run-all-compositor-stages-before-draw', '--virtual-time-budget=5000', '--no-pdf-header-footer', "--print-to-pdf=$pdfPath", $fileUri)
    $process = Start-Process -FilePath $browser -ArgumentList $arguments -PassThru -Wait -WindowStyle Hidden
    if ($process.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $pdfPath)) { throw "Fallo la generacion del PDF. Codigo: $($process.ExitCode)" }
    $pdfInfo = Get-Item -LiteralPath $pdfPath
    if ($pdfInfo.Length -lt 50000) { throw "El PDF generado es anormalmente pequeno: $($pdfInfo.Length) bytes." }
}

Write-Host "HTML generado: $webPath"
if (-not $SkipPdf) { Write-Host "PDF generado: $pdfPath" }
