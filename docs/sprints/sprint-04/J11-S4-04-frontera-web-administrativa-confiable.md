# J11-S4-04 — Frontera web administrativa confiable

- Estado: Completada; validada en `J11-S4-08`
- Fecha: 2026-07-28
- Dependencias: `J11-S4-01` a `J11-S4-03` y ADR-0009
- Política de pruebas: matriz automatizada diferida a `J11-S4-08`

## Objetivo

Crear la entrada `/admin/*` autenticada por OIDC y autorizada en el servidor contra
la autoridad global persistida. La frontera deberá fallar cerrada, responder sin
enumerar usuarios o permisos y ofrecer una primera superficie Jakarta Faces
responsive para las historias administrativas posteriores.

## Alcance

- contrato neutral para resolver autoridad global desde una identidad OIDC ya
  validada por el contenedor;
- resolución actual de usuario local activo, roles globales activos y permisos
  conocidos, sin cachear autorización en sesión;
- auditoría de acceso permitido o denegado con correlación generada por el servidor;
- constraint OIDC y filtro de autorización server-side sobre `/admin/*`;
- frontera web reutilizable para exigir cualquier permiso global o uno específico;
- landing Faces mínima que muestra únicamente áreas permitidas;
- Material Design 3 responsive sobre JSF, con foco visible, semántica y estados
  seguros;
- enlaces de regreso al workspace y logout OIDC existente.

## Límites

- no se exponen endpoints REST administrativos;
- no se implementan todavía altas o mutaciones de empresas, plugins, usuarios,
  roles o permisos;
- no se consulta JPA desde filtros, beans ni XHTML;
- no se confía en headers, parámetros, roles de Keycloak ni roles empresariales;
- no se guarda una lista de permisos en sesión;
- no se revela si una identidad local, rol o permiso concreto existe.

## Criterios de aceptación

- **CA-01:** `/admin/*` está incluido en el constraint OIDC del WAR.
- **CA-02:** cada request administrativo vuelve a resolver el usuario local y sus
  permisos globales actuales mediante un puerto de aplicación.
- **CA-03:** entrar al área exige al menos un permiso global efectivo conocido.
- **CA-04:** la frontera permite exigir un permiso exacto para cada futura pantalla
  o comando y deniega por defecto.
- **CA-05:** usuario ausente, inactivo, sin permisos o con contexto inconsistente
  recibe una respuesta genérica `401/403`, `no-store`, sin IDs ni diagnósticos.
- **CA-06:** el servidor audita autorización y denegación con correlación propia,
  permiso requerido e ID local solo cuando existe; nunca issuer, subject o token.
- **CA-07:** la landing no muestra áreas para las que el actor carece de permiso.
- **CA-08:** no se agrega una API administrativa pública ni lógica de dominio al
  backing bean.
- **CA-09:** la pantalla usa JSF, tokens Material Design 3 y layouts utilizables a
  375, 720 y 1280 px.
- **CA-10:** futuras mutaciones deberán reautorizar el permiso exacto en la acción;
  ocultar navegación no se considera autorización.

## Respuesta segura

El filtro devuelve únicamente “Acceso no disponible” para el navegador. Los códigos
internos quedan restringidos a la auditoría estructurada y no cambian el cuerpo por
identidad, usuario, estado o permiso. La autenticación continúa siendo
responsabilidad de WildFly OIDC antes de la autorización local.

## Matriz de pruebas ejecutada en J11-S4-08

| Nivel | Comprobación | Estado |
|---|---|---|
| JUnit | resolución positiva/negativa y permiso exacto | Verde |
| JUnit | auditoría sin datos sensibles y correlación válida | Verde |
| Servlet | filtro, `401/403`, headers y ausencia de enumeración | Verde |
| Integración OIDC | anónimo, usuario empresarial y administrador global | Verde |
| Integración | revocación efectiva sin nueva sesión | Verde |
| ArchUnit | bean/filtro sin JPA ni dominio de negocio | Verde |
| Playwright | landing a 375/720/1280 y navegación por permisos | Verde |
| Maven | `mvn verify` acumulado | Verde |

La matriz acumulada de `J11-S4-08` quedó verde sin omisiones.

## Documentación afectada

- arquitectura general y ADR-0009;
- guía de implementación;
- runbook del shell y Compose/OIDC;
- backlog, Sprint 4 e índices;
- evidencia específica de la historia.

## Resultado del corte

Se implementaron el contrato neutral de acceso, la auditoría con correlación, el
adaptador de lectura JTA, la guarda web para rutas directas y prefijadas por Faces,
el constraint OIDC y la landing responsive. El detalle verificable se conserva en
[la evidencia de J11-S4-04](../../evidence/J11-S4-04-frontera-web-administrativa-confiable.md).

La historia quedó completada y validada junto con la UI administrativa posterior.
