# J11-S2-07 — Contrato y composición de personalizaciones de pantalla

- Estado: Completada y verde
- Dependencia: `J11-S2-06` completada y verde
- Fecha de cierre: 2026-07-27

## Objetivo

Definir y demostrar en Java puro el contrato mediante el cual el plugin obligatorio de una empresa modifica pantallas publicadas por plugins funcionales, aplicándose como última capa sin importar internos, reemplazar recursos arbitrariamente ni debilitar controles del servidor.

## Alcance

### Contratos públicos

- identidades estables para pantalla, versión de contrato, elemento y slot;
- definición inmutable publicada por el plugin propietario de la pantalla;
- propiedades personalizables y restricciones que el propietario habilita explícitamente;
- overlay inmutable publicado por un plugin `CUSTOMIZATION`;
- diagnósticos estables para objetivo ausente, versión incompatible, referencia inválida, operación prohibida o conflicto.

Todos estos contratos deben permanecer en `plugin-api` como Java puro, sin Jakarta, JSF, PrimeFaces, CDI ni tipos internos del kernel.

### Composición empresarial

- compositor neutral que recibe una empresa y la composición efectiva de `J11-S2-06`;
- selección exclusiva del plugin de personalización asignado a esa empresa;
- validación previa de dependencias, propietario, pantalla, versión, elementos, slots y operaciones;
- aplicación del overlay después de todas las contribuciones funcionales;
- resultado determinista, inmutable y sin aplicación parcial;
- plugin funcional de referencia con una pantalla neutral extensible;
- dos plugins de personalización de referencia, cada uno asignable a una empresa distinta, para demostrar resultados diferentes.

### Operaciones representativas

El incremento debe demostrar al menos:

- cambiar etiqueta o texto de ayuda;
- ocultar o deshabilitar un elemento declarado personalizable;
- volver más estricta una condición requerida, sin poder relajarla;
- reordenar elementos dentro de una región autorizada;
- aportar contenido propio dentro de un slot explícito.

La lista definitiva y su semántica deben respetar el ADR de `J11-S2-01`. Agregar validadores, acciones, columnas o filtros podrá hacerse en incrementos posteriores mediante el mismo modelo tipado.

## Reglas

- el plugin funcional es propietario del contrato y declara qué puede cambiarse;
- la personalización declara dependencia compatible con cada plugin funcional que modifica;
- toda referencia usa identificadores públicos, nunca nombres de clases, beans, expresiones EL o rutas internas;
- un fragmento agregado pertenece al plugin de personalización y solo puede entrar por un slot declarado;
- la composición falla de forma segura antes de producir resultado si el overlay completo no es válido;
- visibilidad, habilitación y required de presentación nunca conceden autorización ni omiten validaciones del caso de uso;
- ninguna personalización de otra empresa participa en el cálculo;
- identificadores duplicados y conflictos no se resuelven por orden accidental.

## Fuera de alcance

- renderizado Jakarta Faces/PrimeFaces y pruebas Playwright;
- una pantalla productiva de ventas, facturación u otro dominio ERP;
- reemplazo de XHTML, beans, controladores, clases, CSS o JavaScript global;
- acceso directo a entidades, repositorios o tablas de otro plugin;
- editor visual de personalizaciones;
- contratos productivos de personalización para reportes, cálculos, flujos o integraciones;
- instalación dinámica, descarga o hot deployment de plugins.

## Criterios de aceptación

- **CA-01:** los contratos de pantalla y overlay son Java puros, pequeños, inmutables y documentados.
- **CA-02:** cada pantalla declara identidad y versión estables junto con su plugin propietario.
- **CA-03:** elementos, slots y propiedades solo pueden referenciarse mediante identificadores públicos.
- **CA-04:** el descriptor funcional publica una definición y la personalización publica overlays compatibles.
- **CA-05:** el compositor recibe una empresa y usa exactamente su personalización asignada.
- **CA-06:** todos los plugins funcionales se componen antes de aplicar la única capa empresarial final.
- **CA-07:** cambiar texto o ayuda autorizados produce el resultado esperado.
- **CA-08:** ocultar o deshabilitar solo funciona para elementos que lo permiten.
- **CA-09:** una personalización puede endurecer `required` y nunca relajar una exigencia estándar.
- **CA-10:** reordenamiento y slot adicional respetan regiones y restricciones publicadas.
- **CA-11:** pantalla, versión, elemento, slot u operación inválidos rechazan el overlay completo con diagnóstico estable.
- **CA-12:** dos personalizaciones que reclaman la misma empresa o una personalización ajena se rechazan antes de componer.
- **CA-13:** dos empresas obtienen resultados distintos y deterministas sin filtración cruzada.
- **CA-14:** no se importan internos ni se accede a tablas de otro plugin; ArchUnit lo verifica.
- **CA-15:** ningún overlay puede eliminar autorización, validación de negocio, auditoría o guarda operativa.
- **CA-16:** las variantes de distribución requeridas incluyen exactamente los plugins de referencia esperados.
- **CA-17:** pruebas unitarias, integración, ArchUnit, WAR y `mvn verify` quedan verdes.
- **CA-18:** documentación explica cómo un futuro adaptador JSF/PrimeFaces consumirá el resultado sin convertirlo en contrato público.

## Gates

1. pruebas inmediatas de `plugin-api` después de cada contrato;
2. pruebas unitarias del validador y compositor;
3. matriz de dos empresas, dos personalizaciones y referencias válidas/inválidas;
4. ArchUnit para aislamiento y Java puro;
5. inspección de variantes del WAR;
6. `mvnw.cmd -B verify`.

## Siguiente historia permitida

`J11-S2-08` queda habilitada. La composición de pantalla es segura, determinista, aislada por empresa y está certificada en Java puro, PostgreSQL, JTA, WildFly y las variantes físicas del WAR.

## Resultado del cierre

- CA-01 a CA-18 satisfechos.
- `PluginApiVersion.CURRENT = 0.3.0` incorpora definiciones de pantalla y overlays tipados.
- El compositor rechaza atómicamente referencias, versiones, operaciones, posiciones, capacidades de slot y conflictos inválidos.
- Dos empresas sobre el mismo catálogo reciben composiciones A/B distintas sin filtración cruzada.
- El gate limpio totalizó 136 pruebas con PostgreSQL real; el baseline normal contiene 122.
- Las 6 pruebas runtime sobre WildFly/JTA quedaron verdes.
- Los WAR base, referencia y personalizaciones contienen respectivamente 0, 1 y 3 plugins de referencia.
- Evidencia reproducible: [J11-S2-07 — Contrato y composición de personalizaciones de pantalla](../../evidence/J11-S2-07-contrato-personalizacion-pantallas.md).
