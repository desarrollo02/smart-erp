# Sprint 5 - Fundaciones ejecutables para plugins productivos

- Estado: Gates técnicos G0-G6 y demo verdes; G7 independiente pendiente
- Fecha de inicio: 2026-07-29
- Dependencia: Sprint 4 con gates técnicos verdes y validación independiente pendiente
- ADR rectores: [ADR-0012](../../adr/0012-composicion-unica-y-migraciones-de-plugins.md)
  y [ADR-0013](../../adr/0013-eventos-integracion-outbox-por-plugin.md)

## Objetivo

Preparar una ruta reproducible para crear el primer plugin ERP sin duplicar la
composición física, mezclar esquemas ni improvisar contratos de integración. Este
Sprint todavía no implementa `business_partners`: deja verdes los habilitadores
que ese plugin necesita.

## Autorización de continuidad

El responsable de producto decidió el 2026-07-29 continuar y dejar pendiente la
validación independiente del cierre anterior. Los resultados técnicos verdes de
Sprint 4 se conservan. Sprint 4 no se declara cerrado y no se autorizan promoción,
publicación de la guía `1.0` ni producción.

Sprint 5 usa nuevamente pruebas incrementales normales. La excepción vigente solo
permite coexistir con la validación humana pendiente; no permite acumular ni omitir
pruebas de código nuevas.

## Alcance

- composición física única para WAR y migrador;
- descubrimiento neutral de descriptores en el proceso one-shot;
- migraciones por esquema `plg_<plugin_id>` y orden topológico;
- fixture persistente del plugin de referencia;
- plantilla mínima reproducible de plugin productivo;
- decisión y contrato mínimo de eventos/outbox cuando sea necesario;
- guía de implementación actualizada;
- demo visual responsive sobre el baseline real;
- validación integral y PDF obligatorio de cierre.

## Fuera de alcance

- entidades o casos de uso de clientes/proveedores;
- copiar código o tablas del sistema legado;
- migraciones dinámicas por empresa;
- borrar esquemas al desactivar o retirar plugins;
- carga dinámica de JAR;
- promoción a producción mientras Sprint 4 conserve G7 pendiente.

## Secuencia de historias

| Orden | Historia | Resultado | Estado |
|---:|---|---|---|
| 1 | [J11-S5-00](J11-S5-00-gobierno-planificacion.md) | excepción, alcance, ADR y gates | Completada documentalmente |
| 2 | [J11-S5-01](J11-S5-01-migraciones-plugins-composicion.md) | composición única y migraciones `plg_*` | Completada |
| 3 | [J11-S5-02](J11-S5-02-plantilla-plugin-productivo.md) | plantilla mínima de plugin productivo | Completada |
| 4 | [J11-S5-03](J11-S5-03-eventos-integracion-outbox.md) | decisión y contrato mínimo de eventos/outbox | Completada documentalmente |
| 5 | [J11-S5-04](J11-S5-04-validacion-demo-cierre.md) | validación, demo visual, guía y PDF | Gates técnicos verdes; G7 pendiente |

No se inicia una historia de código antes de crear su documento con criterios,
límites, documentación afectada y pruebas.

## Criterios globales de éxito

- **CS-01:** WAR y migrador reciben la misma selección física de plugins.
- **CS-02:** `plugin-api` continúa libre de Jakarta e infraestructura.
- **CS-03:** el catálogo del migrador usa las mismas validaciones y orden que el kernel.
- **CS-04:** cada plugin solo migra `plg_<plugin_id>`.
- **CS-05:** cada esquema conserva historial, checksum e idempotencia independientes.
- **CS-06:** retirar o desactivar un plugin no elimina datos.
- **CS-07:** la plantilla no contiene lógica de un dominio concreto.
- **CS-08:** cualquier intercambio asíncrono nace con propietario, entrega y
  recuperación explícitos.
- **CS-09:** la guía explica cómo componer, migrar y verificar una distribución.
- **CS-10:** el Sprint termina con demo JSF Material Design 3 responsive y PDF
  revisado visualmente.

## Gates

| Gate | Resultado requerido |
|---|---|
| G0 | ADR, historias, enlaces y trazabilidad coherentes |
| G1 | unitarias de planificación, propiedad y orden de migraciones |
| G2 | composición Maven base/referencia/personalizaciones |
| G3 | PostgreSQL vacío, idempotencia, checksum y conservación |
| G4 | ArchUnit y límites de dependencias |
| G5 | Docker/Compose, migrator, app y health |
| G6 | demo visual a 375/720/1280 px |
| G7 | guía, evidencia, retrospectiva, PDF y validación independiente aplicable |

## Demo visual objetivo

1. construir aplicación y migrador con el mismo perfil de referencia;
2. mostrar en logs que `core` y `plg_reference_plugin` se migraron antes del WAR;
3. iniciar sesión y comprobar el plugin presente en la administración;
4. activar la capacidad para una empresa de demo;
5. abrir su pantalla JSF Material Design 3;
6. repetir la vista en 375, 720 y 1280 px;
7. recrear la aplicación y comprobar que los datos/migraciones no se pisan.

## Siguiente incremento

Los gates técnicos de este Sprint permiten planificar
[Sprint 6](../sprint-06/README.md) para `business_partners`, comenzando por
caracterización del comportamiento necesario, modelo de dominio y contratos
públicos antes de persistencia o UI. El código no comienza antes de aceptar esa
caracterización.

## Retrospectiva técnica

### Funcionó bien

- una única selección física alimenta WAR y migrador y evita composiciones
  divergentes;
- cada plugin conserva migraciones e historial en su esquema `plg_*`;
- el generador convierte reglas arquitectónicas en una plantilla verificable;
- ADR-0013 evita introducir infraestructura asíncrona sin caso real;
- la demo A/B volvió a comprobar que una personalización exclusiva puede cambiar
  una pantalla funcional mediante contratos públicos, no reemplazando XHTML.

### Hallazgos

- la primera ejecución visual reveló que el bootstrap global debía usar exactamente
  la identidad ficticia persistida y permanecer desactivado después del alta;
- el logout requería alinear el redirect posterior tanto en WildFly como en el
  cliente persistido de Keycloak;
- una consulta diagnóstica usó inicialmente `marker` en vez de `fixture_key`; se
  corrigió contra el esquema real antes de registrar el resultado.

### Acción para Sprint 6

Crear primero historias de gobierno y caracterización. Mantener pruebas
incrementales, reservar contratos/slots de UI antes de una personalización y
materializar outbox únicamente cuando aparezca el primer evento con productor y
consumidor concretos.

## Condición de cierre formal

G0-G6 están técnicamente verdes. G7 requiere que una persona independiente complete
la ficha de validación de la guía candidata. Hasta entonces no se declara cerrado
Sprint 4 ni Sprint 5, no se publica la guía `1.0` y no se promueve la imagen.
