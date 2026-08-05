# ADR-0022 - Recorridos visuales de `commercial_catalog`

- Estado: Aceptado
- Fecha: 2026-07-30
- Historia: `J11-S7-05`

## Contexto

`commercial_catalog` posee dos agregados de trabajo distintos: artículos/servicios
y listas de precios. Mostrar búsqueda, alta, ficha, clasificaciones, unidades,
identificadores, precios y ciclo de vida en una única página repetiría el problema
de densidad y desorganización que motivó ADR-0018.

El renderer actual implementa el floorplan correcto, pero todavía contiene textos,
pestañas y validaciones de regiones específicos de `business_partners`. Duplicar
XHTML por plugin rompería la propiedad del shell definida por ADR-0017.

## Decisión

1. El plugin publica dos pantallas interactivas independientes:
   `commercial_catalog:items` en `/catalog` y
   `commercial_catalog:price_lists` en `/catalog/price-lists`.
2. Cada pantalla usa los modos `directory`, `create` y `detail` de ADR-0018. Nunca
   muestra simultáneamente el directorio, el alta y todas las operaciones.
3. Artículos y servicios comparten directorio y ficha. El tipo `PRODUCT` o
   `SERVICE` permanece estable y se presenta como filtro y dato de alta.
4. Las listas de precios tienen su propio directorio y ficha porque son agregados
   empresariales con moneda, política tributaria, redondeo, entradas y ciclo de
   vida propios.
5. El shell generaliza encabezados, navegación por secciones y lista adaptable.
   Continúa siendo dueño de XHTML, CSS, Material Design 3, accesibilidad y
   breakpoints; el plugin sólo aporta contratos e interacción neutrales.
6. Los textos, regiones y acciones aceptados siguen en un registro cerrado del
   shell. Un contrato, región, handler o acción desconocidos se rechazan.
7. Unidades, categorías, marcas y perfiles tributarios activos alimentan
   selectores. Su administración completa no se mezcla con estas pantallas de
   operación y requerirá un recorrido de configuración separado.
8. La autorización se revalida por acción: lectura, artículos, precios o
   definiciones. El modo, la pestaña, el recurso y la versión recibidos del
   navegador no conceden autoridad.
9. Los slots públicos `directory_extensions` y `detail_extensions` quedan
   disponibles en ambas pantallas para personalizaciones empresariales tipadas;
   no permiten inyectar XHTML, CSS, JavaScript o EL.

## Alternativas descartadas

- Una página única con todos los formularios: mezcla tareas, reduce densidad útil
  y obliga a desplazamiento vertical excesivo.
- Pestaña de precios dentro de la ficha del artículo: oculta que una lista cubre
  muchos artículos y tiene identidad, moneda y vigencia propias.
- XHTML propio dentro de `commercial-catalog`: dispersa el sistema de diseño y
  permite dependencias visuales no controladas.
- Administración de todas las definiciones en el alta del artículo: mezcla
  configuración empresarial con operación cotidiana y complica permisos.

## Consecuencias

- El menú fusionado mostrará dos entradas cuando el JAR esté presente, el plugin
  esté activo y el rol posea `commercial_catalog.view`.
- El shell deja de asumir nombres, pestañas y copias de socios comerciales y puede
  representar nuevos directorios declarados sin duplicar el floorplan.
- `plugin-api` conserva la versión `0.4.0`: no se agrega ningún tipo neutral nuevo.
- `J11-S7-06` seguirá siendo responsable de la composición física, migraciones,
  datos ficticios y candidata de demo ejecutable.

## Verificación

- contratos y descriptor de las dos pantallas;
- handlers con autorización exacta y resultados reales de aplicación;
- renderer cerrado, XHTML parseable y regresión de `business_partners`;
- navegación, alta, ficha, acciones y rechazos;
- Playwright en 375, 720 y 1280 px, límites 599/600 y 839/840, teclado, foco y
  ausencia de overflow horizontal una vez compuesta la candidata.

