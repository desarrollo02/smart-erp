# J11-S2-01 — Evidencia de elaboración de ADR-0005

- Fecha: 2026-07-27
- Estado: Verde; ADR aceptado
- Tipo de cambio: arquitectura y documentación; sin código, SQL, POM ni infraestructura

## Autorización y alcance

El usuario aceptó continuar con el Sprint 2 ampliado después de confirmar dos reglas de producto:

1. cada empresa tendrá un plugin de personalización distinto;
2. la personalización será obligatoria y podrá modificar pantallas de otros plugins.

La historia se mantuvo documental porque sus decisiones afectan arquitectura, seguridad, datos, compatibilidad y disponibilidad. No se inició `J11-S2-02` ni se modificó el contrato Java preliminar.

## Fuentes revisadas

- `AGENTS.md`;
- ADR-0002 sobre composición física, activación y límites entre plugins;
- ADR-0003 sobre propiedad de `core`, Flyway, JPA y recuperación;
- vista general y estrategia de pruebas certificadas en Sprint 1;
- `PluginDescriptor`, `PluginDefinition`, `PluginCatalogResolver` y contrato de health vigentes;
- épicas y las 15 preguntas de decisión de `J11-S2-01`;
- evidencia de contratos de plugins y de liveness/readiness de Sprint 1.

## Resultado

Se creó y aceptó [ADR-0005](../adr/0005-contexto-empresarial-activacion-personalizacion.md). La decisión cubre:

- `CompanyId` UUID opaco y ciclo `INACTIVE`/`ACTIVE`;
- alta transaccional con personalización no nula y exclusiva;
- categoría obligatoria `FUNCTIONAL`/`CUSTOMIZATION`;
- activación deseada y efectiva solo para plugins funcionales;
- conservación de decisiones cuando un JAR se retira;
- contexto neutral y prohibición de confiar en headers no autenticados;
- concurrencia optimista, idempotencia, rollback y auditoría;
- cuarentena por empresa frente a personalización ausente o incompatible;
- readiness global reservado para fallos comunes de instancia;
- contratos públicos de pantalla y overlay aplicado como última capa;
- evolución aditiva de `core` V1 a V2 y recuperación.

## Decisiones especialmente relevantes

### Alta y despliegue

Como la arquitectura no carga JAR dinámicamente, la imagen con el plugin de personalización debe desplegarse antes de registrar la empresa. `core.company.customization_plugin_id` se propone no nulo y único para expresar la cardinalidad obligatoria directamente en la base.

### Disponibilidad

Una empresa sin su personalización válida no recibe UI estándar ni operaciones parciales: todas sus capacidades se deniegan. La instancia conserva readiness global para no afectar a empresas sanas. Un catálogo físico inválido, base inaccesible, esquema pendiente o JPA inválido sí produce `DOWN` global.

### Compatibilidad del API

Agregar `PluginKind` sin valor implícito rompe deliberadamente el contrato preliminar. La implementación propuesta para `J11-S2-02` elevará `plugin-api` de `0.1.0` a `0.2.0` y migrará conjuntamente el único plugin de referencia existente.

### Extensión de pantallas

La personalización no obtiene acceso especial. El plugin funcional publica pantalla, versión, elementos, slots e invariantes; el overlay solo usa esos identificadores. Reemplazar XHTML, importar beans, acceder a tablas ajenas o relajar controles del servidor permanece prohibido.

## Alternativas documentadas

ADR-0005 compara y rechaza explícitamente:

- identificadores secuenciales o comerciales;
- una personalización compartida configurada por empresa;
- asignación opcional separada de la empresa;
- desactivar personalizaciones como plugins comunes;
- degradar readiness global por una sola empresa;
- aceptar empresa desde un header no autenticado;
- reemplazar directamente XHTML, beans o recursos internos.

## Cobertura de criterios

| Criterio | Evidencia en ADR-0005 |
|---|---|
| `CA-01` | UUID comparado con identidad secuencial/comercial y generación asignada al kernel. |
| `CA-02` | Estados `INACTIVE`/`ACTIVE`, alta inactiva y disponibilidad derivada. |
| `CA-03` | Ausencia de fila igual a desactivado y algoritmo de efectividad enumerado. |
| `CA-04` | Decisiones, asignación y datos sobreviven desactivación o retiro físico. |
| `CA-05` | Reglas transaccionales para dependencias, habilitación y deshabilitación. |
| `CA-06` | Versión optimista, resultado `UNCHANGED`, conflicto y rollback definidos. |
| `CA-07` | Headers, cookies, parámetros y otros datos no autenticados declarados no confiables. |
| `CA-08` | Comandos administrativos explícitos separados del puerto de contexto funcional. |
| `CA-09` | Códigos estables, respuesta funcional genérica y detalle administrativo futuro. |
| `CA-10` | Tabla de propiedad por módulo separa APIs, dominio, aplicación e infraestructura. |
| `CA-11` | V2 aditiva, V1 inmutable, JPA `validate` y rollback sin eliminar datos. |
| `CA-12` | ADR, arquitectura, estrategia, historia, Sprint, épicas e índices actualizados. |
| `CA-13` | `PluginKind` obligatorio y columna de personalización no nula/única. |
| `CA-14` | Cuarentena empresarial separada de liveness y readiness global. |
| `CA-15` | Reemplazo validado, optimista y atómico con convivencia temporal de ambos JAR. |
| `CA-16` | Dependencias por categoría, orden funcional primero y selección empresarial única. |
| `CA-17` | Contrato cerrado por defecto y prohibiciones sobre internos, datos y controles. |

## Gates documentales

- inspección completa de ADR-0002, ADR-0003, arquitectura, estrategia y contratos actuales;
- validación incremental de UTF-8 estricto, caracteres de reemplazo y enlaces locales;
- auditoría de las 15 preguntas contra secciones de ADR-0005;
- búsqueda de contradicciones sobre activación, UI, readiness y propiedad de datos;
- cero modificación de Java, SQL, POM, Docker o Compose.

La repetición final produjo:

```text
markdown_files=59 g0_errors=0
stories=9 story_criteria=147 sprint_criteria=16 structural_errors=0
```

Las 15 preguntas de `J11-S2-01` tienen resolución explícita en ADR-0005 y no quedaron referencias locales rotas.

## Pruebas no ejecutadas

No se ejecutaron Maven, Docker, Compose, PostgreSQL ni pruebas Java porque el cambio es exclusivamente documental y ADR-0005 todavía no autoriza implementación. El gate aplicable es G0.

La carpeta no contiene metadata Git, por lo que no fue posible producir un estado o diff verificable mediante Git. La revisión de alcance se realizó sobre los archivos documentales editados explícitamente.

## Archivos creados o modificados

- `docs/adr/0005-contexto-empresarial-activacion-personalizacion.md` e índice de ADR;
- `docs/architecture/overview.md`;
- `docs/architecture/test-strategy.md`;
- `docs/sprints/sprint-02/J11-S2-01-adr-contexto-activacion.md`;
- `docs/sprints/sprint-02/README.md`;
- ambas épicas relacionadas e índices generales;
- esta evidencia.

## Estado y siguiente paso

El usuario autorizó continuar el 2026-07-27. ADR-0005 queda `Aceptado`, `J11-S2-01` queda completada y `J11-S2-02` pasa a ser la historia activa.
