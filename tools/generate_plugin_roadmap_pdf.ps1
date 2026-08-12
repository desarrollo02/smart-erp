[CmdletBinding()]
param()

$nativePath = Join-Path $PSScriptRoot 'generate_plugin_roadmap_pdf_native.ps1'
$nativeSource = Get-Content -LiteralPath $nativePath -Raw -Encoding UTF8
$script:PluginRoadmapRepoRoot = Split-Path -Parent $PSScriptRoot
& ([scriptblock]::Create($nativeSource))
if (-not $?) { exit 1 }
exit 0
