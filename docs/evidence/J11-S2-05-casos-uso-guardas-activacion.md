# J11-S2-05 — Casos de uso y guardas de activación

- Fecha: 2026-07-27
- Estado: Verde; 17 de 17 criterios de aceptación satisfechos
- Ambiente: Windows 11 amd64, Java Temurin 21.0.11+10, Maven Wrapper 3.9.16, Docker Engine 29.6.2, Compose 5.3.1 y WildFly 41
- PostgreSQL probado: 18.4 real, imagen fijada por digest

## Resultado

El kernel dispone de casos de uso neutrales para registrar empresas, cambiar su ciclo de vida, habilitar o deshabilitar plugins funcionales y reemplazar la personalización empresarial. Las operaciones expresan `CHANGED`, `UNCHANGED` o `REJECTED`, preservan idempotencia y transforman conflictos de persistencia en códigos de aplicación estables.

`PluginOperationGuard` consulta la empresa mediante `CompanyContext`, resuelve sus plugins efectivos y deniega antes de ejecutar el callback cuando la empresa está inactiva o el plugin está ausente, desactivado, es una personalización ajena o no pertenece a una composición empresarial operativa.

## Diseño implementado

- `CompanyAdministrationService` registra siempre en `INACTIVE`, valida la personalización física, su categoría y propiedad exclusiva, cambia estado y reemplaza la asignación de forma atómica.
- `PluginActivationService` aplica `PluginActivationPolicy`, exige dependencias requeridas habilitadas en la misma empresa, rechaza dependientes activos y prohíbe desactivar la personalización asignada.
- La idempotencia se decide antes del control de versión: repetir un estado ya satisfecho devuelve `UNCHANGED` sin escritura ni incremento artificial.
- `CompanyOperationResult<T>` mantiene invariantes entre estado, valor y código de rechazo; los códigos propios de orquestación permanecen en aplicación y no alteran el vocabulario de diagnóstico congelado del dominio.
- `CompanyAuditPort` recibe eventos inmutables con identificadores técnicos, operación, resultado, código, versiones, instante UTC y actor. `StructuredCompanyAudit` no registra nombres comerciales, datos personales ni secretos.
- `TransactionalCompanyUseCases` es una fachada CDI interna con cuatro métodos `@Transactional`. No contiene anotaciones Jakarta REST ni existe endpoint administrativo de producción.
- Un fallo de auditoría participa de la misma transacción: no se confirma el cambio empresarial si el evento no puede registrarse.

## Pruebas por incremento

La prueba de aplicación se ejecutó después de cada cambio coherente:

```powershell
.\mvnw.cmd -B -pl kernel-application -am test
```

Resultado final: 25 pruebas en `kernel-application`, incluidas 10 nuevas que cubren alta, exclusividad de personalización, dependencias, idempotencia, versiones, dos empresas, reemplazo, guarda y las invariantes de resultados.

La infraestructura se verificó directamente con PostgreSQL real:

```powershell
.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" `
  -pl kernel-infrastructure-jakarta verify
```

Resultado: 14 unitarias y 7 integraciones PostgreSQL verdes. El escenario agregado usa los repositorios JPA reales, obtiene composiciones distintas para dos empresas y fuerza un fallo de auditoría después de `save`; la transacción revierte y la empresa no queda persistida.

## Gate integral

```powershell
.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" clean verify
```

Resultado: código 0, 14 de 14 módulos y 111 pruebas verdes:

- 97 pruebas del baseline normal, incluidas 6 reglas ArchUnit;
- 7 escenarios de migración sobre PostgreSQL 18.4;
- 7 escenarios JPA y de casos de uso sobre PostgreSQL 18.4.

No se omitió, desactivó ni relajó una prueba. La neutralidad existente de APIs, dominio y aplicación frente a Jakarta/JPA/JDBC permaneció verde; una prueba adicional comprueba que la fachada transaccional no publica REST.

## Prueba JTA dentro de WildFly

El arnés opt-in se reconstruyó y se desplegó únicamente en la composición efímera `logixone-s205`:

```powershell
.\mvnw.cmd -B -Pjta-runtime-harness `
  -pl tests/runtime-persistence-harness -am package

.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:18085" `
  "-Dlogixone.jta-probe=true" verify
```

Resultado: código 0, 6 de 6 pruebas y cero omitidas. Dos validaron liveness/readiness; las cuatro JTA demostraron:

- commit atómico del probe base;
- rollback atómico ante excepción runtime;
- alta, activación y consulta efectiva mediante los servicios de aplicación para dos empresas independientes;
- rollback del alta cuando falla la auditoría posterior a la escritura.

Los logs de auditoría mostraron `REGISTER_COMPANY`, `CHANGE_PLUGIN_ACTIVATION` y `CHANGE_COMPANY_STATUS` con IDs, versiones, UTC y actor de prueba, sin contenido comercial ni credenciales. La primera empresa resolvió `jta_functional` más `jta_custom_a`; la segunda resolvió únicamente `jta_custom_b`.

## WAR, imagen y Compose

Las dos distribuciones se construyeron desde limpio. La variante `with-reference-plugin` contiene exactamente un JAR del plugin; la variante normal final contiene los seis JAR propios esperados y cero `reference-plugin`.

```powershell
.\mvnw.cmd -B -Pwith-reference-plugin `
  -pl distribution/logixone-war -am clean package
.\mvnw.cmd -B -pl distribution/logixone-war -am clean package
```

La imagen `logixone/app:j11-s2-05` se construyó mediante el Dockerfile multi-stage, cuyo stage builder ejecutó nuevamente `mvn verify`. Su identificador/digest local fue `sha256:aca878d12088f819ce6dd5c77dab252c37b34584381af76a134c62e25e517ec3`.

Compose creó redes y el volumen nuevo `logixone-s205_postgres-data`, ejecutó el migrador one-shot y dejó PostgreSQL y aplicación saludables antes de las pruebas. El arnés temporal se copió después del arranque y nunca fue incorporado a la imagen ni al WAR normal.

## Fallos detectados y correcciones

1. El primer intento agregó códigos de orquestación al enum de diagnóstico del dominio. La prueba de vocabulario congelado lo rechazó; se restauró el enum y se creó `CompanyOperationCode` en aplicación.
2. El primer reactor con PostgreSQL encontró una llamada `assertTrue` ambigua al compilar un resultado `Boolean`. Se materializó un `boolean` local y se repitieron primero el módulo afectado y después el gate integral.
3. El Maven Wrapper se invocó una vez sin `MAVEN_USER_HOME`; PowerShell 5 falló al inspeccionar un directorio no enlazado antes de iniciar Maven. Se fijaron `JAVA_HOME` y `MAVEN_USER_HOME` a las instalaciones validadas de `.tools` y todos los comandos posteriores usaron ese entorno.

Ninguno de estos fallos se ocultó relajando pruebas o cambiando el baseline.

## Guía para implementadores

Se actualizó el capítulo [Flujo empresarial disponible desde J11-S2-05](../implementation-guide/README.md#flujo-empresarial-disponible-desde-j11-s2-05). Explica la secuencia de catálogo físico, alta inactiva, activación ordenada, transición operativa, guarda, resultados, concurrencia, reemplazo y auditoría, y declara expresamente que todavía no hay UI ni endpoint administrativo público.

La validación independiente de la primera edición sigue asignada a `J11-S2-08`; esta historia no presenta el temario planificado como una guía entregable ya certificada.

## Criterios y continuidad

CA-01 a CA-17 están satisfechos. `J11-S2-06` queda habilitada para filtrar capacidades, permisos y menús por composición efectiva. No se inició código ni se adelantó alcance de esa historia.

La composición efímera se inspeccionó por la etiqueta exacta `com.docker.compose.project=logixone-s205` y luego se retiró con `down --volumes --remove-orphans`. La comprobación posterior informó `containers=0 volumes=0 networks=0`. El volumen eliminado contenía únicamente datos sintéticos de esta prueba, ya no es recuperable y no pertenecía a otro entorno.
