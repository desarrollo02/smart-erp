# J11-S5-02 - Plantilla mínima de plugin productivo

- Estado: Completada
- Fecha: 2026-07-29
- Dependencia: `J11-S5-01` completada
- ADR rectores: [ADR-0002](../../adr/0002-arquitectura-plugins.md) y [ADR-0012](../../adr/0012-composicion-unica-y-migraciones-de-plugins.md)

## Objetivo

Entregar una plantilla reproducible que permita iniciar un plugin ERP real sin
copiar el fixture de referencia, inventar dependencias ni olvidar descriptor,
migraciones, pruebas, documentación o composición física.

La plantilla enseña la estructura mínima y genera un punto de partida neutral. No
contiene entidades, tablas, permisos, menús ni reglas de un dominio comercial.

## Cambio coherente implementado

1. definir entradas validadas para identidad Maven, `PluginId`, paquete Java,
   nombre legible y versión inicial;
2. mantener una fuente de plantilla versionada y un generador reproducible;
3. producir únicamente el módulo neutral mínimo; API pública, dominio, aplicación,
   infraestructura Jakarta, persistencia y UI se agregan después desde requisitos
   aprobados;
4. incluir una única `PluginDefinition`, registro CDI/SPI y pruebas mínimas, sin
   inventar una migración V1 vacía;
5. registrar explícitamente el módulo y su selección en
   `logixone-plugin-set`, sin activar perfiles de forma implícita;
6. generar documentación inicial para contratos, datos, permisos, pantalla,
   operación y decisiones pendientes;
7. demostrar el resultado creando un plugin neutral temporal, compilándolo y
   descartándolo fuera de las fuentes canónicas.

## Criterios de aceptación

- **CA-01:** la misma entrada produce byte a byte la misma estructura textual.
- **CA-02:** identidades inválidas, reservadas o duplicadas fallan sin dejar un
  módulo parcial.
- **CA-03:** el código generado no usa `javax.*` ni importa internos de kernel u
  otros plugins.
- **CA-04:** `plugin-api` sigue siendo la única dependencia obligatoria del
  descriptor neutral.
- **CA-05:** CDI y `ServiceLoader` exponen una única definición equivalente.
- **CA-06:** si el plugin declara persistencia, su primera migración vive en
  `db/migration/<plugin_id>/` y sólo usa `plg_<plugin_id>`.
- **CA-07:** la plantilla no crea relaciones JPA ni accesos SQL hacia tablas de
  otro propietario.
- **CA-08:** permisos, capacidades, menús y pantallas nacen vacíos o desde entradas
  explícitas; no se inventa comportamiento productivo.
- **CA-09:** una pantalla opcional usa JSF, Material Design 3 y los rangos
  responsive del shell, sin reemplazar XHTML de otro plugin.
- **CA-10:** la plantilla distingue plugin funcional de personalización; una
  personalización puede contribuir overlays mediante contratos públicos, nunca
  importar beans o vistas privadas.
- **CA-11:** el módulo generado pasa pruebas propias, ArchUnit y `mvn verify` en la
  composición que lo incluye.
- **CA-12:** WAR y migrador reciben el plugin mediante el mismo perfil del plugin
  set y la variante base permanece sin él.
- **CA-13:** la guía explica cómo generar, completar, componer, migrar, probar y
  retirar el plugin sin borrar datos.

## Pruebas ejecutadas

- unitarias del validador y del renderizado de plantilla;
- casos negativos de ruta, identidad, colisión y generación incompleta;
- comparación determinista de dos generaciones iguales;
- compilación del módulo generado con Java 21;
- pruebas del descriptor CDI/SPI y de propiedad del esquema;
- ArchUnit sobre dependencias e imports prohibidos;
- empaquetado base/con-plugin e inspección de WAR/migrador;
- `mvn verify` del corte completo.

El generador pasó 9 pruebas propias. La arquitectura quedó en 13 reglas verdes y
el reactor base ejecutó 191 pruebas sin fallos, errores ni omitidas. También se
compiló un plugin neutral generado y se incorporó temporalmente a una composición
real: el WAR recibió exactamente su JAR y el migrador exactamente su proveedor.

Aunque la herramienta no forma parte del runtime, se repitieron build Docker y
Compose con la variante de tres plugins. La aplicación quedó `UP`, el migrador
ejecutó cero cambios pendientes y el marcador persistente de `J11-S5-01`
continuó presente después de reutilizar los volúmenes.

## Documentación afectada

- guía de implementación para crear el ERP de una empresa;
- arquitectura de plugins y estrategia de pruebas;
- runbook o manual del generador;
- evidencia de `J11-S5-02`;
- índice documental del repositorio.

## Límites

- no implementar todavía `business_partners` ni otro dominio ERP;
- no copiar clases del legado ni del fixture como solución productiva;
- no crear un framework de carga dinámica;
- no decidir todavía un transporte de eventos ni outbox;
- no generar secretos, credenciales, datos de empresa ni migraciones destructivas;
- no declarar la historia terminada con archivos generados que no compilen.

## Resultado esperado

Un implementador puede crear el esqueleto de un plugin nuevo mediante un
procedimiento documentado, entender cada extensión y obtener una composición
verde sin conocer internos del kernel. El primer uso de negocio de esa ruta será
planificado después del cierre de Sprint 5.

## Resultado obtenido

`tools/plugin-scaffold` entrega un JAR ejecutable Java 21. Genera de forma
determinista siete archivos: POM, definición, prueba, `beans.xml`, proveedor de
`ServiceLoader`, README y checklist contractual. Valida ruta, identidades,
versiones, tipo y dependencia objetivo; trabaja en un directorio temporal y sólo
mueve el árbol completo al destino cuando la generación terminó correctamente.

El modo `functional` nace sin comportamiento. El modo `customization` exige el
plugin funcional objetivo y su rango compatible, y genera una dependencia
`REQUIRED`. Ningún modo modifica automáticamente el reactor ni el plugin set; esa
decisión física sigue siendo explícita y auditable.

El procedimiento reproducible está en el
[runbook del generador](../../runbooks/plugin-scaffold.md) y los comandos,
conteos, imágenes y verificaciones se conservan en la
[evidencia de J11-S5-02](../../evidence/J11-S5-02-plantilla-plugin-productivo.md).
