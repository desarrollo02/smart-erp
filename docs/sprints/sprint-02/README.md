# Sprint 2 — Kernel multiempresa, activación y personalización

- Estado: Completado con validación independiente diferida hasta la demo visual
- Fecha de planificación: 2026-07-27
- Fecha de cierre: 2026-07-28
- Duración propuesta: 3 semanas
- Dependencia: Sprint 1 completado y verde

## Objetivo

Demostrar que el kernel puede representar empresas, persistir la activación de plugins por empresa y exigir exactamente un plugin de personalización propio para cada una. La composición debe aislar empresas, proteger operaciones y aplicar esa personalización como última capa mediante contratos públicos de pantalla, sin introducir todavía autenticación, una UI renderizada ni lógica empresarial productiva.

## Backlog ordenado

| Historia | Resultado esperado | Estado |
|---|---|---|
| [`J11-S2-00`](J11-S2-00-gobierno-planificacion.md) | Gobierno, épica y backlog verificable | Completado |
| [`J11-S2-01`](J11-S2-01-adr-contexto-activacion.md) | ADR e invariantes de empresa, contexto, activación y personalización | Completada |
| [`J11-S2-02`](J11-S2-02-modelo-neutral-multiempresa.md) | Contratos y modelo neutral multiempresa | Completada |
| [`J11-S2-03`](J11-S2-03-migracion-core-v2.md) | Migración `core` V2 y evolución segura del migrador | Completada |
| [`J11-S2-04`](J11-S2-04-persistencia-jpa-jta.md) | Persistencia JPA/JTA y repositorios PostgreSQL | Completada |
| [`J11-S2-05`](J11-S2-05-casos-uso-guardas-activacion.md) | Casos de uso y guardas de activación | Completada |
| [`J11-S2-06`](J11-S2-06-filtrado-contribuciones-empresa.md) | Filtrado de contribuciones por empresa | Completada |
| [`J11-S2-07`](J11-S2-07-contrato-personalizacion-pantallas.md) | Contrato y composición de personalizaciones de pantalla | Completada |
| [`J11-S2-08`](J11-S2-08-validacion-integral-cierre.md) | Pruebas integrales, guía para implementadores, evidencias y cierre | Completada con excepción documentada |

No se inicia una historia mientras su dependencia inmediata tenga criterios o pruebas pendientes.

## Alcance del Sprint

- decisiones arquitectónicas y de datos para identidad y ciclo de vida de empresa;
- contratos neutrales de contexto empresarial;
- modelo neutral de activación deseada y efectiva;
- categoría explícita de plugin funcional o de personalización;
- asignación obligatoria y exclusiva de un plugin de personalización por empresa;
- migraciones versionadas del esquema `core`;
- datasource JTA, unidad de persistencia `logixone-core-pu` y repositorios del kernel;
- integración PostgreSQL real mediante Testcontainers donde corresponda;
- casos de uso transaccionales para consultar y cambiar activación;
- validación de dependencias requeridas al activar y desactivar;
- filtrado de capacidades, permisos y menús por empresa;
- orden global donde los plugins funcionales se componen antes de la personalización empresarial;
- contratos neutrales y versionados de pantalla, slots, elementos y overlays autorizados;
- guarda de aplicación que deniega operaciones de plugins no efectivos;
- plugins funcional y de personalización de referencia para probar dos empresas, aislamiento y compatibilidad;
- Docker, Compose, migración desde V1, persistencia, health y cierre documental.
- guía versionada para que un implementador aprenda a relevar, configurar, personalizar, desplegar y validar el ERP para una empresa.
- PDF actualizado de estructura del repositorio, generado y revisado visualmente como entregable obligatorio del cierre.

## Fuera de alcance

- login, usuario, rol, sesión y proveedor de identidad;
- aceptar un header HTTP como identidad empresarial confiable;
- endpoints administrativos o funcionales públicos;
- renderizado real con Jakarta Faces/PrimeFaces o cualquier UI navegable; el contrato neutral de personalización sí está incluido;
- primer dominio ERP;
- persistencia propia del plugin de referencia;
- personalizaciones productivas no visuales como reportes, cálculos, flujos o integraciones;
- descubrimiento de migraciones de plugins;
- borrado irreversible de empresas, activaciones o datos.

## Criterios de éxito del Sprint

- **CS-01:** existe un ADR aceptado para identidad empresarial, ciclo de vida, contexto, estado de activación y concurrencia.
- **CS-02:** `kernel-api`, dominio y aplicación conservan neutralidad respecto de Jakarta y persistencia.
- **CS-03:** la base evoluciona de V1 a V2 y también se crea desde cero, sin alterar migraciones aplicadas.
- **CS-04:** JPA usa `logixone-core-pu`, datasource JTA y validación de esquema sin generación automática.
- **CS-05:** repositorios y casos de uso aíslan estrictamente por empresa.
- **CS-06:** activar exige dependencias requeridas activas en la misma empresa.
- **CS-07:** desactivar se rechaza si rompe un plugin dependiente activo.
- **CS-08:** una fila ausente, empresa inactiva o plugin físicamente ausente nunca produce activación efectiva.
- **CS-09:** menús, permisos y capacidades solo incluyen plugins efectivos para la empresa consultada.
- **CS-10:** una guarda de aplicación deniega antes de ejecutar una operación cuando el plugin no es efectivo.
- **CS-11:** dos empresas pueden observar resultados distintos sobre el mismo catálogo físico sin filtración cruzada.
- **CS-12:** Maven, ArchUnit, Testcontainers, ambas variantes del WAR, Docker/Compose, migraciones, health y persistencia quedan verdes y documentados.
- **CS-13:** cada empresa operativa tiene exactamente una personalización propia, compatible y efectiva.
- **CS-14:** la personalización asignada se aplica después de todos los plugins funcionales y las personalizaciones de otras empresas no son visibles.
- **CS-15:** una pantalla solo puede modificarse mediante contratos públicos, versionados y declarados por su plugin propietario.
- **CS-16:** ninguna personalización puede importar internos, acceder a tablas ajenas, reemplazar recursos arbitrariamente ni relajar autorización, validación o auditoría del servidor.
- **CS-17:** una guía validada por un implementador ajeno al desarrollo explica el recorrido completo para implementar el ERP en una empresa y evoluciona con cada baseline.
- **CS-18:** el cierre regenera el PDF de estructura contra el baseline final, revisa visualmente todas sus páginas y registra páginas, tamaño y SHA-256.

## Gates obligatorios

| Gate | Resultado requerido |
|---|---|
| G0 | UTF-8, enlaces, ADR, criterios y trazabilidad sin brechas |
| G1 | pruebas del módulo afectado después de cada cambio coherente |
| G2 | `mvnw.cmd -B clean verify` verde |
| G3 | ArchUnit y WAR con/sin plugin correctamente compuestos |
| G4 | PostgreSQL real, V1→V2, base vacía, JPA `validate`, imágenes y Compose verdes |
| G5 | aislamiento entre empresas, activación positiva/negativa, personalización final, filtrado y guarda probados |
| G6 | PDF de estructura regenerado, renderizado, revisado y registrado por checksum |

## Política sobre pruebas aún no aplicables

- Playwright continúa fuera de alcance porque todavía no habrá UI renderizada; será obligatorio al implementar el adaptador Jakarta Faces/PrimeFaces.
- Seguridad HTTP/OIDC se incorporará cuando exista identidad en Sprint 3.
- La ausencia de endpoint público en Sprint 2 es deliberada: no se abrirá una superficie administrativa sin autorización.
- Testcontainers sí pasa a ser obligatorio para repositorios y SQL PostgreSQL de este Sprint.

## Riesgos principales

- decidir tarde la identidad de empresa podría forzar una migración incompatible;
- mezclar contexto de empresa con autenticación futura puede acoplar Sprint 2 a un proveedor OIDC;
- una política implícita para filas ausentes puede habilitar plugins accidentalmente;
- probar JPA solo fuera de WildFly podría ocultar diferencias JTA, por lo que el cierre exige una validación runtime adicional;
- el catálogo físico y la activación persistida pueden divergir tras retirar plugins; el comportamiento debe quedar explícito en el ADR.
- un mecanismo libre de reemplazo de XHTML, CSS o beans recrearía el acoplamiento legado; solo se admitirán extensiones tipadas y publicadas;
- la ausencia o incompatibilidad de la personalización obligatoria puede afectar disponibilidad global o solo a una empresa; `J11-S2-01` debe decidirlo antes del código;
- el nuevo alcance aumenta el Sprint propuesto de dos a tres semanas y requiere una historia propia antes del cierre.
- una guía escrita solo desde la perspectiva de sus autores puede omitir conocimiento tácito; el cierre exige una ejecución independiente del recorrido.

## Próxima historia

`J11-S2-08` quedó cerrado con la excepción de producto documentada. El siguiente trabajo autorizado es `J11-S3-00`: planificar identidad confiable y la primera UI demostrable. El recorrido independiente y la promoción de la guía a `1.0` pasan a ser gates obligatorios de aceptación de esa demo.

## Planificación completada

- `J11-S2-00`: 10 de 10 criterios cumplidos.
- La planificación original enlazó nueve historias, 16 criterios globales y 147 criterios de aceptación de historias.
- Las adendas de guía para implementadores y PDF obligatorio de cierre elevan el plan vigente a 18 criterios globales y 154 criterios de aceptación, sin alterar la secuencia de historias.
- Evidencia: [Planificación del Sprint 2](../../evidence/J11-S2-00-planificacion-sprint-02.md).
- Backlog y ADR-0005 aceptados el 2026-07-27; `J11-S2-02` se completó ese mismo día.
- `J11-S2-02`: 16 de 16 criterios cumplidos, 83 pruebas verdes y ambas composiciones del WAR verificadas.
- Evidencia: [Modelo neutral multiempresa](../../evidence/J11-S2-02-modelo-neutral-multiempresa.md).
- `J11-S2-03`: 15 de 15 criterios cumplidos; 85 pruebas del build limpio, 7 escenarios PostgreSQL reales y tres trayectorias Compose verificadas.
- Evidencia: [Migración `core` V2](../../evidence/J11-S2-03-migracion-core-v2.md).
- `J11-S2-04`: 16 de 16 criterios cumplidos; 99 pruebas en el gate limpio con PostgreSQL, 4 pruebas runtime REST/JTA y ambas composiciones del WAR verificadas.
- Evidencia: [Persistencia JPA/JTA](../../evidence/J11-S2-04-persistencia-jpa-jta.md).
- `J11-S2-05`: 17 de 17 criterios cumplidos; 111 pruebas en el gate limpio con PostgreSQL, 6 pruebas runtime REST/JTA, imagen real y ambas composiciones del WAR verificadas.
- Evidencia: [Casos de uso y guardas de activación](../../evidence/J11-S2-05-casos-uso-guardas-activacion.md).
- `J11-S2-06`: 16 de 16 criterios cumplidos; 118 pruebas en el gate limpio con PostgreSQL, 6 pruebas runtime REST/JTA sin omisiones, 2 imágenes OCI y ambas variantes arrancadas en Compose.
- Evidencia: [Filtrado de contribuciones por empresa](../../evidence/J11-S2-06-filtrado-contribuciones-empresa.md).
- `J11-S2-07`: 18 de 18 criterios cumplidos; 136 pruebas en el gate limpio con PostgreSQL, 6 pruebas runtime REST/JTA sin omisiones, matriz WAR de 0/1/3 plugins y 2 imágenes OCI verificadas en Compose.
- Evidencia: [Contrato y composición de personalizaciones de pantalla](../../evidence/J11-S2-07-contrato-personalizacion-pantallas.md).

## Cierre de J11-S2-08

- gate limpio: 16 de 16 módulos, 136 pruebas, 0 fallos, 0 errores y 0 omitidas;
- WAR base, referencia y pantallas reproducibles, con composición física 0/1/3;
- PostgreSQL 18.4, base vacía, V1→V2, JPA `validate`, JTA, rollback y dos empresas aisladas verdes;
- tres imágenes candidatas y tres proyectos Compose verificados;
- persistencia idéntica después de recrear contenedores sin retirar el volumen;
- auditoría de WAR, imágenes y logs sin arnés, librerías provistas, secretos ni SQL;
- recursos efímeros retirados: 0 contenedores, 0 volúmenes y 0 redes residuales;
- guía `1.0-rc1` con 14 capítulos, ejemplo Distribuidora Boreal y [ficha independiente](../../implementation-guide/VALIDATION.md).

Evidencia: [Validación integral y cierre del Sprint 2](../../evidence/J11-S2-08-validacion-integral-cierre.md).

El responsable de producto decidió el 2026-07-28 trasladar el recorrido independiente de `CA-23` a la aceptación de la demo visual. No se considera ejecutado: queda registrado en su ficha y como excepción de este cierre. G0 final y el PDF G6 completan el resto del corte.

## Retrospectiva

- La secuencia por dependencias permitió evolucionar desde contratos puros hasta PostgreSQL, JTA y pantallas sin mezclar responsabilidades.
- Los arneses opt-in demostraron comportamiento runtime sin abrir endpoints administrativos inseguros en producción.
- Firmar el contenido persistido antes y después de recrear contenedores produjo evidencia más fuerte que comprobar solo conteos.
- Los perfiles 0/1/3 demostraron que presencia física, activación por empresa y personalización exclusiva son decisiones distintas.
- Las mayores fricciones operativas fueron las comillas de plantillas Docker bajo PowerShell y recordar nombres físicos de columnas; ambas se resolvieron consultando metadatos antes de cualquier acción destructiva.
- La ausencia de metadata Git sigue impidiendo certificar diff, commit y procedencia del árbol.
- La revisión escrita por sus propios autores no sustituye la experiencia de un implementador nuevo; por eso la validación independiente permanece pendiente como gate de la demo, aunque el Sprint 2 se cierre con la excepción registrada.

## Siguiente incremento propuesto

La dependencia más fuerte para una superficie pública segura es identidad autenticada. El valor de producto inmediato solicitado es una versión demostrable con interfaz. Por ello, `J11-S3-00` deberá planificar un Sprint 3 que, si sus ADR y estimaciones lo permiten, ordene:

1. proveedor de identidad/OIDC, usuarios, membresía empresarial y roles;
2. contexto empresarial confiable derivado de identidad autenticada;
3. autorización de aplicación y auditoría con actor real;
4. shell UI mínimo que renderice navegación efectiva por empresa;
5. primera pantalla compuesta por contrato y pruebas Playwright;
6. cierre integral y demo reproducible.

La UI no debe adelantarse al contexto confiable ni presentar un dominio ERP ficticio como funcionalidad productiva. Esta continuidad se materializó posteriormente: `J11-S3-00` quedó completada el 2026-07-28, ADR-0006 fue aceptado y el Sprint 3 comenzó con `J11-S3-01` como siguiente historia.
