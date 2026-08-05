# Evidencia J11-S7-04 — Aplicación y seguridad de `commercial_catalog`

- Fecha: 2026-07-30
- Estado: verde
- Historia: [J11-S7-04](../sprints/sprint-07/J11-S7-04-aplicacion-seguridad-commercial-catalog.md)
- ADR: [ADR-0021](../adr/0021-aplicacion-autorizacion-auditoria-commercial-catalog.md)
- Entorno: Windows, Java 21.0.11, Maven Wrapper 3.9.16 y PostgreSQL 18.4 por digest

## Resultado funcional

El descriptor declara exactamente:

1. `commercial_catalog.view`;
2. `commercial_catalog.items.manage`;
3. `commercial_catalog.prices.manage`;
4. `commercial_catalog.definitions.manage`.

La aplicación incorpora:

- contexto derivado de `AuthorizedCompanyOperation`, con plugin y permiso exactos;
- alta y mantenimiento optimista de ítems, identificadores, unidades,
  clasificación, perfil tributario, variante y ciclo de vida;
- alta y mantenimiento de listas, entradas de precio y ciclo de vida;
- alta/listado de unidades, categorías, marcas, etiquetas, perfiles tributarios y
  familias de variantes, sin SQL desde la futura UI;
- búsqueda paginada de ítems por código, nombre o identificador y filtros de
  tipo/estado;
- búsqueda paginada de listas con conteos total/activo de entradas;
- detalle, conversión y cotización por empresa;
- adaptadores CDI para `CatalogItemDirectory`, `CatalogUnitConversions` y
  `CatalogPricing`;
- frontera JTA y auditoría técnica sin nombres, descripciones, códigos escaneables,
  importes, tasas o valores de atributos.

## Seguridad negativa y resultados estables

Las pruebas demostraron que plugin o permiso incorrectos se rechazan antes de
generar identidad o tocar el repositorio. La empresa no se recibe de una entrada
libre: procede del contexto autenticado. Las consultas de otra empresa no devuelven
ítems, listas o definiciones.

Los resultados distinguen acceso denegado, no encontrado, versión, código,
identificador, referencia, vigencia y operación inválida. Las excepciones SQL/JPA
no forman parte del contrato de aplicación.

La auditoría registra actor, empresa, plugin, permiso, operación, recurso/ID
técnico, versiones, resultado y correlación. Los tests verifican expresamente que
los nombres y descripciones comerciales usados como datos de prueba no aparecen en
los eventos.

## Gate unitario del módulo

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk\jdk-21.0.11+10').Path
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -B -pl plugins/commercial-catalog -am test
```

El corte definitivo del módulo ejecutó 35 pruebas unitarias, con cero fallos,
errores u omisiones. Doce pruebas nuevas cubren contratos de aplicación, comandos,
consultas, definiciones, permisos, empresa, auditoría y conflicto optimista.

## Gate PostgreSQL

```powershell
.\mvnw.cmd -B -pl plugins/commercial-catalog -am verify `
  "-Dlogixone.postgres.integration=true"
```

Resultado: cinco módulos del alcance y `BUILD SUCCESS`; 35 unitarias y 12
integraciones, sin fallos, errores u omisiones. Los siete escenarios JPA validaron:

- las seis familias de definiciones y su aislamiento empresarial;
- búsqueda por identificador, filtros y scopes del ítem;
- búsqueda/resumen de listas con conteos de entradas;
- round-trip completo, control optimista, historia de precios y secuencia
  concurrente.

Los otros cinco escenarios reconfirmaron las veinte tablas, V1 inmutable,
idempotencia, constraints y ausencia de cruces de propietario.

## Gate arquitectónico

```powershell
.\mvnw.cmd -B -pl tests/architecture-tests -am test
```

Resultado: 18 módulos construidos y 20/20 pruebas verdes. Las 16 reglas ArchUnit
confirmaron que API, dominio, aplicación, comandos, consultas, definiciones y
puertos no dependen de Jakarta, JPA, JDBC, Hibernate ni internos de otro plugin.
Las cuatro pruebas restantes validaron composición física y plugin de referencia.

## Reactor integral

```powershell
.\mvnw.cmd -B verify
```

Resultado: 22/22 módulos y `BUILD SUCCESS`. Se contabilizaron 77 reportes y 291
pruebas unitarias, con cero fallos, errores u omisiones. El WAR base fue empaquetado;
`commercial_catalog` no se incorporó físicamente, conforme al alcance de S7-04.

## Gate documental

El escaneo estricto recorrió todos los Markdown mantenidos, validó UTF-8, buscó
marcadores de texto dañado y resolvió los enlaces locales:

```text
MARKDOWN_FILES=191
BAD_FILES=0
LOCAL_LINKS=695
BROKEN_LINKS=0
```

## Incidencias detectadas y corregidas

1. La primera terminal heredó JDK 8 del sistema. Maven Enforcer detuvo el build
   antes de compilar; se fijó `JAVA_HOME` al JDK 21 validado de `.tools` y se repitió
   el gate.
2. Los helpers privados `code`/`version` del record de definiciones colisionaron con
   accessors generados. El compilador bloqueó el avance; se renombraron y la misma
   prueba quedó verde.
3. PostgreSQL/Hibernate estricto rechazó `entry` como alias JPQL reservado en el
   resumen de precios. Se cambió a `priceEntry`, se repitió primero la suite JPA y
   después el gate PostgreSQL completo, ambos verdes.

No se omitió, relajó ni desactivó ninguna prueba. Cada ejecución fallida detuvo el
avance hasta corregir su causa y repetir el gate relevante.

## Archivos y documentación

- aplicación: permisos, contexto, resultados, comandos, consultas, definiciones,
  fachada y adaptadores públicos bajo `plugins/commercial-catalog/src/main/java`;
- infraestructura: adaptadores CDI/JTA, generador UUID, búsquedas JPA y repositorio
  de definiciones;
- pruebas: tres suites de aplicación nuevas y dos escenarios PostgreSQL nuevos;
- decisión: ADR-0021 e índice ADR;
- contrato: `plugins/commercial-catalog/docs/plugin-contract.md`;
- guía de implementación: edición `1.0-rc40` y ficha independiente alineada;
- historia y estado de Sprint 7 actualizados.

El workspace entregado no contiene metadatos `.git`; por ello no fue posible
obtener `git status`/`git diff`. La revisión se realizó mediante inventario de
archivos, compilación, pruebas, búsquedas de higiene y gates reproducibles.

## Límites conservados y continuidad

- no se agregó menú, endpoint, Jakarta Faces, CSS, JavaScript ni Playwright;
- no se compuso el plugin en WAR/migrador ni se modificó Docker/Compose;
- no se crearon stock, costos, compras, ventas, documentos, SIFEN u outbox;
- no se regenera el PDF porque Sprint 7 no está cerrando;
- no existe todavía una nueva demo visual del catálogo;
- la validación independiente transversal de la guía candidata sigue pendiente.

El siguiente trabajo autorizado es `J11-S7-05`: interfaz real de directorio, alta y
ficha del catálogo con Jakarta Faces 4.1, Material Design 3 y responsive en 375,
720 y 1280 px. Al completar una candidata navegable se notificará expresamente al
responsable de producto.
