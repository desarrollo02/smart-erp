[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing
$storageFileType=[Windows.Storage.StorageFile,Windows.Storage,ContentType=WindowsRuntime]
$pdfDocumentType=[Windows.Data.Pdf.PdfDocument,Windows.Data.Pdf,ContentType=WindowsRuntime]
$pdfRenderOptionsType=[Windows.Data.Pdf.PdfPageRenderOptions,Windows.Data.Pdf,ContentType=WindowsRuntime]
$memoryStreamType=[Windows.Storage.Streams.InMemoryRandomAccessStream,Windows.Storage.Streams,ContentType=WindowsRuntime]
$dataReaderType=[Windows.Storage.Streams.DataReader,Windows.Storage.Streams,ContentType=WindowsRuntime]

function Wait-Operation {
 param([Parameter(Mandatory)]$Operation,[Parameter(Mandatory)][Type]$ResultType)
 $method=[System.WindowsRuntimeSystemExtensions].GetMethods()|Where-Object{$_.Name-eq'AsTask'-and$_.IsGenericMethod-and$_.GetParameters().Count-eq1}|Select-Object -First 1
 $task=$method.MakeGenericMethod($ResultType).Invoke($null,@($Operation));$task.Wait();return $task.Result
}
function Wait-Action {
 param([Parameter(Mandatory)]$Action)
 $method=[System.WindowsRuntimeSystemExtensions].GetMethods()|Where-Object{$_.Name-eq'AsTask'-and-not$_.IsGenericMethod-and$_.GetParameters().Count-eq1}|Select-Object -First 1
 $task=$method.Invoke($null,@($Action));$task.Wait()
}

$repoRoot=Split-Path -Parent $PSScriptRoot
$pdfPath=Join-Path $repoRoot 'docs\output\pdf\00-roadmap-plugins-y-orden-construccion.pdf'
$qaRoot=Join-Path $repoRoot '.tools\tmp\pdfs\roadmap-plugins\rendered'
$contactPath=Join-Path $repoRoot '.tools\tmp\pdfs\roadmap-plugins\contact-sheet.png'
if(-not(Test-Path -LiteralPath $pdfPath)){throw "No existe el PDF: $pdfPath"}
if(-not(Test-Path -LiteralPath $qaRoot)){New-Item -ItemType Directory -Path $qaRoot|Out-Null}
Get-ChildItem -LiteralPath $qaRoot -Filter 'page-*.png' -File|ForEach-Object{Remove-Item -LiteralPath $_.FullName -Force}

$storageFile=Wait-Operation ($storageFileType::GetFileFromPathAsync($pdfPath)) $storageFileType
$document=Wait-Operation ($pdfDocumentType::LoadFromFileAsync($storageFile)) $pdfDocumentType
$pagePaths=@()
for($index=0;$index-lt$document.PageCount;$index++){
 $page=$document.GetPage([uint32]$index)
 try{
  $options=$pdfRenderOptionsType::new();$options.DestinationWidth=[uint32]1400;$stream=$memoryStreamType::new()
  try{
   Wait-Action ($page.RenderToStreamAsync($stream,$options));$stream.Seek(0);$input=$stream.GetInputStreamAt(0);$reader=$dataReaderType::new($input)
   try{$size=[uint32]$stream.Size;[void](Wait-Operation ($reader.LoadAsync($size)) ([uint32]));$bytes=New-Object byte[] $size;$reader.ReadBytes($bytes);$path=Join-Path $qaRoot ('page-{0:d2}.png'-f($index+1));[IO.File]::WriteAllBytes($path,$bytes);$pagePaths+=$path}
   finally{$reader.Dispose();$input.Dispose()}
  }finally{$stream.Dispose()}
 }finally{$page.Dispose()}
}

$columns=3;$tw=360;$th=510;$ch=28;$gap=16;$rows=[Math]::Ceiling($pagePaths.Count/$columns)
$sheet=New-Object Drawing.Bitmap(($columns*($tw+$gap)+$gap),($rows*($th+$ch+$gap)+$gap));$graphics=[Drawing.Graphics]::FromImage($sheet)
try{
 $graphics.Clear([Drawing.Color]::FromArgb(235,239,245));$graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
 $font=New-Object Drawing.Font('Segoe UI',11,[Drawing.FontStyle]::Bold);$brush=New-Object Drawing.SolidBrush([Drawing.Color]::FromArgb(36,50,72))
 try{
  for($i=0;$i-lt$pagePaths.Count;$i++){
   $row=[Math]::Floor($i/$columns);$col=$i%$columns;$x=$gap+$col*($tw+$gap);$y=$gap+$row*($th+$ch+$gap);$image=[Drawing.Image]::FromFile($pagePaths[$i])
   try{$ratio=[Math]::Min($tw/$image.Width,$th/$image.Height);$w=[int][Math]::Round($image.Width*$ratio);$h=[int][Math]::Round($image.Height*$ratio);$dx=$x+[int](($tw-$w)/2);$dy=$y+[int](($th-$h)/2);$graphics.FillRectangle([Drawing.Brushes]::White,$x,$y,$tw,$th);$graphics.DrawImage($image,$dx,$dy,$w,$h);$graphics.DrawString(('Página {0}'-f($i+1)),$font,$brush,$x,$y+$th+3)}finally{$image.Dispose()}
  }
 }finally{$font.Dispose();$brush.Dispose()}
 $sheet.Save($contactPath,[Drawing.Imaging.ImageFormat]::Png)
}finally{$graphics.Dispose();$sheet.Dispose()}
Write-Host ("Render finalizado: páginas={0} | contacto={1}"-f$pagePaths.Count,$contactPath)
