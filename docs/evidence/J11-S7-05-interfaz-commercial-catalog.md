# Evidencia J11-S7-05 — Interfaz neutral de `commercial_catalog`

- Fecha: 2026-07-30
- Estado: corte de implementación verde; pendientes satisfechos posteriormente por J11-S7-06
- Historia: [J11-S7-05](../sprints/sprint-07/J11-S7-05-interfaz-commercial-catalog.md)
- ADR: [ADR-0022](../adr/0022-recorridos-visuales-commercial-catalog.md)
- Entorno: Windows, Java 21.0.11 y Maven Wrapper 3.9.16

## Resultado funcional

El descriptor de `commercial_catalog` aporta dos capacidades, dos menús protegidos
por `commercial_catalog.view` y dos pantallas interactivas:

1. `commercial_catalog:items` en `/catalog`;
2. `commercial_catalog:price_lists` en `/catalog/price-lists`.

Artículos/servicios permite buscar, registrar, abrir ficha, revisar datos, agregar
identificadores, clasificar, agregar conversiones de unidad, asignar perfil
tributario y cambiar el ciclo de vida. Listas permite buscar, registrar, abrir
ficha, renombrar, agregar/inactivar entradas y cambiar el ciclo de vida.

Los handlers vuelven a pedir la autorización actual de empresa y permiso exacto
para cada interacción. Las entradas se convierten a tipos de dominio antes de
autorizar una mutación; después de mutar se vuelve a consultar el recurso con
`commercial_catalog.view`. No se conserva una autorización en sesión ni se acepta
`companyId` del navegador.

## Shell y arquitectura visual

El shell dejó de fijar pestañas, títulos y secciones de `business_partners`. Un
registro cerrado define la presentación de cada contrato, sus modos de directorio,
alta y ficha, pestañas permitidas y textos. El XHTML sigue siendo único y propiedad
del shell; los plugins no aportan XHTML, CSS, JavaScript ni EL.

Las dos fichas de catálogo usan secciones propias:

- artículos: general, identificadores, clasificación, unidades, impuestos y ciclo
  de vida;
- listas: general, entradas y ciclo de vida.

El floorplan conserva la adaptación de tabla a lista, formularios compactos,
labels, avisos y foco del baseline Material Design 3. La verificación efectiva en
375, 599, 600, 720, 839, 840 y 1280 px requiere la aplicación desplegada y se
ejecutará en J11-S7-06.

## Pruebas focalizadas

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk\jdk-21.0.11+10').Path
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -B -pl plugins/commercial-catalog -am test `
  '-Dtest=CommercialCatalogItemScreenHandlerTest,CommercialCatalogPriceListScreenHandlerTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false'
```

Resultado: 8/8 pruebas verdes. Cubren carga autorizada, alta, versión optimista,
mutaciones de artículos/precios y rechazo de entrada inválida sin tocar el caso de
uso.

```powershell
.\mvnw.cmd -B -pl web-shell -am test `
  '-Dtest=BusinessPartnerScreenRendererTest,CommercialCatalogScreenRendererTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false'
```

Resultado: 4/4 pruebas verdes. Validan las seis pestañas de artículos, las tres de
listas, las rutas independientes, acciones y la regresión de socios comerciales.

## Módulos completos y arquitectura

```powershell
.\mvnw.cmd -B -pl plugins/commercial-catalog,web-shell -am test
.\mvnw.cmd -B -pl tests/architecture-tests -am test
```

Resultados:

- `commercial-catalog`: 43/43 pruebas verdes;
- `web-shell`: 19/19 pruebas verdes;
- arquitectura: 20/20 pruebas verdes;
- 18/18 módulos del gate arquitectónico construidos.

Las 16 reglas ArchUnit conservaron la independencia de API, dominio, aplicación y
plugins; las cuatro pruebas restantes verificaron composición física y referencia.

## Reactor integral

```powershell
.\mvnw.cmd -B verify
```

Resultado: 22/22 módulos y `BUILD SUCCESS`. Se contabilizaron 80 reportes y 301
pruebas, con cero fallos, errores u omisiones. El WAR base fue empaquetado sin
incorporar aún `commercial_catalog`, conforme al alcance de J11-S7-05.

## Gate documental y de higiene

El escaneo estricto validó UTF-8, marcadores de texto dañado y enlaces locales de
todos los Markdown mantenidos. Además, el XHTML se abrió como XML y se buscaron
`javax.*` o recursos visuales dentro del plugin:

```text
MARKDOWN_FILES=194
BAD_FILES=0
LOCAL_LINKS=701
BROKEN_LINKS=0
XHTML_ROOT=html
XHTML_XML=OK
PROHIBITED_MATCHES=0
```

## Incidencias detectadas y corregidas

1. La terminal heredó Java 8. Maven Enforcer detuvo el build antes de compilar; se
   fijó el JDK 21 validado de `.tools` y se repitió la prueba.
2. La prueba negativa de entrada de precio detectó que la autorización de gestión
   se evaluaba antes de construir por completo el comando. Se tipó y validó el
   comando primero, se mantuvo la autorización inmediatamente antes del caso de uso
   y la misma prueba quedó verde.
3. El primer comando del gate arquitectónico usó un selector inexistente. No se
   ejecutaron pruebas; se corrigió a `tests/architecture-tests` y el gate real
   quedó 20/20 verde.

No se omitió, relajó ni desactivó ninguna prueba. Cada fallo detuvo el avance hasta
corregir su causa y repetir el alcance pertinente.

## Archivos principales

- contrato visual: `CommercialCatalogScreenContract` y descriptor del plugin;
- interacción: `CommercialCatalogItemScreenHandler` y
  `CommercialCatalogPriceListScreenHandler`;
- shell: registro/presentación genéricos, bean de navegación, catálogo de textos y
  `view.xhtml` reutilizable;
- pruebas: handlers, descriptor, renderer de catálogo y regresión de socios;
- decisión y documentación: ADR-0022, contrato del plugin, historia, guía candidata
  `1.0-rc41` y esta evidencia.

El workspace no contiene metadatos `.git`; la revisión se realizó por inventario,
búsquedas, compilación, pruebas y gates reproducibles.

## Continuidad satisfecha

Esta evidencia conserva el estado histórico de J11-S7-05. J11-S7-06 agregó después
la composición física única, migración, activación empresarial, fixture controlado,
despliegue, capturas y validación visual real. Su
[evidencia](J11-S7-06-integracion-composicion-commercial-catalog.md) cierra los
pendientes de interfaz. El PDF y la demo oficial continúan reservados al cierre de
Sprint 7; la validación independiente transversal de la guía sigue pendiente.
