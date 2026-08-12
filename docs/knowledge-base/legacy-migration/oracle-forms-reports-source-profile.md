# Perfil de origen - Oracle Forms & Reports

- Estado: planificado; no implementado
- Fecha: 2026-08-11
- Capacidad consumidora: `legacy_migration`
- Fuente: documentación oficial de Oracle
- Datos de cliente analizados: ninguno

## Propósito

Definir qué artefactos y evidencias debe entregar un sistema Oracle Forms &
Reports para que Smart ERP pueda inventariar comportamiento, preparar migración
de datos y demostrar cobertura sin copiar automáticamente la arquitectura del
legado.

Este perfil no promete convertir un formulario Oracle en una pantalla Jakarta
Faces ni un RDF en un reporte productivo equivalente. Convierte artefactos a un
formato inspeccionable, produce inventario y trazabilidad, y alimenta requisitos,
casos de uso, mapeos y pruebas de caracterización.

## Herramientas Oracle observadas

Oracle documenta:

- `Forms2XML` para convertir módulos de formulario `.fmb`, menús `.mmb` y
  bibliotecas de objetos `.olb` a XML;
- Forms Migration Assistant para revisar/actualizar módulos y bibliotecas, emitir
  advertencias y respetar dependencias entre `.olb`, `.pll`, `.mmb` y `.fmb`;
- `rwconverter` para transformar definiciones de Oracle Reports, incluida la
  conversión de `.rdf` a XML y el tratamiento previo de bibliotecas `.pll`
  adjuntas.

Referencias oficiales:

- [Forms2XML: conversión de FormModules, ObjectLibraries y MenuModules](https://docs.oracle.com/en/database/oracle/application-express/20.1/aemig/Converting_FormModules_ObjectLibraries_MenuModules_to_XML.html)
- [Oracle Forms Migration Assistant](https://docs.oracle.com/en/middleware/developer-tools/forms/12.2.1.19/upgrade-forms/using-oracle-forms-migration-assistant.html)
- [`rwconverter` y formatos de Oracle Reports](https://docs.oracle.com/html/E24479_01/pbr_cla002.htm)
- [Uso de XML en Oracle Reports](https://docs.oracle.com/middleware/12212/formsandreports/use-reports/pbr_xml004.htm)

Las versiones exactas, licencias, compatibilidad con el legado y checksums se
registrarán por proyecto. Smart ERP no redistribuirá herramientas o drivers de
Oracle sin autorización y licencia aplicables.

## Paquete de descubrimiento esperado

| Grupo | Artefactos de entrada | Salida controlada |
|---|---|---|
| Forms | `.fmb`, `.mmb`, `.olb` | XML producido por `Forms2XML`, log y checksum |
| bibliotecas | `.pll`, exportación `.pld` o fuente autorizada | inventario de unidades PL/SQL, dependencias y checksum |
| Reports | `.rdf`, `.rex`, `.pll` asociadas | XML producido por `rwconverter`, log, parámetros y checksum |
| base Oracle | DDL/exportación de diccionario autorizada | tablas, vistas, sinónimos, secuencias, restricciones, triggers, paquetes, funciones, procedimientos y grants relevantes |
| datos | exportaciones CSV/JSON/NDJSON o extracción JDBC de solo lectura | manifiesto, lotes, conteos, charset, zona horaria, claves y checksums |
| evidencias | capturas, recorridos, PDFs, XLS/CSV y casos representativos | catálogo de pantallas/reportes y especímenes de aceptación |
| operación | parámetros, jobs, directorios, integraciones y variables no secretas | inventario de dependencias y riesgos |

Los binarios originales y exportaciones con datos reales no se versionan en Git.
Se almacenan en un workspace cifrado y controlado del proyecto de migración, con
retención y eliminación aprobadas.

## Inventario de Forms

Como mínimo se registrará:

- módulo, versión, checksum, idioma y dependencias;
- menús, ventanas, canvases y navegación;
- bloques de datos/control, ítems, propiedades y relaciones master-detail;
- LOV, record groups, listas y fuentes SQL;
- triggers por formulario, bloque e ítem;
- program units, paquetes y bibliotecas adjuntas;
- parámetros, variables globales y valores de sistema utilizados;
- validaciones, mensajes, alertas, timers y built-ins;
- llamadas a reportes, archivos, host, Java, WebUtil u otras integraciones;
- tablas/vistas leídas o escritas y procedimientos ejecutados;
- permisos inferidos, estados y transacciones observadas;
- objetos obsoletos o conversiones que la herramienta no pudo resolver.

Cada trigger o unidad PL/SQL se clasifica por comportamiento: validación,
cálculo, persistencia, autorización, navegación, integración, reporte o utilidad.
La clasificación no convierte código automáticamente; genera backlog y casos de
caracterización.

## Inventario de Reports

Como mínimo se registrará:

- reporte, versión, checksum y formato de origen;
- parámetros, valores predeterminados, bind y lexical parameters;
- consultas, grupos, columnas, enlaces y fuentes externas;
- fórmulas, summaries, placeholders y program units;
- triggers de reporte y llamadas a paquetes;
- layout, secciones, campos, formatos, condiciones y páginas;
- fuentes/tipografías, imágenes, códigos de barras y recursos externos;
- destinos y formatos usados: pantalla, impresora, PDF, texto, hoja de cálculo u
  otros observados;
- formulario o proceso que invoca el reporte;
- datos sensibles, filtros empresariales y reglas de autorización;
- espécimen de salida y conjunto de datos ficticio para comparación.

El reemplazo productivo de un reporte pertenece al plugin dueño de su información
o a una capacidad de reporting futura. `legacy_migration` mantiene inventario,
trazabilidad, parámetros y evidencia de paridad; no se convierte en un generador
universal de reportes operativos.

## Descubrimiento de datos Oracle

La extracción debe registrar, por esquema autorizado:

- versión y edición declarada por el cliente;
- charset, NLS, zona horaria y formatos relevantes;
- tablas, columnas, tipos, nulabilidad, defaults y comentarios;
- PK, UK, FK, checks, índices, particiones y secuencias;
- vistas/materialized views y dependencias;
- triggers y efectos laterales;
- paquetes, funciones y procedimientos invocados por Forms/Reports;
- sinónimos, database links e integraciones externas;
- conteos, rangos, duplicados, nulos, huérfanos y distribuciones;
- datos LOB, archivos externos y volúmenes;
- claves naturales y reglas necesarias para resolver IDs públicos en Smart ERP.

La base origen permanece en solo lectura. No se deshabilitan triggers, no se crea
infraestructura de cola, no se cambia NLS y no se ejecuta DDL para facilitar la
migración.

## Dos modos de extracción

### Paquete portable recomendado

El cliente o un runner controlado produce exportaciones con:

- manifiesto versionado;
- herramienta y versión;
- fuente, corte temporal y criterio de selección;
- charset, zona horaria y separadores;
- archivos, tamaño y SHA-256;
- conteos por entidad/lote;
- clasificación de datos y responsable;
- firma o aprobación cuando corresponda.

Este modo evita mantener un driver Oracle y credenciales dentro del WAR.

### Conector Oracle de solo lectura opcional

Sólo se habilita si el volumen o los deltas justifican conexión directa. El runner
externo usa un usuario de mínimo privilegio, secretos inyectados, TLS/wallet según
el entorno, límites de consulta, checkpoints y una versión del driver aportada
bajo licencia. El driver y las herramientas Oracle no se incorporan
silenciosamente a la distribución de Smart ERP.

## Resultados del descubrimiento

El perfil debe producir:

1. catálogo de artefactos y checksums;
2. grafo de dependencias Forms/Reports/PLSQL/base;
3. matriz pantalla/trigger/tabla/operación;
4. matriz reporte/parámetro/consulta/salida;
5. inventario de reglas y cálculos;
6. datos sensibles y riesgos de seguridad;
7. mapa hacia módulos funcionales de Smart ERP;
8. casos de uso y pruebas de caracterización;
9. lista de objetos sin dueño o sin equivalencia;
10. estimación de cobertura manual, asistida y no migrable.

## Limitaciones explícitas

- XML no demuestra por sí solo el comportamiento real ni los datos efectivos.
- PL/SQL dinámico, llamadas externas, personalizaciones y objetos compilados
  pueden requerir investigación manual.
- Un formulario puede contener lógica que debe convertirse en dominio, no en otra
  pantalla idéntica.
- Un reporte visualmente idéntico puede ser funcionalmente incorrecto si cambian
  filtros, redondeo, seguridad o snapshot histórico.
- No se afirma migración completa sin reconciliación de datos y aceptación humana.
- No se guarda una contraseña, connect string o dato personal innecesario en logs,
  manifiestos o evidencia.

## Criterio de aceptación del perfil

Un origen Oracle Forms & Reports está caracterizado cuando todos los artefactos
esperados están inventariados o justificados, el grafo no tiene dependencias
desconocidas críticas, cada acceso a datos tiene propietario candidato, cada
pantalla/reporte tiene decisión de reemplazo y los objetos no migrables quedan
visibles con riesgo, responsable y tratamiento.
