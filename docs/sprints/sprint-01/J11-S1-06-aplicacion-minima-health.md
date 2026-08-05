# J11-S1-06 — Aplicación mínima y endpoints semánticos de salud

- Fecha de inicio: 2026-07-24
- Estado: Completada
- Dependencia: `J11-S1-05` completada y verde

## Objetivo

Convertir el WAR desplegable en una aplicación Jakarta REST mínima que exponga liveness y readiness propios de Logixone. Los probes deben distinguir entre un proceso vivo y una instancia capaz de recibir tráfico, reemplazando el control temporal que solo comprueba la raíz HTTP de WildFly.

## Decisiones del corte

- Los endpoints serán `GET /logixone/health/live` y `GET /logixone/health/ready`.
- Liveness solo afirma que el WAR y Jakarta REST pueden responder; no depende de PostgreSQL.
- Readiness agrega comprobaciones deterministas de configuración, catálogo de plugins, PostgreSQL y migraciones `core`.
- Readiness responde `200` y `UP` únicamente cuando todas las comprobaciones están verdes; cualquier fallo responde `503` y `DOWN`.
- Las respuestas usan JSON estable, no contienen secretos, URL JDBC, nombres de usuario, rutas internas, mensajes de excepción ni stack traces.
- La lógica de agregación permanece en `kernel-application` y no depende de Jakarta.
- Los adaptadores de entorno, archivo de secreto, JDBC y CDI pertenecen a `kernel-infrastructure-jakarta`.
- El endpoint HTTP pertenece a `web-shell`; la distribución conserva únicamente composición física.
- La aplicación solo valida migraciones. El contenedor `migrator` continúa siendo el único responsable de ejecutarlas.
- Compose usará readiness para su único health check de aplicación. Liveness quedará disponible para operación, pruebas y futuros orquestadores.

## Alcance

- Modelo neutral de estados, resultados y reporte de salud.
- Agregador neutral que transforma excepciones de checks en estado `DOWN` sin divulgar detalles.
- Bootstrap Jakarta REST.
- Recursos HTTP de liveness y readiness.
- Checks CDI del catálogo, configuración externa, conectividad PostgreSQL e historial de Flyway `core`.
- Driver PostgreSQL empaquetado como dependencia de runtime de la aplicación, con versión centralizada.
- Actualización de Docker Compose, runbooks, arquitectura y evidencia.
- Pruebas unitarias, de arquitectura, empaquetado, WildFly, PostgreSQL y Compose.

## Fuera de alcance

- Login, autorización, empresa activa o activación de plugins por empresa.
- UI Jakarta Faces, página principal o lógica empresarial.
- Métricas, tracing, dashboards o alertas.
- Ejecución o reparación automática de migraciones desde el WAR.
- Contribuciones de health en `plugin-api`.
- Configuración de unidades JPA o persistencia empresarial.

## Criterios de aceptación

- **CA-01:** `kernel-application` define un modelo inmutable y Jakarta-free para estado, resultado, reporte y checks de readiness.
- **CA-02:** el agregador entrega resultados ordenados y marca `DOWN` si un check devuelve fallo o lanza una excepción.
- **CA-03:** `GET /logixone/health/live` responde `200`, JSON y `UP` cuando el WAR está desplegado, sin consultar PostgreSQL.
- **CA-04:** `GET /logixone/health/ready` responde `200` únicamente cuando todos los checks están `UP`; de lo contrario responde `503`.
- **CA-05:** readiness comprueba que el catálogo CDI terminó de inicializar y acepta correctamente una distribución sin plugins.
- **CA-06:** la configuración exige URL JDBC PostgreSQL sin credenciales embebidas, usuario y archivo de secreto legible; los errores no exponen valores.
- **CA-07:** readiness comprueba conectividad con PostgreSQL mediante un timeout acotado.
- **CA-08:** readiness valida la migración `core` esperada sin ejecutar ni modificar migraciones.
- **CA-09:** las respuestas públicas solo exponen nombres de checks y estados controlados; registran fallos con tipo seguro, sin mensaje ni secreto.
- **CA-10:** Compose consulta el endpoint readiness con `curl --fail` y conserva el orden PostgreSQL saludable, migrador exitoso, aplicación lista.
- **CA-11:** liveness continúa `UP` y readiness pasa a `DOWN` cuando PostgreSQL deja de estar disponible.
- **CA-12:** las distribuciones con y sin `reference-plugin` compilan, arrancan y quedan listas.
- **CA-13:** las reglas ArchUnit continúan verdes y los módulos neutrales no incorporan Jakarta, JDBC ni implementaciones del servidor.
- **CA-14:** pruebas de módulo, `mvn verify`, inspección del WAR, imágenes, Compose, health y casos negativos quedan verdes y documentados.

## Secuencia de implementación y gates

1. Modelo neutral y pruebas de `kernel-application`.
2. Configuración y probes de infraestructura con pruebas unitarias seguras.
3. Jakarta REST y pruebas del recurso.
4. Gate ArchUnit y build del WAR presente/ausente.
5. Imagen, Compose, liveness/readiness y caída controlada de PostgreSQL.
6. `mvnw.cmd -B clean verify`, evidencia y cierre documental.

No se inicia `J11-S1-07` mientras cualquiera de estos gates permanezca rojo.

## Resultado final

- Los 14 criterios de aceptación quedaron cumplidos.
- El gate local `mvnw.cmd -B clean verify` terminó con 14/14 módulos y 56 pruebas, sin fallos, errores ni omitidas.
- El perfil runtime ejecutó 2 pruebas REST Assured adicionales contra WildFly y Compose reales.
- Las variantes sin plugin y con `reference_plugin@1.0.0` quedaron saludables con los cuatro checks de readiness en `UP`.
- Al detener PostgreSQL, liveness permaneció `200 UP` y readiness respondió `503 DOWN`; al restaurarlo, readiness volvió a `200 UP` sin reiniciar la aplicación.
- La evidencia reproducible está en [J11-S1-06 — Aplicación mínima y health semántico](../../evidence/J11-S1-06-aplicacion-minima-health.md).

`J11-S1-06` queda cerrada y habilita `J11-S1-07`.
