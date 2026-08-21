$ErrorActionPreference = 'Stop'
$repoRoot = $script:PluginRoadmapRepoRoot
$outputRoot = Join-Path $repoRoot 'docs\output\pdf'
$outputPath = Join-Path $outputRoot '00-roadmap-plugins-y-orden-construccion.pdf'
if (-not (Test-Path -LiteralPath $outputRoot)) { New-Item -ItemType Directory -Path $outputRoot | Out-Null }

$script:PW = 595.276
$script:PH = 841.890
$script:Objects = New-Object 'System.Collections.Generic.List[byte[]]'
$script:PageObjects = New-Object 'System.Collections.Generic.List[int]'
$script:Encoding = [Text.Encoding]::GetEncoding(1252)
$script:Content = $null
$invariant = [Globalization.CultureInfo]::InvariantCulture

function B([string]$value) { return $script:Encoding.GetBytes($value) }
function N([double]$value) { return $value.ToString('0.###', $invariant) }
function Add-Object([byte[]]$bytes) { [void]$script:Objects.Add($bytes); return $script:Objects.Count }
function Set-Object([int]$number, [byte[]]$bytes) { $script:Objects[$number - 1] = $bytes }
function Cmd([string]$command) { [void]$script:Content.Append($command) }

function Escape-Text([string]$text) {
    return $text.Replace('\', '\\').Replace('(', '\(').Replace(')', '\)')
}

function Text-Width([string]$text, [double]$size) {
    $units = 0.0
    foreach ($c in $text.ToCharArray()) {
        if ($c -eq ' ') { $units += 0.28 }
        elseif ('ilI.,:;!|'.Contains([string]$c)) { $units += 0.27 }
        elseif ('mwMW@%'.Contains([string]$c)) { $units += 0.82 }
        elseif ([char]::IsUpper($c)) { $units += 0.64 }
        elseif ([char]::IsDigit($c)) { $units += 0.56 }
        else { $units += 0.51 }
    }
    return $units * $size
}

function Split-Word([string]$word, [double]$maxWidth, [double]$size) {
    $parts = New-Object 'System.Collections.Generic.List[string]'
    $current = ''
    foreach ($c in $word.ToCharArray()) {
        $candidate = $current + $c
        if ($current.Length -gt 0 -and (Text-Width $candidate $size) -gt $maxWidth) {
            [void]$parts.Add($current); $current = [string]$c
        } else { $current = $candidate }
    }
    if ($current) { [void]$parts.Add($current) }
    return $parts.ToArray()
}

function Wrap([string]$text, [double]$maxWidth, [double]$size) {
    $tokens = New-Object 'System.Collections.Generic.List[string]'
    foreach ($word in ($text -split '\s+')) {
        if ((Text-Width $word $size) -le $maxWidth) { [void]$tokens.Add($word) }
        else { foreach ($part in (Split-Word $word $maxWidth $size)) { [void]$tokens.Add($part) } }
    }
    $lines = New-Object 'System.Collections.Generic.List[string]'
    $line = ''
    foreach ($token in $tokens) {
        $candidate = if ($line) { "$line $token" } else { $token }
        if ($line -and (Text-Width $candidate $size) -gt $maxWidth) { [void]$lines.Add($line); $line = $token }
        else { $line = $candidate }
    }
    if ($line) { [void]$lines.Add($line) }
    if ($lines.Count -eq 0) { [void]$lines.Add('') }
    return $lines.ToArray()
}

function Rect([double]$x, [double]$top, [double]$width, [double]$height, [string]$fill, [string]$stroke = '') {
    $y = $script:PH - $top - $height
    if ($fill) { Cmd "$fill rg`n" }
    if ($stroke) { Cmd "$stroke RG`n" }
    $op = if ($fill -and $stroke) { 'B' } elseif ($fill) { 'f' } else { 'S' }
    Cmd ("{0} {1} {2} {3} re {4}`n" -f (N $x),(N $y),(N $width),(N $height),$op)
}

function Line([double]$x1,[double]$t1,[double]$x2,[double]$t2,[string]$color='0.78 0.82 0.88',[double]$width=1) {
    Cmd "$color RG`n"
    Cmd ("{0} w {1} {2} m {3} {4} l S`n" -f (N $width),(N $x1),(N ($script:PH-$t1)),(N $x2),(N ($script:PH-$t2)))
}

function Text([string]$value,[double]$x,[double]$top,[double]$size=10,[switch]$Bold,[string]$color='0.086 0.125 0.2') {
    $font = if ($Bold) { 'F2' } else { 'F1' }
    $baseline = $script:PH - $top - ($size * 0.78)
    Cmd ("BT /{0} {1} Tf {2} rg 1 0 0 1 {3} {4} Tm ({5}) Tj ET`n" -f $font,(N $size),$color,(N $x),(N $baseline),(Escape-Text $value))
}

function Wrapped([string]$value,[double]$x,[double]$top,[double]$maxWidth,[double]$size=9,[double]$leading=11,[switch]$Bold,[string]$color='0.086 0.125 0.2',[int]$maxLines=0) {
    $lines = @(Wrap $value $maxWidth $size)
    if ($maxLines -gt 0 -and $lines.Count -gt $maxLines) {
        $lines = $lines[0..($maxLines-1)]; $last=$lines.Count-1
        if ($lines[$last].Length -gt 3) { $lines[$last]=$lines[$last].Substring(0,$lines[$last].Length-3)+'...' }
    }
    for ($i=0;$i -lt $lines.Count;$i++) { Text $lines[$i] $x ($top+($i*$leading)) $size -Bold:$Bold -color $color }
    return $top + ($lines.Count*$leading)
}

function Start-Page([string]$section='') {
    $script:Content = New-Object Text.StringBuilder
    Rect 0 0 $script:PW $script:PH '1 1 1'
    if ($section) {
        Text 'SMART ERP' 40 26 8 -Bold -color '0.122 0.31 0.561'
        Text $section 440 26 7.5 -color '0.34 0.38 0.47'
        Line 40 39 555 39 '0.68 0.75 0.85' 0.8
    }
}

function Finish-Page([int]$pageNumber,[int]$pagesObject,[int]$fontRegular,[int]$fontBold,[switch]$Cover) {
    if (-not $Cover) {
        Line 40 806 555 806 '0.76 0.8 0.86' 0.7
        Text 'Guía 00 - Plugins y orden de construcción' 40 813 7.2 -color '0.4 0.44 0.52'
        Text ("Página $pageNumber") 515 813 7.2 -color '0.4 0.44 0.52'
    }
    $contentBytes = B $script:Content.ToString()
    $head = B ("<< /Length {0} >>`nstream`n" -f $contentBytes.Length)
    $tail = B "`nendstream"
    $stream = New-Object byte[] ($head.Length+$contentBytes.Length+$tail.Length)
    [Array]::Copy($head,0,$stream,0,$head.Length)
    [Array]::Copy($contentBytes,0,$stream,$head.Length,$contentBytes.Length)
    [Array]::Copy($tail,0,$stream,$head.Length+$contentBytes.Length,$tail.Length)
    $contentObject = Add-Object $stream
    $definition = "<< /Type /Page /Parent $pagesObject 0 R /MediaBox [0 0 $($script:PW) $($script:PH)] /Resources << /Font << /F1 $fontRegular 0 R /F2 $fontBold 0 R >> >> /Contents $contentObject 0 R >>"
    [void]$script:PageObjects.Add((Add-Object (B $definition)))
}

function Title([string]$number,[string]$title) {
    Text $number 40 55 11 -Bold -color '0.059 0.42 0.38'
    Text $title 40 72 21 -Bold -color '0.071 0.22 0.4'
    Line 40 100 555 100 '0.42 0.59 0.78' 1.4
}

function Callout([string]$title,[string]$body,[double]$top,[string]$kind='blue',[double]$height=54) {
    $fill=if($kind-eq'green'){'0.89 0.96 0.93'}elseif($kind-eq'amber'){'1 0.94 0.86'}else{'0.91 0.94 1'}
    $accent=if($kind-eq'green'){'0.059 0.42 0.38'}elseif($kind-eq'amber'){'0.6 0.3 0'}else{'0.122 0.31 0.561'}
    Rect 40 $top 515 $height $fill; Rect 40 $top 4 $height $accent
    Text $title 52 ($top+9) 9.3 -Bold -color $accent
    [void](Wrapped $body 52 ($top+24) 490 8.5 10 -color '0.16 0.21 0.29' -maxLines 3)
}

function Table-Rows([array]$rows,[double]$top) {
    $x=@(40,73,188,255,445,555);$headers=@('Orden','Plugin','Fase','Descripción breve','Prerrequisito');$hh=25
    for($c=0;$c-lt5;$c++){Rect $x[$c] $top ($x[$c+1]-$x[$c]) $hh '0.122 0.31 0.561' '0.09 0.24 0.44';[void](Wrapped $headers[$c] ($x[$c]+3) ($top+7) ($x[$c+1]-$x[$c]-6) 7.2 8 -Bold -color '1 1 1' -maxLines 2)}
    $cursor=$top+$hh;$rowIndex=0
    foreach($row in $rows){
        $values=@($row.Order,$row.Plugin,$row.Phase,$row.Description,$row.Dependency);$sizes=@(7.1,6.8,6.8,7.0,6.8);$leads=@(8,7.8,7.8,8,7.8);$counts=@()
        for($c=0;$c-lt5;$c++){$counts+=@(Wrap ([string]$values[$c]) ($x[$c+1]-$x[$c]-6) $sizes[$c]).Count}
        $height=[Math]::Max(27,(($counts|Measure-Object -Maximum).Maximum*8)+8);$fill=if(($rowIndex%2)-eq0){'1 1 1'}else{'0.965 0.973 0.988'}
        for($c=0;$c-lt5;$c++){Rect $x[$c] $cursor ($x[$c+1]-$x[$c]) $height $fill '0.77 0.82 0.88';$bold=$c-eq0-or$c-eq1;$color=if($c-eq0){'0.122 0.31 0.561'}else{'0.12 0.16 0.23'};[void](Wrapped ([string]$values[$c]) ($x[$c]+3) ($cursor+5) ($x[$c+1]-$x[$c]-6) $sizes[$c] $leads[$c] -Bold:$bold -color $color)}
        $cursor+=$height;$rowIndex++
    }
    return $cursor
}

$erp1=@(
 @{Order='R0';Plugin='reference_data';Phase='Fundación';Description='Países, monedas, procedencia y políticas normativas por empresa.';Dependency='Kernel, seguridad y migraciones.'},
 @{Order='1';Plugin='business_partners';Phase='Fundaciones';Description='Clientes, proveedores, contactos, direcciones y definiciones empresariales.';Dependency='reference_data.'},
 @{Order='2';Plugin='commercial_catalog';Phase='Fundaciones';Description='Productos, servicios, unidades, impuestos, precios, variantes y definiciones comerciales.';Dependency='reference_data; independiente de Socios.'},
 @{Order='3';Plugin='inventory';Phase='Operación';Description='Depósitos, ubicaciones, existencias, movimientos, reservas y trazabilidad de stock.';Dependency='Identidades públicas del Catálogo.'},
 @{Order='4';Plugin='purchasing';Phase='Operación';Description='Solicitudes, órdenes de compra, recepciones y devoluciones a proveedores.';Dependency='Socios, Catálogo, Datos e Inventario.'},
 @{Order='5';Plugin='sales';Phase='Operación';Description='Presupuestos, pedidos, condiciones y compromisos de venta.';Dependency='Socios, Catálogo e Inventario.'},
 @{Order='6';Plugin='logistics';Phase='Operación';Description='Preparación, despacho, vehículos, rutas, transporte y entrega.';Dependency='Pedidos, reservas y participantes.'},
 @{Order='7';Plugin='vehicle_telemetry';Phase='Operación';Description='Dispositivos, posiciones, recorridos, sensores, geocercas, alertas y seguimiento.';Dependency='Identidad pública de vehículo de Logística.'}
)
$erp2=@(
 @{Order='8';Plugin='commercial_documents';Phase='Documentos';Description='Factura, notas, remisión, snapshots, impuestos, pagos, totales y emisión masiva.';Dependency='Socios, Catálogo, Ventas y Logística.'},
 @{Order='9';Plugin='recurring_billing';Phase='Servicios';Description='Planes, suscripciones, prorrateo, consumo medido y corridas de cargos.';Dependency='Publica candidatos a Documentos.'},
 @{Order='10';Plugin='sifen';Phase='Fiscal';Description='Firma, transmisión, respuestas y eventos fiscales SIFEN versionados.';Dependency='Proyección pública de Documentos.'},
 @{Order='11';Plugin='treasury';Phase='Finanzas';Description='Cajas, bancos, cobros, pagos, movimientos y conciliaciones.';Dependency='Referencias de operaciones y documentos.'},
 @{Order='12';Plugin='point_of_sale';Phase='Canal';Description='Terminales, sesiones, carrito y checkout online u offline, durable e idempotente.';Dependency='Catálogo, Inventario, Ventas, Documentos y Tesorería.'},
 @{Order='13';Plugin='fuel_station';Phase='Vertical';Description='Tanques, surtidores, picos, turnos, lecturas y conciliación de inventario húmedo.';Dependency='Catálogo, Inventario y POS opcional.'},
 @{Order='14';Plugin='accounts_receivable';Phase='Finanzas';Description='Deuda comercial de clientes, cuotas, vencimientos, imputaciones y cobranzas.';Dependency='Documentos y Tesorería.'},
 @{Order='15';Plugin='accounts_payable';Phase='Finanzas';Description='Obligaciones con proveedores, vencimientos, programación e imputación de pagos.';Dependency='Compras, Documentos y Tesorería.'},
 @{Order='16';Plugin='accounting';Phase='Finanzas';Description='Asientos, períodos, mayores, cierres y proyecciones contables.';Dependency='Consume eventos; la operación no depende de ella.'},
 @{Order='17';Plugin='human_resources';Phase='Personas';Description='Legajo, relación laboral, organización, ausencias, novedades y tiempo.';Dependency='Identidad y referencias públicas.'},
 @{Order='18';Plugin='payroll';Phase='Personas';Description='Conceptos, períodos, liquidaciones, cálculos y recibos de nómina neutrales.';Dependency='API pública de Recursos Humanos.'},
 @{Order='19';Plugin='payroll_paraguay';Phase='País';Description='Reglas, cálculos y artefactos IPS/MTESS versionados para Paraguay.';Dependency='Nómina y fuentes oficiales vigentes.'}
)

$fontRegular=Add-Object (B '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>')
$fontBold=Add-Object (B '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>')
$pagesObject=Add-Object (B '<< >>')

# 1. Portada
Start-Page
Rect 0 0 $script:PW 12 '0.122 0.31 0.561'
Text 'SMART ERP | GUÍA 00' 48 145 10 -Bold -color '0.122 0.31 0.561'
[void](Wrapped 'Plugins y orden de construcción' 48 182 490 34 38 -Bold -color '0.071 0.22 0.4')
Rect 48 280 165 8 '0.059 0.42 0.38'
[void](Wrapped 'Catálogo completo de capacidades reutilizables, secuencia ERP, familias paralelas y plugins transversales.' 48 316 475 15 20 -color '0.2 0.25 0.34')
$cx=@(48,219,390);$cv=@('34','1-19','2026-08-15');$ct=@('plugins reutilizables','secuencia principal ERP','baseline documentado')
for($i=0;$i-lt3;$i++){Rect $cx[$i] 430 153 80 '0.91 0.94 1' '0.69 0.78 0.91';Text $cv[$i] ($cx[$i]+14) 448 22 -Bold -color '0.122 0.31 0.561';[void](Wrapped $ct[$i] ($cx[$i]+14) 478 125 8.5 10 -color '0.34 0.38 0.47')}
Callout 'Propósito' 'Mostrar qué se construye, en qué orden y para qué sirve cada plugin. La guía no convierte capacidades planificadas en implementadas.' 570 'blue' 62
Text 'Fuentes canónicas: ADR y Markdown versionados del proyecto.' 48 750 8 -color '0.4 0.44 0.52'
Finish-Page 1 $pagesObject $fontRegular $fontBold -Cover

# 2. Panorama
Start-Page 'PANORAMA';Title '01' 'Cómo leer el orden'
[void](Wrapped 'Smart ERP no tiene una única fila de 34 posiciones. El catálogo usa una secuencia ERP principal, familias y verticales con perfiles propios y dos capacidades transversales.' 40 118 515 10.5 14 -color '0.2 0.25 0.34')
$metrics=@(@('20','R0 más 19 ERP'),@('3','operaciones del proveedor'),@('9','verticales: C1-C6, F1-F2 e Inmobiliaria'),@('2','plugins transversales'))
for($i=0;$i-lt4;$i++){$mx=40+($i*131);Rect $mx 167 121 62 '0.965 0.973 0.988' '0.77 0.82 0.88';Text $metrics[$i][0] ($mx+10) 181 18 -Bold -color '0.122 0.31 0.561';[void](Wrapped $metrics[$i][1] ($mx+10) 207 101 7.8 9 -color '0.34 0.38 0.47' -maxLines 2)}
$tracks=@(
 @('RUTA ERP','R0 Datos -> 1 Socios -> 2 Catálogo -> 3 Inventario -> 4 Compras -> 5-19 Resto ERP -> Personalización','0.122 0.31 0.561'),
 @('PROVEEDOR','Releases y Soporte central pueden avanzar por prioridad -> Conector seguro opcional después','0.41 0.29 0.53'),
 @('COOPERATIVA','C1 Membresía -> C2 Gobierno -> C3 LA/FT -> C4 Ahorros -> C5 Crédito -> C6 Regulación PY','0.55 0.37 0.07'),
 @('VERTICALES','F1 Mantenimiento -> F2 Taller comercial | real_estate: RE-00 y decisiones aprobadas antes del código','0.19 0.37 0.47'),
 @('TRANSVERSALES','Migración: según destinos y proyecto | BPM: después de Compras compuesta y eventos reales','0.059 0.42 0.38'))
$top=246
foreach($track in $tracks){Rect 40 $top 105 54 $track[2];[void](Wrapped $track[0] 50 ($top+15) 85 8.3 9.5 -Bold -color '1 1 1' -maxLines 2);Rect 145 $top 410 54 '0.965 0.973 0.988' '0.77 0.82 0.88';[void](Wrapped $track[1] 157 ($top+11) 385 8 10 -color '0.12 0.16 0.23' -maxLines 4);$top+=62}
Callout 'Importante' 'R0, 1-19, C1-C6 y F1-F2 son órdenes aprobados. Proveedor, transversales y real_estate no reciben número ERP; su momento depende de prerrequisitos y prioridad explícita.' 564 'amber' 58
Text 'Reglas comunes' 40 638 13 -Bold -color '0.122 0.31 0.561'
$rules=@('Propiedad separada de código, permisos, esquema y migraciones.','Sin relaciones JPA ni lectura directa de tablas privadas ajenas.','Activación por empresa; desactivar no elimina datos.','Personalización empresarial al final, sobre contratos estabilizados.');$rt=664
foreach($rule in $rules){Rect 43 ($rt+3) 5 5 '0.059 0.42 0.38';[void](Wrapped $rule 55 $rt 490 8.7 10 -color '0.16 0.21 0.29');$rt+=25}
Finish-Page 2 $pagesObject $fontRegular $fontBold

# 3. ERP R0-7
Start-Page 'RUTA ERP';Title '02' 'Fundación y operación inicial'
[void](Wrapped 'La ruta estabiliza maestros y existencias antes de sumar operaciones que los consumen mediante contratos públicos.' 40 116 515 9.2 12 -color '0.2 0.25 0.34')
$end=Table-Rows $erp1 150
Callout 'Punto actual' 'R0 y órdenes 1-3 están implementados. Compras, orden 4, tiene dominio, persistencia, aplicación, interfaz y pruebas automatizadas verdes; falta composición y gate navegable.' ($end+18) 'green' 62
Finish-Page 3 $pagesObject $fontRegular $fontBold

# 4. ERP 8-19
Start-Page 'RUTA ERP';Title '03' 'Documentos, finanzas y personas'
[void](Wrapped 'La segunda mitad construye el documento canónico antes de la integración fiscal y separa dinero, deuda, contabilidad, personas y reglas nacionales.' 40 116 515 9.2 12 -color '0.2 0.25 0.34')
[void](Table-Rows $erp2 150)
Finish-Page 4 $pagesObject $fontRegular $fontBold

# 5. Familias
Start-Page 'FAMILIAS';Title '04' 'Familias con secuencia propia'
Text 'Operaciones del proveedor' 40 120 14 -Bold -color '0.41 0.29 0.53'
[void](Wrapped 'No recibe números 20-22 y usa perfiles distintos del ERP del cliente.' 40 143 515 8.8 11 -color '0.2 0.25 0.34')
$provider=@(
 @('release_management','Defectos, mejoras, candidatos, compatibilidad, gates, notas y publicación de releases. Puede comenzar sin Soporte.'),
 @('customer_support','Cobertura, instalaciones, tickets, SLA, conversaciones, diagnósticos y resolución. Requiere Socios y puede integrar Releases.'),
 @('support_connector','Conector técnico opcional del cliente: HTTPS saliente, consentimiento y diagnósticos sanitizados. Va después del protocolo central.'))
$top=172
foreach($item in $provider){Rect 40 $top 515 56 '0.965 0.945 0.988' '0.79 0.72 0.87';Text $item[0] 52 ($top+10) 8.8 -Bold -color '0.35 0.24 0.47';[void](Wrapped $item[1] 190 ($top+9) 350 8.1 10 -color '0.16 0.21 0.29' -maxLines 4);$top+=65}
Callout 'Dirección por dependencias' 'Releases y Soporte central pueden avanzar en paralelo por prioridad; el Conector seguro va después. Esto no es numeración ERP.' 376 'amber' 52
Text 'Cooperativa de ahorro y crédito' 40 452 14 -Bold -color '0.55 0.37 0.07'
[void](Wrapped 'Orden interno C1-C6. Reutiliza Socios, Tesorería y Contabilidad cuando corresponda.' 40 475 515 8.8 11 -color '0.2 0.25 0.34')
$coop=@(
 @('C1','cooperative_membership','Socios, admisión, estado, aportes y desvinculación.'),
 @('C2','cooperative_governance','Asambleas, órganos, mandatos, votaciones y decisiones.'),
 @('C3','aml_compliance','Debida diligencia, riesgo, alertas y casos LA/FT.'),
 @('C4','cooperative_savings','Productos, cuentas, submayor, intereses y restricciones.'),
 @('C5','cooperative_credit','Solicitud, aprobación, cartera, garantías, mora y cobranza.'),
 @('C6','cooperative_regulatory_paraguay','Reglas y artefactos INCOOP/SEPRELAD versionados.'))
$top=507
for($i=0;$i-lt$coop.Count;$i++){
 $col=$i%2;$row=[Math]::Floor($i/2);$x=40+($col*260);$y=$top+($row*82)
 Rect $x $y 250 72 '1 0.97 0.91' '0.85 0.74 0.54';Text $coop[$i][0] ($x+10) ($y+9) 9 -Bold -color '0.55 0.37 0.07'
 [void](Wrapped $coop[$i][1] ($x+35) ($y+9) 203 7.5 9 -Bold -color '0.29 0.2 0.07' -maxLines 2)
 [void](Wrapped $coop[$i][2] ($x+10) ($y+32) 228 7.7 9 -color '0.2 0.25 0.34' -maxLines 4)
}
Finish-Page 5 $pagesObject $fontRegular $fontBold

# 6. Familia Flota
Start-Page 'FAMILIA FLOTA';Title '05' 'Mantenimiento y taller automotriz'
[void](Wrapped 'La familia separa la ejecución técnica del mantenimiento de la recepción, autorización y venta de servicios a vehículos de clientes.' 40 116 515 9.2 12 -color '0.2 0.25 0.34')

Rect 40 154 515 205 '0.9 0.95 0.98' '0.58 0.72 0.8';Rect 40 154 515 38 '0.19 0.37 0.47'
Text 'F1  fleet_maintenance' 52 165 12 -Bold -color '1 1 1';Text 'PLANIFICADO' 456 167 8 -Bold -color '1 1 1'
Text 'Qué hace' 52 207 9 -Bold -color '0.19 0.37 0.47'
[void](Wrapped 'Planes preventivos, solicitudes, defectos, checklists, órdenes técnicas, personal, repuestos requeridos, terceros, costos e indisponibilidad.' 52 224 486 8.6 10.5 -color '0.16 0.21 0.29' -maxLines 4)
Text 'Límites' 52 274 9 -Bold -color '0.19 0.37 0.47'
[void](Wrapped 'Logística posee VehicleId; Telemetría aporta lecturas opcionales; Inventario conserva stock; Compras conserva reposición/terceros y RR. HH. conserva empleados.' 52 291 486 8.6 10.5 -color '0.16 0.21 0.29' -maxLines 4)
Text 'Inicio' 52 333 9 -Bold -color '0.19 0.37 0.47';[void](Wrapped 'Después de estabilizar logistics-api. Opera con Telemetría, BPM y F2 ausentes o inactivos.' 100 333 438 8.6 10.5 -color '0.16 0.21 0.29' -maxLines 2)

Rect 40 378 515 183 '1 0.96 0.9' '0.82 0.68 0.43';Rect 40 378 515 38 '0.46 0.32 0.1'
Text 'F2  automotive_workshop' 52 389 12 -Bold -color '1 1 1';Text 'PLANIFICADO' 456 391 8 -Bold -color '1 1 1'
Text 'Qué hace' 52 431 9 -Bold -color '0.46 0.32 0.1'
[void](Wrapped 'Recepción y condición del vehículo del cliente, autorización de diagnóstico/reparación, comunicación, seguimiento y entrega.' 52 448 486 8.6 10.5 -color '0.16 0.21 0.29' -maxLines 3)
Text 'Límites' 52 489 9 -Bold -color '0.46 0.32 0.1'
[void](Wrapped 'Referencia la única OT de F1, presupuesto/pedido de Ventas y factura/notas de Documentos. No posee precio, stock, pago, deuda o asiento.' 52 506 486 8.6 10.5 -color '0.16 0.21 0.29' -maxLines 3)
Text 'Inicio' 52 543 9 -Bold -color '0.46 0.32 0.1';[void](Wrapped 'Después de F1, sales y commercial_documents.' 100 543 438 8.6 10.5 -color '0.16 0.21 0.29' -maxLines 2)

Callout 'Decisión aprobada' 'Producto aceptó FM-D01 a FM-D12 y AW-D01 a AW-D10 sin cambios el 2026-08-12. Catálogo 31 -> 33; ERP 1-19 y J11-S9-06 no cambian.' 582 'green' 64
Text 'Orden y contratos' 40 669 13 -Bold -color '0.122 0.31 0.561'
Rect 40 695 150 54 '0.9 0.95 0.98' '0.58 0.72 0.8';Text 'VehicleId estable' 52 706 9 -Bold -color '0.19 0.37 0.47';Text 'Logística' 52 726 8 -color '0.34 0.38 0.47'
Text '->' 204 711 15 -Bold -color '0.122 0.31 0.561'
Rect 232 695 130 54 '0.9 0.95 0.98' '0.58 0.72 0.8';Text 'F1 técnico' 244 706 9 -Bold -color '0.19 0.37 0.47';Text 'planes y OT' 244 726 8 -color '0.34 0.38 0.47'
Text '->' 376 711 15 -Bold -color '0.122 0.31 0.561'
Rect 404 695 151 54 '1 0.96 0.9' '0.82 0.68 0.43';Text 'F2 comercial' 416 706 9 -Bold -color '0.46 0.32 0.1';Text 'Ventas + Documentos' 416 726 8 -color '0.34 0.38 0.47'
Finish-Page 6 $pagesObject $fontRegular $fontBold

# 7. Vertical inmobiliario
Start-Page 'VERTICAL INMOBILIARIO';Title '06' 'Gestión inmobiliaria'
[void](Wrapped 'real_estate queda planificado como plugin funcional vertical, reutilizable y opcional por empresa. No renumera ERP 1-19 y no está implementado.' 40 116 515 9.2 12 -color '0.2 0.25 0.34')

Rect 40 158 515 154 '0.96 0.92 0.96' '0.76 0.62 0.75';Rect 40 158 515 38 '0.42 0.25 0.4'
Text 'real_estate' 52 169 12 -Bold -color '1 1 1';Text 'PLANIFICADO' 456 171 8 -Bold -color '1 1 1'
Text 'Alcance candidato' 52 210 9 -Bold -color '0.42 0.25 0.4'
[void](Wrapped 'Proyectos o desarrollos, inmuebles, fracciones, lotes, edificios y unidades, catastro, mejoras, documentos, estados y disponibilidad. RE-00 confirmará si forman un dominio o una familia.' 52 228 486 8.6 10.5 -color '0.16 0.21 0.29' -maxLines 5)
Text 'Inicio' 52 283 9 -Bold -color '0.42 0.25 0.4';[void](Wrapped 'Después de RE-00, RE-D01 a RE-D12 aprobadas y APIs públicas estables del perfil elegido.' 100 283 438 8.6 10.5 -color '0.16 0.21 0.29' -maxLines 2)

Text 'Propietarios que no se trasladan' 40 337 13 -Bold -color '0.122 0.31 0.561'
$realEstateLimits=@(
 @('Socios','personas, organizaciones, propietarios y clientes'),
 @('Inventario','bienes físicos, depósitos, saldos y movimientos'),
 @('Ventas y Documentos','presupuestos, pedidos, facturas y notas'),
 @('Finanzas','dinero, deuda, cobranzas y asientos'))
for($i=0;$i-lt$realEstateLimits.Count;$i++){
 $col=$i%2;$row=[Math]::Floor($i/2);$x=40+($col*260);$y=365+($row*82)
 Rect $x $y 250 72 '0.965 0.973 0.988' '0.77 0.82 0.88';Text $realEstateLimits[$i][0] ($x+10) ($y+11) 8.8 -Bold -color '0.122 0.31 0.561'
 [void](Wrapped $realEstateLimits[$i][1] ($x+10) ($y+32) 228 8 9.5 -color '0.16 0.21 0.29' -maxLines 3)
}

Callout 'Fuente legado de solo lectura' 'C:\cosme\mega\miaterra, rama miaterra_master, raíz fuente/tag. RE-00 debe registrar el commit completo analizado; no se copian clases, XHTML, SQL ni dependencias javax.' 545 'blue' 68
Text 'Secuencia de decisión' 40 632 13 -Bold -color '0.122 0.31 0.561'
$realEstateSteps=@(@('1','Caracterizar','perfiles, ciclos y datos'),@('2','Decidir','frontera y propietarios'),@('3','Implementar','sólo con iteración autorizada'))
for($i=0;$i-lt3;$i++){$x=40+($i*174);Rect $x 660 164 62 '0.96 0.92 0.96' '0.76 0.62 0.75';Text $realEstateSteps[$i][0] ($x+10) 672 13 -Bold -color '0.42 0.25 0.4';Text $realEstateSteps[$i][1] ($x+34) 673 8.5 -Bold -color '0.42 0.25 0.4';[void](Wrapped $realEstateSteps[$i][2] ($x+10) 696 142 7.7 9 -color '0.16 0.21 0.29' -maxLines 2)}
Callout 'Incorporación al plan' 'ADR-0048 eleva el catálogo 33 -> 34. Sprint 10 y ERP 1-19 no cambian; no se agrega código ejecutable.' 735 'green' 48
Finish-Page 7 $pagesObject $fontRegular $fontBold

# 8. Transversales
Start-Page 'TRANSVERSALES';Title '07' 'Migración, BPM y personalización'
$cards=@(
 @('legacy_migration','Planificado','Inventaría legados, gobierna paquetes y mapeos, ensayos, cuarentena, importación idempotente, conciliación, corte y rollback. Oracle Forms & Reports es el primer perfil.','Descubrimiento cuando exista una oportunidad; adaptadores después de estabilizar APIs destino; obligatorio antes de vender migración Oracle.','0.059 0.42 0.38','0.89 0.96 0.93'),
 @('business_process_management','Planificado','Procesos versionados por empresa, tareas humanas, responsables, plazos, temporizadores, incidentes, SLA, métricas y cuellos de botella.','Después de Compras compuesta y eventos reales; BPM-D01 a BPM-D12 y spike de motor antes del código; piloto con solicitudes.','0.122 0.31 0.561','0.91 0.94 1'),
 @('<empresa>_customization','Uno por empresa','Personalización exclusiva sobre contratos y pantallas estabilizados. No integra el conteo de 34 reutilizables.','Se construye al final para cada empresa; nunca se comparte ni puede relajar seguridad o reglas del servidor.','0.27 0.36 0.46','0.93 0.95 0.97'))
$top=122
foreach($card in $cards){
 Rect 40 $top 515 174 $card[5] '0.7 0.76 0.83';Rect 40 $top 515 38 $card[4]
 Text $card[0] 52 ($top+11) 11 -Bold -color '1 1 1';Text $card[1] 455 ($top+12) 8 -Bold -color '1 1 1'
 Text 'Qué hace' 52 ($top+53) 9 -Bold -color $card[4];[void](Wrapped $card[2] 52 ($top+69) 486 8.5 10.5 -color '0.16 0.21 0.29' -maxLines 4)
 Text 'Cuándo se construye' 52 ($top+116) 9 -Bold -color $card[4];[void](Wrapped $card[3] 52 ($top+132) 486 8.5 10.5 -color '0.16 0.21 0.29' -maxLines 4)
 $top+=190
}
Callout 'Límite BPM' 'BPM coordina, pero el dominio decide. No ejecuta SQL, scripts o HTTP arbitrario ni reemplaza permisos, estados o invariantes.' 700 'amber' 54
Finish-Page 8 $pagesObject $fontRegular $fontBold

# 9. Estado
Start-Page 'ESTADO';Title '08' 'Estado actual y siguiente construcción'
Text 'Situación al 2026-08-15' 40 120 14 -Bold -color '0.122 0.31 0.561'
$status=@(
 @('R0','IMPLEMENTADO','Datos de referencia en el reactor y consumido por fundaciones.','0.89 0.96 0.93'),
 @('ERP 1-3','IMPLEMENTADOS','Socios, Catálogo e Inventario con demos oficiales y gates técnicos ejecutados.','0.89 0.96 0.93'),
 @('ERP 4','VALIDADO AUTO.','Compras está compuesta; su validación independiente continúa pendiente.','0.89 0.96 0.93'),
 @('SPRINT 10','EN CURSO','Floorplans operativos sobre Inventario y Compras antes de Ventas.','1 0.94 0.86'),
 @('ERP 5-19','PLANIFICADOS','Continúan desde Ventas según el orden aprobado.','0.965 0.945 0.988'),
 @('OTRAS RUTAS','PLANIFICADAS','Proveedor, Cooperativa, Migración, BPM, Flota e Inmobiliaria requieren prioridad propia.','0.965 0.945 0.988'))
$top=151
foreach($item in $status){Rect 40 $top 515 50 $item[3] '0.77 0.82 0.88';Text $item[0] 51 ($top+9) 8.5 -Bold -color '0.122 0.31 0.561';Text $item[1] 51 ($top+27) 7 -Bold -color '0.34 0.38 0.47';[void](Wrapped $item[2] 145 ($top+10) 395 8.1 9.5 -color '0.16 0.21 0.29' -maxLines 3);$top+=58}
Callout 'Siguiente paso autorizado: Sprint 10' 'Completar el gate de bandejas, editores transaccionales y operaciones guiadas sobre Inventario y Compras.' 510 'green' 58
Callout 'Después' 'Continuar con sales, orden ERP 5, salvo una nueva decisión explícita de producto.' 580 'blue' 54
Callout 'No confundir planificación con prioridad' 'real_estate requiere RE-00 y decisiones aprobadas; su incorporación al catálogo no lo convierte en el siguiente Sprint.' 646 'amber' 58
Text 'Fuentes canónicas' 40 718 12 -Bold -color '0.122 0.31 0.561'
[void](Wrapped 'ADR-0011, ADR-0027, ADR-0030, ADR-0032, ADR-0033, ADR-0034, ADR-0036, ADR-0037, ADR-0038, ADR-0040, ADR-0045, ADR-0046, ADR-0048 y la Épica de roadmap de plugins productivos.' 40 739 515 8 10 -color '0.34 0.38 0.47' -maxLines 4)
Finish-Page 9 $pagesObject $fontRegular $fontBold

$kids=($script:PageObjects|ForEach-Object{"$_ 0 R"})-join' '
Set-Object $pagesObject (B ("<< /Type /Pages /Kids [{0}] /Count {1} >>" -f $kids,$script:PageObjects.Count))
$catalogObject=Add-Object (B ("<< /Type /Catalog /Pages {0} 0 R /PageMode /UseNone >>" -f $pagesObject))
$infoObject=Add-Object (B '<< /Title (Smart ERP - Plugins y orden de construcción) /Author (Smart ERP) /Subject (Catálogo de 34 plugins reutilizables y su orden de construcción) /Keywords (Smart ERP, plugins, roadmap) /Creator (PowerShell PDF generator) /CreationDate (D:20260815090000-04''00'') >>')

$memory=New-Object IO.MemoryStream;$header=B "%PDF-1.4`n%âãÏÓ`n";$memory.Write($header,0,$header.Length)
$offsets=New-Object 'System.Collections.Generic.List[long]';[void]$offsets.Add(0)
for($i=0;$i-lt$script:Objects.Count;$i++){
 [void]$offsets.Add($memory.Position);$head=B ("{0} 0 obj`n"-f($i+1));$memory.Write($head,0,$head.Length)
 $bytes=$script:Objects[$i];$memory.Write($bytes,0,$bytes.Length);$tail=B "`nendobj`n";$memory.Write($tail,0,$tail.Length)
}
$xrefOffset=$memory.Position;$xref=B ("xref`n0 {0}`n0000000000 65535 f `n"-f($script:Objects.Count+1));$memory.Write($xref,0,$xref.Length)
for($i=1;$i-lt$offsets.Count;$i++){$line=B (("{0:0000000000} 00000 n `n"-f$offsets[$i]));$memory.Write($line,0,$line.Length)}
$trailer=B ("trailer`n<< /Size {0} /Root {1} 0 R /Info {2} 0 R >>`nstartxref`n{3}`n%%EOF`n"-f($script:Objects.Count+1),$catalogObject,$infoObject,$xrefOffset);$memory.Write($trailer,0,$trailer.Length)
[IO.File]::WriteAllBytes($outputPath,$memory.ToArray());$memory.Dispose()
$pdf=Get-Item -LiteralPath $outputPath
if($pdf.Length-lt30000){throw "PDF anormalmente pequeño: $($pdf.Length) bytes."}
Write-Host ("PDF generado: {0} | páginas={1} | bytes={2}" -f $pdf.FullName, $script:PageObjects.Count, $pdf.Length)
