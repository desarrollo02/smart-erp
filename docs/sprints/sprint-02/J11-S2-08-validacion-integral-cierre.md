# J11-S2-08 — Validación integral y cierre del Sprint 2

- Estado: Completada con validación independiente diferida por decisión de producto
- Dependencia: `J11-S2-07` completada y verde
- Fecha de inicio: 2026-07-27
- Fecha del gate técnico: 2026-07-28
- Fecha de cierre: 2026-07-28

## Objetivo

Certificar desde limpio que empresas, activación persistida, personalización obligatoria, composición de pantallas, dependencias, filtrado, JPA/JTA y operación en contenedores forman un baseline coherente sin degradar la fundación del Sprint 1, y publicar la primera guía que permita a un implementador aprender a preparar el ERP para una empresa.

## Alcance

- trazabilidad completa de `J11-S2-00` a `J11-S2-07`;
- G0, Maven Enforcer, unitarias, integración, ArchUnit y reproducibilidad;
- PostgreSQL Testcontainers para repositorios y aislamiento;
- migración desde V1 y creación desde base vacía;
- JPA `validate`, JTA, rollback, idempotencia y concurrencia;
- matriz de al menos dos empresas, estados activo/inactivo/ausente y personalizaciones exclusivas;
- composición con y sin plugins funcionales y de personalización de referencia;
- contrato neutral de pantalla, overlay válido e inválido y orden final;
- build Docker, Compose, health y persistencia tras recreación;
- auditoría de WAR, imágenes, logs, secretos y recursos temporales;
- primera edición utilizable de la [Guía de implementación del ERP por empresa](../../implementation-guide/README.md), con recorrido didáctico y ejemplo ficticio completo;
- validación del recorrido por una persona que no haya implementado las capacidades explicadas;
- regeneración y verificación visual del PDF de estructura del repositorio contra el baseline final del Sprint 2;
- retrospectiva, riesgos y siguiente backlog.

## Estrategia de integración sin superficie insegura

La activación y asignación de personalizaciones no se expondrán mediante un endpoint de producción. La prueba integral puede usar los servicios directamente con Testcontainers y, si WildFly requiere un punto de entrada para demostrar JTA/CDI, un arnés exclusivo de pruebas. Ese arnés debe pertenecer a módulos de tests o a un perfil explícito, estar ausente del WAR normal y quedar verificado por inspección binaria.

## Escenarios mínimos

1. build predeterminado sin plugin funcional opcional;
2. build con plugin funcional de referencia y las personalizaciones de referencia esperadas;
3. base vacía aplica V1 y V2;
4. volumen V1 actualiza solo a V2;
5. dos empresas guardan decisiones opuestas para el plugin funcional y personalizaciones propias distintas;
6. reinicio y recreación conservan empresas, activaciones y asignaciones;
7. empresa inactiva, plugin ausente y fila ausente deniegan;
8. personalización ausente, incompatible o asignada a otra empresa produce el comportamiento seguro del ADR;
9. dependencias requeridas bloquean transiciones inválidas;
10. filtrado, guarda y composición de pantalla coinciden para cada empresa;
11. el overlay válido se aplica al final y un overlay inválido no se aplica parcialmente;
12. health detecta esquema desactualizado y se recupera después del migrador.
13. un implementador puede seguir la guía desde los prerrequisitos hasta una empresa validada sin depender de pasos tácitos;
14. el ejemplo ficticio demuestra selección de plugins, personalización exclusiva, extensión permitida de pantalla, pruebas, despliegue y rollback.
15. el PDF de estructura refleja el árbol, la arquitectura, el estado y los pendientes finales del Sprint 2 y supera la revisión visual completa.

## Fuera de alcance

- login/OIDC, usuario, rol y autorización HTTP;
- UI renderizada y Playwright;
- primer plugin empresarial productivo;
- migraciones `plg_*`;
- publicación o despliegue en producción;
- documentación definitiva de identidad, UI renderizada, dominios e integraciones todavía no implementados;
- commit o pull request sin autorización.

## Criterios de aceptación

- **CA-01:** todas las historias previas están completadas y enlazadas con evidencia.
- **CA-02:** G0 no encuentra UTF-8 inválido, metadatos incoherentes ni enlaces rotos.
- **CA-03:** `mvnw.cmd -B clean verify` termina verde sin omitidas.
- **CA-04:** ArchUnit y Enforcer preservan límites y convergencia.
- **CA-05:** Testcontainers valida SQL, repositorios, transacciones y aislamiento sobre PostgreSQL real.
- **CA-06:** V1→V2, base vacía, reejecución y checksum terminan según contrato.
- **CA-07:** JPA valida y nunca genera DDL.
- **CA-08:** dos empresas conservan activaciones, personalizaciones y contribuciones diferentes sin filtración cruzada.
- **CA-09:** dependencias, idempotencia, concurrencia, sustitución y rollback tienen casos positivos/negativos verdes.
- **CA-10:** las variantes del WAR son reproducibles y contienen exactamente sus dependencias funcionales y personalizaciones esperadas.
- **CA-11:** cualquier arnés de integración está ausente del WAR normal.
- **CA-12:** imágenes y Compose arrancan, migran, quedan listas y conservan datos al recrearse.
- **CA-13:** respuestas públicas y logs no exponen secretos, SQL ni información de otra empresa.
- **CA-14:** contenedores, redes, volúmenes y fixtures efímeros se limpian; imágenes de evidencia quedan identificadas.
- **CA-15:** pruebas no aplicables y riesgos residuales se declaran expresamente.
- **CA-16:** el contrato de pantalla rechaza referencias y operaciones no autorizadas sin aplicar resultados parciales.
- **CA-17:** cada empresa operativa utiliza exactamente su personalización y esta aparece después de todos los plugins funcionales.
- **CA-18:** Sprint 2 solo se cierra con todos los gates verdes y siguiente trabajo definido.
- **CA-19:** existe una primera edición versionada de la guía para implementadores, enlazada desde los índices y coherente con el baseline real.
- **CA-20:** la guía explica conceptos, decisiones y límites antes de presentar procedimientos y comandos.
- **CA-21:** cubre relevamiento, clasificación de requisitos, empresa, plugins, personalización, datos, despliegue, validación, diagnóstico, rollback y entrega.
- **CA-22:** contiene un ejemplo ficticio de extremo a extremo sin secretos, datos reales ni acceso a internos de plugins.
- **CA-23:** un implementador ajeno al desarrollo valida el recorrido en limpio y sus hallazgos se corrigen o quedan documentados.
- **CA-24:** versión, compatibilidad y política de actualización convierten la guía en un entregable vivo de los siguientes Sprints.
- **CA-25:** `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` se regenera después del último cambio, identifica Sprint 2 y queda certificado por páginas, tamaño, SHA-256, metadatos, extracción de texto y revisión visual de todas sus páginas.

## Gates

1. G0 y matriz de trazabilidad;
2. `mvnw.cmd -B clean verify`;
3. reproducibilidad y composición de todas las variantes requeridas;
4. Testcontainers y casos G5;
5. imágenes, Compose, V1→V2, health y persistencia;
6. recorrido independiente de la guía para implementadores;
7. seguridad, personalización, limpieza, evidencia y cierre.
8. G6: regeneración y revisión integral del PDF de estructura del repositorio.

## Resultado final

- gates técnicos, PostgreSQL, JTA, WAR, imágenes, Compose, seguridad y limpieza verdes;
- guía para implementadores publicada como `1.0-rc1` y aceptada para continuar;
- `CA-23` no fue ejecutada: se difiere expresamente hasta la demo visual y permanece como gate de esa entrega;
- retrospectiva, siguiente incremento y G6 documentados en la evidencia de cierre;
- ninguna prueba se declara ejecutada sin haberlo sido.

## Siguiente paso

La decisión de producto del 2026-07-28 difiere la [ficha de validación independiente](../../implementation-guide/VALIDATION.md) hasta la demo visual y acepta continuar con la guía `1.0-rc1`. La validación diferida permanece como gate obligatorio de aceptación de la futura demo. El siguiente trabajo permitido es `J11-S3-00`: planificar identidad confiable y la primera UI demostrable antes de iniciar código del nuevo Sprint.

Evidencia técnica provisional: [J11-S2-08 — Validación integral y cierre](../../evidence/J11-S2-08-validacion-integral-cierre.md).
