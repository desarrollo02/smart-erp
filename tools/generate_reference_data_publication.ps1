[CmdletBinding()]
param(
    [string]$CountrySource,
    [string]$CurrencySource,
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$toolsRoot = [IO.Path]::GetFullPath((Join-Path $repositoryRoot '.tools'))
$downloadRoot = [IO.Path]::GetFullPath((Join-Path $toolsRoot 'downloads\reference-data'))
$temporaryRoot = [IO.Path]::GetFullPath((Join-Path $toolsRoot 'tmp'))

if ([string]::IsNullOrWhiteSpace($CountrySource)) {
    $CountrySource = Join-Path $downloadRoot 'un-m49-overview-2026-08-04.html'
}
if ([string]::IsNullOrWhiteSpace($CurrencySource)) {
    $CurrencySource = Join-Path $downloadRoot 'iso-4217-list-one-2026-08-04.xml'
}
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $temporaryRoot 'reference-data\V4__publish_full_reference_data.sql'
}

$CountrySource = [IO.Path]::GetFullPath($CountrySource)
$CurrencySource = [IO.Path]::GetFullPath($CurrencySource)
$OutputPath = [IO.Path]::GetFullPath($OutputPath)

foreach ($source in @($CountrySource, $CurrencySource)) {
    if (-not $source.StartsWith(
            $downloadRoot + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase)) {
        throw "Reference source must remain under .tools/downloads/reference-data: $source"
    }
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
        throw "Missing validated reference source: $source"
    }
}
if (-not $OutputPath.StartsWith(
        $temporaryRoot + [IO.Path]::DirectorySeparatorChar,
        [StringComparison]::OrdinalIgnoreCase)) {
    throw "Generated output must remain under .tools/tmp: $OutputPath"
}

function Assert-Artifact {
    param(
        [string]$Path,
        [long]$ExpectedLength,
        [string]$ExpectedSha256
    )
    $item = Get-Item -LiteralPath $Path
    if ($item.Length -ne $ExpectedLength) {
        throw "Unexpected source length for ${Path}: $($item.Length)"
    }
    $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $ExpectedSha256) {
        throw "Unexpected source SHA-256 for ${Path}: $actual"
    }
}

function Plain-Text {
    param([string]$Html)
    $withoutTags = [regex]::Replace($Html, '<[^>]+>', ' ')
    $decoded = [Net.WebUtility]::HtmlDecode($withoutTags)
    return [regex]::Replace($decoded, '\s+', ' ').Trim()
}

function Sql-Text {
    param([string]$Value)
    return "'" + $Value.Replace("'", "''") + "'"
}

Assert-Artifact `
    -Path $CountrySource `
    -ExpectedLength 1721568 `
    -ExpectedSha256 '748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11'
Assert-Artifact `
    -Path $CurrencySource `
    -ExpectedLength 47463 `
    -ExpectedSha256 '838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9'

$countryHtml = Get-Content -LiteralPath $CountrySource -Raw -Encoding UTF8
$countryTable = [regex]::Match(
    $countryHtml,
    '(?is)<table\s+id\s*=\s*"downloadTableEN"[^>]*>.*?</table>')
if (-not $countryTable.Success) {
    throw 'UN M49 English table was not found'
}
$countryBody = [regex]::Match($countryTable.Value, '(?is)<tbody>(.*?)</tbody>')
if (-not $countryBody.Success) {
    throw 'UN M49 English table body was not found'
}

$countries = foreach ($rowMatch in [regex]::Matches($countryBody.Groups[1].Value, '(?is)<tr>(.*?)</tr>')) {
    $cells = @([regex]::Matches($rowMatch.Groups[1].Value, '(?is)<td[^>]*>(.*?)</td>') |
        ForEach-Object { Plain-Text $_.Groups[1].Value })
    if ($cells.Count -lt 12) {
        throw "UN M49 row has only $($cells.Count) cells"
    }
    $country = [pscustomobject]@{
        Name = $cells[8]
        Numeric = $cells[9]
        Alpha2 = $cells[10]
        Alpha3 = $cells[11]
    }
    if ($country.Alpha2 -notmatch '^[A-Z]{2}$' -or
            $country.Alpha3 -notmatch '^[A-Z]{3}$' -or
            $country.Numeric -notmatch '^[0-9]{3}$' -or
            [string]::IsNullOrWhiteSpace($country.Name)) {
        throw "Invalid UN M49 country row: $($country | ConvertTo-Json -Compress)"
    }
    $country
}
$countries = @($countries | Sort-Object Alpha2)
if ($countries.Count -ne 248) {
    throw "Expected 248 countries, found $($countries.Count)"
}
foreach ($property in @('Alpha2', 'Alpha3', 'Numeric')) {
    $duplicates = @($countries | Group-Object $property | Where-Object Count -ne 1)
    if ($duplicates.Count -ne 0) {
        throw "Duplicate country $property values: $(($duplicates.Name | Sort-Object) -join ',')"
    }
}

[xml]$currencyXml = Get-Content -LiteralPath $CurrencySource -Raw -Encoding UTF8
if ([string]$currencyXml.ISO_4217.Pblshd -ne '2026-01-01') {
    throw "Unexpected ISO 4217 publication date: $($currencyXml.ISO_4217.Pblshd)"
}
$currencyRows = @($currencyXml.ISO_4217.CcyTbl.CcyNtry |
    Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_.Ccy) })
if ($currencyRows.Count -ne 277) {
    throw "Expected 277 ISO 4217 country rows, found $($currencyRows.Count)"
}

$currencies = foreach ($group in ($currencyRows | Group-Object { [string]$_.Ccy })) {
    $names = @($group.Group | ForEach-Object { [string]$_.CcyNm } | Sort-Object -Unique)
    $numericCodes = @($group.Group | ForEach-Object { [string]$_.CcyNbr } | Sort-Object -Unique)
    $minorUnits = @($group.Group | ForEach-Object { [string]$_.CcyMnrUnts } | Sort-Object -Unique)
    if ($names.Count -ne 1 -or $numericCodes.Count -ne 1 -or $minorUnits.Count -ne 1) {
        throw "Conflicting ISO 4217 rows for $($group.Name)"
    }
    $currency = [pscustomobject]@{
        Code = $group.Name
        Name = $names[0].Trim()
        Numeric = $numericCodes[0]
        MinorUnit = $minorUnits[0]
    }
    if ($currency.Code -notmatch '^[A-Z]{3}$' -or
            $currency.Numeric -notmatch '^[0-9]{3}$' -or
            ($currency.MinorUnit -ne 'N.A.' -and $currency.MinorUnit -notmatch '^[0-9]$') -or
            [string]::IsNullOrWhiteSpace($currency.Name)) {
        throw "Invalid ISO 4217 currency row: $($currency | ConvertTo-Json -Compress)"
    }
    $currency
}
$currencies = @($currencies | Sort-Object Code)
if ($currencies.Count -ne 178) {
    throw "Expected 178 currency codes, found $($currencies.Count)"
}
$duplicateCurrencyNumbers = @($currencies | Group-Object Numeric | Where-Object Count -ne 1)
if ($duplicateCurrencyNumbers.Count -ne 0) {
    throw "Duplicate currency numeric codes: $(($duplicateCurrencyNumbers.Name | Sort-Object) -join ',')"
}
$notApplicable = @($currencies | Where-Object MinorUnit -eq 'N.A.')
if ($notApplicable.Count -ne 13) {
    throw "Expected 13 N.A. minor units, found $($notApplicable.Count)"
}

$builder = [Text.StringBuilder]::new()
[void]$builder.AppendLine('-- Generated deterministically by tools/generate_reference_data_publication.ps1.')
[void]$builder.AppendLine('-- Sources are validated locally under .tools/downloads/reference-data/.')
[void]$builder.AppendLine()
[void]$builder.AppendLine('UPDATE catalog_release SET current_release = FALSE')
[void]$builder.AppendLine("WHERE catalog_kind IN ('COUNTRY', 'CURRENCY') AND current_release;")
[void]$builder.AppendLine()
[void]$builder.AppendLine('INSERT INTO catalog_release (')
[void]$builder.AppendLine('    catalog_kind, release_id, standard_id, authority, source_uri, source_sha256,')
[void]$builder.AppendLine('    observed_on, completeness, entry_count, current_release)')
[void]$builder.AppendLine('VALUES')
[void]$builder.AppendLine("    ('COUNTRY', 'un-m49-2026-08-04-full', 'UN M49 with ISO alpha codes',")
[void]$builder.AppendLine("     'United Nations Statistics Division',")
[void]$builder.AppendLine("     'https://unstats.un.org/unsd/methodology/m49/overview/',")
[void]$builder.AppendLine("     '748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11',")
[void]$builder.AppendLine("     DATE '2026-08-04', 'FULL', 248, TRUE),")
[void]$builder.AppendLine("    ('CURRENCY', 'six-list-one-2026-01-01-full', 'ISO 4217 List One 2026-01-01',")
[void]$builder.AppendLine("     'SIX Financial Information AG',")
[void]$builder.AppendLine("     'https://www.six-group.com/dam/download/financial-information/data-center/iso-currrency/lists/list-one.xml',")
[void]$builder.AppendLine("     '838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9',")
[void]$builder.AppendLine("     DATE '2026-08-04', 'FULL', 178, TRUE);")
[void]$builder.AppendLine()
[void]$builder.AppendLine('INSERT INTO country_entry (')
[void]$builder.AppendLine('    catalog_kind, release_id, alpha2_code, alpha3_code, numeric_code, display_name)')
[void]$builder.AppendLine('VALUES')
for ($index = 0; $index -lt $countries.Count; $index++) {
    $country = $countries[$index]
    $suffix = if ($index -eq $countries.Count - 1) { ';' } else { ',' }
    [void]$builder.AppendLine((
        "    ('COUNTRY', 'un-m49-2026-08-04-full', {0}, {1}, {2}, {3}){4}" -f
        (Sql-Text $country.Alpha2),
        (Sql-Text $country.Alpha3),
        (Sql-Text $country.Numeric),
        (Sql-Text $country.Name),
        $suffix))
}
[void]$builder.AppendLine()
[void]$builder.AppendLine('INSERT INTO currency_entry (')
[void]$builder.AppendLine('    catalog_kind, release_id, alphabetic_code, numeric_code, minor_unit, display_name)')
[void]$builder.AppendLine('VALUES')
for ($index = 0; $index -lt $currencies.Count; $index++) {
    $currency = $currencies[$index]
    $minorUnit = if ($currency.MinorUnit -eq 'N.A.') { 'NULL' } else { $currency.MinorUnit }
    $suffix = if ($index -eq $currencies.Count - 1) { ';' } else { ',' }
    [void]$builder.AppendLine((
        "    ('CURRENCY', 'six-list-one-2026-01-01-full', {0}, {1}, {2}, {3}){4}" -f
        (Sql-Text $currency.Code),
        (Sql-Text $currency.Numeric),
        $minorUnit,
        (Sql-Text $currency.Name),
        $suffix))
}

$outputDirectory = Split-Path -Parent $OutputPath
[IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
[IO.File]::WriteAllText(
    $OutputPath,
    $builder.ToString().Replace("`r`n", "`n"),
    [Text.UTF8Encoding]::new($false))

$outputHash = (Get-FileHash -LiteralPath $OutputPath -Algorithm SHA256).Hash.ToLowerInvariant()
[pscustomobject]@{
    output = $OutputPath
    sha256 = $outputHash
    countries = $countries.Count
    currencies = $currencies.Count
    not_applicable_minor_units = $notApplicable.Count
    source_currency_rows = $currencyRows.Count
} | ConvertTo-Json -Compress
