# J11-S3-00 — Evidencia de gobierno, ADR y planificación del Sprint 3

- Fecha: 2026-07-28
- Estado: Verde
- Tipo de cambio: documentación, arquitectura y gobierno; sin código, POM, SQL ni infraestructura ejecutable

## Decisión de producto recibida

El responsable de producto confirmó explícitamente:

- Keycloak como proveedor OIDC externo inicial;
- integración por el soporte nativo `elytron-oidc-client` de WildFly;
- autenticación y credenciales en Keycloak;
- usuarios locales por `(issuer, subject)`, membresías, roles y permisos funcionales en el ERP;
- un realm inicial de Logixone, sin convertir empresas en realms;
- contexto empresarial construido y revalidado por el servidor;
- continuidad inmediata con el ADR y la planificación;
- ejecución de las pruebas automatizadas después de terminar la candidata de demo visual.

## Fuentes revisadas

- `AGENTS.md`, arquitectura, estrategia de pruebas y Definition of Done;
- cierre, retrospectiva y siguiente incremento de Sprint 2;
- ADR-0002 a ADR-0005;
- modelo implementado de empresa, activación, guarda, contribuciones y pantallas;
- guía `1.0-rc1`, elevada en esta historia a `1.0-rc2` como adenda planificada;
- [WildFly 41 — secure deployment de `elytron-oidc-client`](https://docs.wildfly.org/41/feature-pack/doc/reference/subsystem/elytron-oidc-client/secure-deployment/index.html);
- [Keycloak — planificación para proteger aplicaciones](https://www.keycloak.org/securing-apps/overview);
- [Keycloak — guía de administración](https://www.keycloak.org/docs/latest/server_admin/);
- [Keycloak 26.7.0 — anuncio oficial](https://www.keycloak.org/2026/07/keycloak-2670-released).

Las referencias oficiales se consultaron el 2026-07-28. No se descargaron binarios ni imágenes durante esta historia.

## ADR-0006

Se creó y aceptó [ADR-0006](../adr/0006-identidad-oidc-membresia-autorizacion.md). Sus decisiones principales son:

1. Keycloak permanece fuera del WAR y la aplicación usa OIDC estándar de WildFly.
2. Authorization Code Flow protege el shell server-side; no hay passwords locales ni tokens en almacenamiento del navegador.
3. `(issuer, subject)` es la identidad externa estable; email y username son atributos.
4. membresías, roles y permisos funcionales pertenecen a `core`.
5. una empresa no es un realm y un `CompanyId` aportado por el cliente no concede acceso.
6. autorización efectiva combina usuario, membresía, empresa, plugin y permiso.
7. V3 será aditiva y V1/V2 permanecerán inmutables.
8. bootstrap administrativo será one-shot, idempotente y sin endpoint anónimo.
9. Jakarta Faces 4.1 será el shell inicial; cualquier biblioteca adicional requiere decisión explícita.
10. liveness no cambia y readiness no consulta sincrónicamente a Keycloak en cada sondeo.

La línea Keycloak 26.7.x solo queda como candidata documentada. `J11-S3-04` debe seleccionar versión exacta, verificar compatibilidad y fijar digest antes de incorporarla a infraestructura.

## Backlog resultante

Se creó la [épica de identidad y demo visual](../backlog/epica-identidad-autorizacion-demo-visual.md) con 12 criterios. El [Sprint 3](../sprints/sprint-03/README.md) contiene 18 criterios globales y nueve historias lineales que suman 136 criterios de aceptación:

| Orden | Historia | Criterios | Resultado esperado |
|---:|---|---:|---|
| 1 | `J11-S3-00` | 15 | ADR y planificación |
| 2 | `J11-S3-01` | 14 | modelo neutral |
| 3 | `J11-S3-02` | 14 | esquema `core` V3 |
| 4 | `J11-S3-03` | 14 | persistencia y casos de uso |
| 5 | `J11-S3-04` | 14 | Keycloak/WildFly OIDC |
| 6 | `J11-S3-05` | 14 | contexto y autorización |
| 7 | `J11-S3-06` | 15 | shell y navegación |
| 8 | `J11-S3-07` | 14 | pantalla personalizada A/B |
| 9 | `J11-S3-08` | 22 | validación, demo y cierre |

La auditoría estructural produjo:

```text
STORIES=9
STORY_ACCEPTANCE=136
GLOBAL_ACCEPTANCE=18
EPIC_ACCEPTANCE=12
STRUCTURAL_ERRORS=0
```

Cada historia contiene estado, dependencia, objetivo, alcance, fuera de alcance, criterios y gates.

## Excepción temporal de pruebas

La decisión de ejecutar las pruebas después de terminar la candidata visual quedó persistida en:

- `AGENTS.md`;
- estrategia de pruebas;
- índice documental y de Sprints;
- épica y README de Sprint 3;
- cada historia de implementación;
- ADR-0006 y esta evidencia.

La excepción cambia el calendario, no la Definition of Done. `J11-S3-01` a `J11-S3-07` solo pueden alcanzar `Implementada pendiente de validación`. `J11-S3-08` acumula JUnit, ArchUnit, PostgreSQL/Testcontainers, JPA/JTA, Keycloak/OIDC, Docker/Compose, health, seguridad negativa y Playwright. No se permite cerrar el Sprint, promover una imagen, publicar la guía `1.0` ni desplegar a producción con gates pendientes.

## Guía y arquitectura

- la arquitectura se elevó a versión 7 e incorporó la cadena Keycloak → WildFly → usuario local → membresía → empresa → plugin → permiso;
- la estrategia de pruebas se elevó a versión 8 y documentó la excepción temporal;
- la guía pasó a `1.0-rc2`, manteniendo como disponible únicamente el baseline de Sprint 2 y marcando identidad/UI como plan confirmado pendiente;
- la ficha independiente sigue sin ejecutarse y se actualizará al baseline de demo antes del recorrido;
- el PDF no se regenera en esta historia porque Sprint 3 recién comienza; será obligatorio en `J11-S3-08` contra el baseline final.

## G0 final

El control se ejecutó después de incorporar esta evidencia y produjo:

```text
MARKDOWN_FILES=82
UTF8_ERRORS=0
DAMAGED_FILES=0
LOCAL_LINKS=228
BROKEN_LINKS=0
ADR_FILES=6
ADR_INVALID=0
SPRINT3_STORIES=9
SPRINT3_ACCEPTANCE=136
SPRINT3_STRUCTURAL_ERRORS=0
```

Los 82 Markdown incluyen `AGENTS.md` y 81 documentos bajo `docs/`. Los enlaces HTTP oficiales se excluyen del conteo de rutas locales y se registran como referencias consultadas, no como archivos del repositorio.

## Incidencias

El primer control posterior a crear ADR-0006 usó una subexpresión PowerShell con un paréntesis incompleto. El parser detuvo el comando antes de leer o modificar archivos. Se repitió con expresiones simples y confirmó UTF-8 válido, estado `Aceptado`, siete secciones principales e índice actualizado.

## Pruebas no ejecutadas

No se ejecutaron Maven, JUnit, ArchUnit, PostgreSQL, Docker, Keycloak ni Playwright. La historia modifica exclusivamente Markdown y la decisión de producto difiere las pruebas de implementación hasta la candidata visual. El único gate aplicable a `J11-S3-00` es G0 documental; ningún resultado futuro se presenta como verde.

## Archivos creados o modificados

- `AGENTS.md` y reglas documentales de la excepción temporal;
- ADR-0006 e índice de decisiones;
- nueva épica e índice de backlog;
- README y nueve historias de Sprint 3;
- arquitectura y estrategia de pruebas;
- guía `1.0-rc2` y ficha independiente;
- índices de documentación, Sprints y evidencias;
- continuidad del cierre de Sprint 2;
- esta evidencia.

## Cobertura de aceptación

| Criterio | Evidencia |
|---|---|
| `CA-01` | ADR-0006 aceptado y enlazado. |
| `CA-02` | Frontera Keycloak/ERP en ADR, épica y Sprint. |
| `CA-03` | `(issuer, subject)` como identidad estable. |
| `CA-04` | Un realm inicial y empresas mantenidas en `core`. |
| `CA-05` | `elytron-oidc-client` y prohibición de adaptador propietario. |
| `CA-06` | Sesión server-side y membresía revalidada. |
| `CA-07` | V3 aditiva; V1/V2 inmutables. |
| `CA-08` | Bootstrap one-shot e idempotente. |
| `CA-09` | Login, selector, shell, pantalla y A/B en la secuencia. |
| `CA-10` | Exclusiones explícitas en Sprint e historias. |
| `CA-11` | Auditoría estructural: nueve historias, cero brechas. |
| `CA-12` | Estado intermedio y gate acumulado `J11-S3-08`. |
| `CA-13` | Validación independiente y PDF en G7. |
| `CA-14` | Índices de ADR, backlog, Sprint, docs y evidencia actualizados. |
| `CA-15` | G0 final registrado abajo después de esta evidencia. |

## Conclusión y siguiente paso

`J11-S3-00` queda completada. La siguiente historia autorizada es `J11-S3-01`: modelo neutral de identidad y autorización. No debe introducir Jakarta, HTTP, Keycloak, JWT, JPA, Faces ni PrimeFaces en los módulos puros.
