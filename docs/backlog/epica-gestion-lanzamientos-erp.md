# Épica — Gestión de lanzamientos, mejoras y correcciones

- Estado: Planificada como plugin funcional `release_management`
- Fecha: 2026-08-04
- Perfil: operaciones centrales del proveedor
- Decisión: [ADR-0036](../adr/0036-operaciones-proveedor-soporte-lanzamientos-conector.md)
- Prioridad: gate previo a publicar una versión soportada; no modifica Sprint 8

## Objetivo

Gobernar el ciclo de una mejora o corrección desde su aceptación hasta una versión
publicada, relacionando contenido, compatibilidad, evidencias, aprobaciones, notas
y artefactos verificables sin reemplazar Git, Maven, CI/CD, Docker o el instalador.

## Alcance inicial

- defectos, mejoras, cambios técnicos y correcciones de seguridad;
- componente/plugin, severidad, prioridad, estado y responsable;
- versiones planificadas y candidatos;
- contenido y notas de versión;
- matriz de compatibilidad y migraciones requeridas;
- gates, evidencia, aprobación, publicación y retiro;
- metadatos de artefactos, digest, SHA-256 y firma;
- canales interno, piloto, estable y mantenimiento;
- eventos públicos para informar corrección y release disponible;
- consola Jakarta Faces Material Design 3 responsive.

## Límites

- no poseer repositorios, ramas, commits ni código fuente;
- no almacenar imágenes o instaladores como blobs de negocio;
- no compilar, firmar, promover, instalar o revertir en el primer alcance;
- no marcar verde un gate por intervención manual sin evidencia y autorización;
- no acceder a casos de soporte ni instalaciones mediante SQL o entidades ajenas;
- no convertir canales, severidades o estados en catálogos empresariales libres.

El esquema previsto es `plg_release_management`. El plugin puede operar sin
`customer_support`; su API será dueña de los comandos de ingreso de cambio y de los
eventos de publicación consumidos por otros plugins.

## Historias propuestas

| Historia | Resultado |
|---|---|
| REL-00 | confirmar versionado, canales, estados, severidad, fuente de verdad, gates y política de soporte |
| REL-01 | crear `release-management-api`, descriptor, dominio neutral y contratos de cambio/release |
| REL-02 | crear esquema privado, migraciones, repositorios, historial y concurrencia |
| REL-03 | implementar ingreso, priorización, planificación y contenido de candidatos |
| REL-04 | implementar compatibilidad, gates, evidencias, aprobaciones y segregación de funciones |
| REL-05 | registrar artefactos por URI/digest/checksum/firma y publicar notas/eventos |
| REL-06 | implementar consola responsive, filtros, comparación y accesibilidad |
| REL-07 | integrar simuladores de pipeline/instalador, ejecutar gates, demo, manuales y PDF |

## Criterios de aceptación

- **REL-CE01:** cada cambio tiene identidad estable, tipo, componente, severidad,
  prioridad, estado e historial append-only.
- **REL-CE02:** una versión publicada conserva inmutablemente contenido, notas,
  compatibilidad, artefactos y evidencias aplicadas.
- **REL-CE03:** no se publica si falta un gate obligatorio, aprobación, digest,
  checksum o compatibilidad exigida.
- **REL-CE04:** crear, verificar, aprobar, publicar y retirar requieren permisos
  separados y auditoría.
- **REL-CE05:** repetir importación o callback de pipeline no duplica evidencia,
  artefacto, cambio ni evento.
- **REL-CE06:** una corrección de seguridad puede restringir detalle y audiencia
  sin esconder el estado de soporte de la versión.
- **REL-CE07:** retirar una versión no elimina su historia ni modifica una release
  anterior.
- **REL-CE08:** el plugin guarda URI y metadatos verificables; no copia secretos ni
  binarios del repositorio de artefactos.
- **REL-CE09:** `customer_support` puede presentar una solicitud y consumir
  `ChangeFixed`/`ReleasePublished` sin crear una dependencia inversa.
- **REL-CE10:** el sistema distingue versión planificada, candidato construido,
  artefacto validado y release publicada.
- **REL-CE11:** la interfaz cubre 375, 720 y 1280 px, teclado, foco, vacío, error,
  gate fallido, incompatibilidad y acceso denegado.
- **REL-CE12:** desactivar o retirar el plugin conserva cambios, releases,
  evidencias y eventos.

## Decisiones pendientes antes de código

- esquema de versiones para producto, kernel y plugins;
- sistema fuente para defectos y mejoras si se integra una herramienta externa;
- canales, política LTS y duración de soporte;
- gates obligatorios por clase de release y segregación de aprobación;
- contrato de compatibilidad, migración y rollback;
- ubicación, autenticación y retención del repositorio de artefactos;
- tratamiento y divulgación responsable de correcciones de seguridad.

No se inicia esta épica durante Sprint 8. REL-00 debe decidir primero las fuentes
de verdad para evitar duplicar un gestor de código o un pipeline.

