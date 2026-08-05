# Sprint 3 — Identidad segura y primera demo visual

- Estado: En curso — `J11-S3-01` a `J11-S3-07` completadas; G7 independiente/PDF pendiente
- Fecha de inicio: 2026-07-28
- Duración propuesta: cuatro semanas
- Dependencia: Sprint 2 cerrado con validación independiente diferida
- ADR rectores: [ADR-0006](../../adr/0006-identidad-oidc-membresia-autorizacion.md), [ADR-0007](../../adr/0007-material-design-responsive-sobre-jsf.md) y [ADR-0008](../../adr/0008-logout-oidc-estabilidad-preview-wildfly.md)

## Objetivo

Construir una demo visual reproducible donde una persona se autentique mediante Keycloak, seleccione únicamente una empresa autorizada y navegue una pantalla real compuesta por plugins y personalizada de forma distinta para dos empresas.

El incremento demuestra seguridad, composición y aislamiento. No declara implementado un dominio ERP productivo.

## Decisiones confirmadas

- Keycloak es el proveedor OIDC externo inicial.
- WildFly usa `elytron-oidc-client`; el WAR no incorpora adaptadores propietarios.
- La identidad estable es el par `(issuer, subject)`.
- El ERP es dueño de usuarios locales, membresías empresariales, roles y permisos funcionales.
- Existe un realm inicial de Logixone; las empresas no son realms.
- La empresa activa se conserva en sesión del servidor y se revalida; ningún identificador del cliente es autoridad.
- El shell inicial usa Jakarta Faces 4.1 y CSS propio. Cualquier biblioteca visual adicional requiere un ADR con versión, licencia, compatibilidad JSF y justificación.
- Material Design 3 es el sistema de diseño sobre JSF, implementado inicialmente mediante tokens y CSS propio del shell.
- Toda pantalla debe ser responsive en compacto (`0–599px`), medio (`600–839px`) y expandido (`840px` o más) desde su propia historia.

## Alcance

- contratos Java puros de identidad y autorización;
- migración aditiva `core` V3;
- repositorios JPA/JTA y casos de uso de seguridad;
- bootstrap administrativo one-shot e idempotente;
- Keycloak y configuración OIDC declarados como código;
- contexto empresarial derivado de identidad y membresía;
- autorización y auditoría con actor real;
- shell web con login, logout, empresa activa, selector y menú;
- renderer inicial del contrato neutral de pantalla;
- personalizaciones A/B visibles;
- pruebas acumuladas, Playwright, demo reproducible, guía y PDF de cierre.

## Fuera de alcance

- facturación, ventas, inventario u otro dominio ERP productivo;
- almacenar credenciales en PostgreSQL;
- realm por empresa, SCIM o administración corporativa de Keycloak;
- endpoint administrativo público anónimo o basado solo en un header;
- SPA, aplicación móvil o API pública completa;
- editor visual libre de XHTML/CSS/JavaScript;
- promoción a producción.

## Secuencia de historias

| Orden | Historia | Resultado | Estado |
|---:|---|---|---|
| 1 | [J11-S3-00](J11-S3-00-gobierno-adr-planificacion.md) | gobierno, ADR y backlog aceptados | Completada |
| 2 | [J11-S3-01](J11-S3-01-modelo-identidad-autorizacion.md) | modelo neutral de usuario, membresía, rol y permiso | Completada |
| 3 | [J11-S3-02](J11-S3-02-migracion-core-v3-seguridad.md) | esquema `core` V3 aditivo | Completada |
| 4 | [J11-S3-03](J11-S3-03-persistencia-casos-uso-seguridad.md) | persistencia y casos de uso JTA | Completada |
| 5 | [J11-S3-04](J11-S3-04-keycloak-wildfly-oidc.md) | Keycloak y OIDC reproducibles | Completada |
| 6 | [J11-S3-05](J11-S3-05-contexto-confiable-autorizacion.md) | contexto, autorización y auditoría web | Completada |
| 7 | [J11-S3-06](J11-S3-06-shell-ui-navegacion.md) | shell visual y navegación por empresa | Completada |
| 8 | [J11-S3-07](J11-S3-07-render-pantalla-personalizada.md) | primera pantalla compuesta con A/B | Completada |
| 9 | [J11-S3-08](J11-S3-08-validacion-demo-cierre.md) | pruebas acumuladas, demo, guía y cierre | En validación: G2–G6 verdes; G7 pendiente |

`J11-S3-08` ejecutó los gates acumulados G2–G6 y habilitó el cierre de las historias
de código. El Sprint permanece abierto hasta completar G7.

## Criterios globales de éxito

- **CS-01:** ADR-0006 permanece aceptado y trazable desde historias, arquitectura y guía.
- **CS-02:** ninguna contraseña o token se almacena en el ERP ni se incluye en fuentes, imagen o logs.
- **CS-03:** `(issuer, subject)` identifica un único usuario local y atributos mutables no cambian su identidad.
- **CS-04:** membresías, roles y permisos empresariales pertenecen a `core` y usan concurrencia optimista.
- **CS-05:** V3 es aditiva, V1/V2 permanecen inmutables y JPA usa `validate`.
- **CS-06:** el bootstrap inicial es one-shot, idempotente, externo y no abre una ruta anónima.
- **CS-07:** WildFly integra OIDC de forma nativa y valida emisor, firma, audiencia, expiración y sesión.
- **CS-08:** cero, una y múltiples membresías tienen comportamientos seguros y explícitos.
- **CS-09:** la empresa activa nunca se acepta como autoridad desde headers, formularios, parámetros o cookies.
- **CS-10:** autorización efectiva combina identidad, membresía, empresa, plugin y permiso.
- **CS-11:** auditoría identifica al actor real sin registrar claims, tokens ni datos personales innecesarios.
- **CS-12:** menús y pantallas se calculan por empresa y permisos; ocultarlos no reemplaza guardas.
- **CS-13:** el shell permite login, logout, selección empresarial y navegación sin exponer dominios ficticios.
- **CS-14:** la pantalla de referencia renderiza el contrato neutral, no rutas XHTML declaradas por plugins.
- **CS-15:** empresas A y B ven personalizaciones diferentes sin filtración cruzada.
- **CS-16:** la infraestructura fija versiones/digests, usa secretos externos y conserva health y persistencia.
- **CS-17:** todas las pruebas diferidas quedan verdes antes de aceptar la demo o completar historias de código.
- **CS-18:** shell y pantallas compuestas usan Material Design 3 sin permitir CSS/JavaScript arbitrario desde plugins.
- **CS-19:** cada pantalla es utilizable en rangos compacto, medio y expandido, por teclado y sin overflow horizontal normal.
- **CS-20:** la guía se valida independientemente, se eleva a `1.0` si corresponde y el PDF final se regenera y revisa.

## Política excepcional de pruebas para la demo

El responsable de producto decidió el 2026-07-28 ejecutar las pruebas después de terminar la candidata visual. Durante `J11-S3-01` a `J11-S3-07`:

- se permiten compilaciones o empaquetados necesarios para construir y abrir la candidata;
- las pruebas automatizadas pueden quedar acumuladas para `J11-S3-08`;
- una prueba ya ejecutada y fallida no se ignora ni se presenta como verde;
- ninguna historia de código se marca `Completada` mientras su validación esté pendiente;
- no se cierra el Sprint, publica la guía `1.0`, promueve una imagen ni despliega a producción;
- `J11-S3-08` ejecuta la matriz completa antes de aceptar la demo.

Después del cierre de Sprint 3 vuelve a regir el flujo incremental normal salvo una nueva decisión explícita.

## Gates

| Gate | Resultado requerido |
|---|---|
| G0 | Markdown UTF-8, enlaces, ADR, historias y trazabilidad sin brechas |
| G1 | candidata compilable/empaquetable, sin afirmar pruebas verdes |
| G2 | JUnit y ArchUnit acumulados para identidad, límites y autorización |
| G3 | PostgreSQL V1→V2→V3, JPA/JTA, concurrencia, bootstrap y rollback |
| G4 | WildFly/Keycloak: login, logout, sesión, issuer/audience y denegaciones |
| G5 | Docker/Compose, secretos, imágenes por digest, health y persistencia |
| G6 | Playwright: selector, navegación, permisos, pantalla y A/B sin filtración |
| G7 | recorrido independiente, guía `1.0`, evidencias, retrospectiva y PDF final |

G0 se aplica durante la planificación. G2–G6 están verdes; G7 continúa obligatorio
en `J11-S3-08`.

## Escenario de demo

La demo usará exclusivamente datos ficticios declarados como tales:

- una identidad sin membresía para demostrar denegación;
- una identidad con una sola empresa para demostrar selección automática;
- una identidad con dos empresas para demostrar selector y cambio de contexto;
- dos empresas con personalizaciones `reference_custom_a` y `reference_custom_b`;
- roles empresariales distintos que produzcan navegación diferente;
- una pantalla de referencia compuesta desde el contrato público.

Las contraseñas y secretos de esas identidades se suministran mediante archivos locales ignorados y nunca aparecen en documentación, realm exportado, Compose o evidencia.

### Aviso de disponibilidad visual

El responsable de producto pidió ser avisado en el primer momento en que la demo
fuera realmente observable. El aviso ya se emitió después de comprobar una URL
arrancada, login ficticio, shell, empresa, menú y personalización A/B. No se basó solo
en XHTML empaquetado o un WAR compilado. El aviso no sustituye G7 ni convierte el
Sprint en cerrado.

## Riesgos

- una configuración incorrecta de redirect URI, issuer, proxy o TLS puede impedir login;
- acumular pruebas aumenta el coste de localizar regresiones al final;
- revocar membresía sin revalidar sesión puede conservar acceso indebido;
- mezclar roles de Keycloak con permisos empresariales puede crear dos fuentes de verdad;
- agregar una biblioteca visual sin decisión de licencia/versionado puede romper reproducibilidad;
- el bootstrap inicial puede convertirse en una puerta trasera si no es one-shot y cerrado por defecto;
- una pantalla atractiva puede inducir a presentar como productivo un dominio que todavía no existe.

## Siguiente paso permitido

La candidata está disponible en
`http://localhost:18080/logixone/faces/app/index.xhtml` mientras la composición local
permanezca levantada. G2–G6 están verdes y las historias 01–07 están completadas. El
siguiente paso es que una persona independiente ejecute `VALIDATION.md`; solo con un
dictamen satisfactorio se actualiza la guía a `1.0`, se documenta retrospectiva y
siguiente incremento y se regenera/verifica el PDF de cierre.
