# J11-S1-07 — Validación integral y cierre del Sprint 1

- Fecha de inicio: 2026-07-27
- Estado: Completada
- Dependencia: `J11-S1-06` completada y verde

## Objetivo

Certificar desde un estado limpio que la fundación técnica construida durante el Sprint 1 es coherente, reproducible y operable como un único baseline. El cierre debe correlacionar las decisiones, pruebas y evidencias de `J11-S1-01` a `J11-S1-06`, volver a ejecutar los gates integrales y dejar explícitos los riesgos y el siguiente trabajo permitido.

## Decisiones del corte

- Esta historia no agrega capacidades empresariales ni amplía los contratos del kernel o de plugins.
- Los resultados históricos sirven como trazabilidad, pero no sustituyen la repetición final de los gates críticos.
- Se validan obligatoriamente las distribuciones con y sin `reference-plugin`.
- Las pruebas de runtime usan proyectos Compose, puertos y volúmenes aislados; no dependen de recursos creados manualmente.
- Los artefactos se comparan por SHA-256 para comprobar reproducibilidad dentro del mismo ambiente y fuente.
- Un gate fallido detiene la secuencia; se registra y corrige antes de continuar.
- No se publican imágenes, no se despliega a producción y no se crea un commit sin autorización explícita.

## Alcance

- Auditoría documental y trazabilidad de las siete historias técnicas del Sprint.
- Build Maven limpio, pruebas unitarias, integración local y ArchUnit.
- Ensamblado e inspección del WAR con plugin de referencia presente y ausente.
- Comprobación de reproducibilidad de los artefactos Maven.
- Validación estática y construcción de Docker.
- Migración inicial, reejecución idempotente y rechazo de checksum alterado en un entorno aislado.
- Arranque Compose de ambas variantes y pruebas REST Assured contra WildFly real.
- Caída y recuperación controlada de PostgreSQL, salud semántica y conservación de datos.
- Auditoría de secretos, configuración, imágenes, contenedores y volúmenes temporales.
- Evidencia consolidada, riesgos residuales, retrospectiva y cierre formal del Sprint.

## Fuera de alcance

- Login, autorización, empresas, activación de plugins por empresa o dominios ERP.
- UI navegable y Playwright; no existe interfaz de usuario en este baseline.
- Persistencia JPA empresarial y pruebas Testcontainers asociadas a repositorios todavía inexistentes.
- Registro remoto de contenedores, firma de imágenes o promoción entre ambientes reales.
- Planificación detallada o implementación del Sprint 2.

## Criterios de aceptación

- **CA-01:** las historias `J11-S1-00` a `J11-S1-06` están cerradas, enlazadas y respaldadas por evidencia reproducible.
- **CA-02:** la documentación Markdown conserva UTF-8 válido, metadatos coherentes y cero enlaces locales rotos.
- **CA-03:** `mvnw.cmd -B clean verify` termina verde con Java 21 y Maven Wrapper, sin pruebas fallidas, erróneas ni omitidas.
- **CA-04:** ArchUnit y Maven Enforcer mantienen verdes los límites de módulos, Java/Maven y convergencia de dependencias.
- **CA-05:** los WAR sin plugin y con `reference-plugin` se construyen desde limpio y contienen exactamente la composición prevista, sin APIs Jakarta empaquetadas.
- **CA-06:** dos construcciones limpias equivalentes producen artefactos Maven finales con los mismos SHA-256 en el ambiente del cierre.
- **CA-07:** Dockerfile y Compose validan estáticamente, conservan imágenes base fijadas por digest y no incorporan `.tools`, secretos ni configuración local.
- **CA-08:** las imágenes de aplicación de ambas variantes y el migrador se construyen correctamente desde la fuente certificada.
- **CA-09:** PostgreSQL vacío recibe la migración `core`, una segunda ejecución no reaplica cambios y una migración aplicada alterada es rechazada por checksum.
- **CA-10:** las composiciones presente y ausente arrancan en WildFly, quedan `healthy` y publican el catálogo esperado.
- **CA-11:** liveness permanece `UP` durante la caída de PostgreSQL; readiness cambia a `DOWN` y se recupera sin reiniciar la aplicación.
- **CA-12:** REST Assured valida los contratos HTTP reales de liveness/readiness con cero fallos.
- **CA-13:** los datos sobreviven a la recreación controlada de la aplicación y de PostgreSQL cuando se conserva el volumen declarado.
- **CA-14:** los casos negativos no exponen contraseñas, URL JDBC con credenciales, rutas sensibles, mensajes internos ni stack traces en respuestas públicas.
- **CA-15:** la evidencia final registra ambiente, comandos, resultados, fallos/correcciones, checksums, limitaciones y limpieza de recursos temporales.
- **CA-16:** el Sprint queda marcado como completado únicamente con todos los gates verdes y deja como siguiente paso la definición explícita del Sprint 2.

## Secuencia de validación

1. Crear la matriz de trazabilidad y validar documentación.
2. Ejecutar el gate Maven limpio, ArchUnit y las dos composiciones.
3. Comprobar contenido y reproducibilidad de artefactos.
4. Validar y construir Docker.
5. Ejecutar migraciones, persistencia, health y casos negativos en Compose aislado.
6. Ejecutar REST Assured contra el servidor real.
7. Limpiar recursos temporales, consolidar evidencia y cerrar el Sprint.

## Estado inicial

- `J11-S1-06` cerró con 14 de 14 criterios cumplidos, 56 pruebas Maven y 2 pruebas REST Assured verdes.
- El índice del Sprint habilitaba `J11-S1-07`, pero la historia todavía no tenía documento ni criterios propios.
- No existe UI navegable, repositorio JPA empresarial ni metadata Git; esas ausencias se tratarán como límites explícitos, no como pruebas omitidas silenciosamente.

## Resultado final

Los 16 criterios de aceptación quedaron cumplidos. El último `mvnw.cmd -B clean verify`, ejecutado después de consolidar la documentación, terminó 14/14 con 56 pruebas y 4 reglas ArchUnit verdes. G0 confirmó 44 Markdown en UTF-8 estricto, sin caracteres de reemplazo ni enlaces locales rotos.

La evidencia consolidada se encuentra en [J11-S1-07 — Validación integral y cierre del Sprint 1](../../evidence/J11-S1-07-validacion-integral-cierre.md). `J11-S1-07` cierra formalmente el Sprint 1.
