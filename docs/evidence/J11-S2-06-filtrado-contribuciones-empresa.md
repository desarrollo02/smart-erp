# J11-S2-06 — Filtrado de contribuciones por empresa

- Fecha: 2026-07-27
- Estado: Verde; 16 de 16 criterios de aceptación satisfechos
- Ambiente: Windows 11 amd64, Java Temurin 21.0.11+10, Maven Wrapper 3.9.16, Docker Engine 29.6.2, Compose 5.3.1 y WildFly 41
- PostgreSQL probado: 18.4 real, imagen fijada por digest

## Resultado

`CompanyContributionService` compone para un `CompanyId` únicamente las contribuciones de los plugins efectivos devueltos por `CompanyPluginQueryService`. La vista resultante es inmutable, conserva propietario y categoría, mantiene el orden declarado dentro de cada descriptor y aplana capacidades, permisos y menús en el orden efectivo: plugins funcionales primero y exactamente la personalización empresarial al final.

No se persisten copias de contribuciones ni se modifican descriptores globales. Una empresa inexistente, inactiva o con personalización ausente/incompatible produce una vista no operativa y vacía; una decisión guardada para un plugin físicamente ausente genera diagnóstico y nunca una contribución fantasma.

## Diseño implementado

- `PluginContributions` copia defensivamente identidad, categoría, capacidades, permisos y menús de un descriptor efectivo.
- `CompanyContributions` valida sus invariantes y ofrece listas inmutables de plugins y contribuciones aplanadas, junto con diagnósticos o fallo de consulta.
- `CompanyContributionService` reutiliza la consulta empresarial existente; no vuelve a implementar activación, dependencias ni selección de personalización.
- `PluginCatalogResolver` rechaza IDs de capacidad, permiso o menú repetidos entre propietarios distintos antes de publicar el catálogo.
- La composición no concede permisos a usuarios. Los permisos devueltos son el vocabulario disponible para la empresa; la autorización y `PluginOperationGuard` siguen siendo controles obligatorios e independientes.

## Pruebas por incremento

```powershell
.\mvnw.cmd -B -pl kernel-domain -am test
.\mvnw.cmd -B -pl kernel-application -am test
.\mvnw.cmd -B -pl tests/architecture-tests -am test
```

Resultados finales:

- `kernel-domain`: 24 pruebas verdes; la nueva prueba rechaza IDs de contribución globalmente duplicados.
- `kernel-application`: 30 pruebas verdes; 5 escenarios nuevos cubren dos empresas, aislamiento, inactividad, ausencia física, dependencias requeridas/opcionales, determinismo, inmutabilidad y guarda.
- `architecture-tests`: 7 pruebas verdes; 6 reglas ArchUnit más una prueba que compone el `ReferencePluginDefinition` real con una personalización final.

## Gate integral con PostgreSQL

```powershell
.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" clean verify
```

Resultado: código 0, 14 de 14 módulos y 118 pruebas verdes:

- 104 pruebas del baseline normal, incluidas 6 reglas ArchUnit y la integración del plugin de referencia;
- 7 escenarios de migración sobre PostgreSQL 18.4;
- 7 escenarios JPA y de aplicación sobre PostgreSQL 18.4.

No se omitió, desactivó ni relajó ninguna prueba. APIs, dominio y aplicación permanecieron libres de Jakarta, JPA y JDBC.

## PostgreSQL y JTA dentro de WildFly

El arnés opt-in se construyó por separado y se copió únicamente a la composición efímera `logixone-s206-base`:

```powershell
.\mvnw.cmd -B -Pjta-runtime-harness `
  -pl tests/runtime-persistence-harness -am package

.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:18086" `
  "-Dlogixone.jta-probe=true" verify
```

Resultado: código 0, 6 de 6 pruebas y cero omitidas. Además de salud, commit y rollback, el recorrido de aplicación creó dos empresas sobre el mismo catálogo físico y comprobó:

- empresa A: `jta_functional`, seguido de `jta_custom_a`, con sus dos capacidades, permisos y menús en ese orden;
- empresa B: únicamente `jta_custom_b` y sus contribuciones;
- ninguna contribución de `jta_custom_a` apareció en B ni de `jta_custom_b` en A;
- el fallo obligatorio de auditoría continuó revirtiendo empresa y activación en la misma transacción JTA.

El WAR normal arrancó primero con `plugin_count=0`; el arnés temporal publicó después su propio catálogo de prueba. El arnés no forma parte de la distribución ni de las imágenes normales.

## WAR, imágenes y Compose

Las dos variantes se construyeron desde limpio e inspeccionaron:

```powershell
.\mvnw.cmd -B -Pwith-reference-plugin `
  -pl distribution/logixone-war -am clean package
.\mvnw.cmd -B -pl distribution/logixone-war -am clean package
```

La variante con perfil contiene exactamente un `WEB-INF/lib/reference-plugin-0.1.0-SNAPSHOT.jar`. La variante base final contiene exactamente las seis bibliotecas propias esperadas y cero `reference-plugin`.

También se construyeron dos imágenes multi-stage; sus builders ejecutaron `mvn verify`:

- `logixone/app:j11-s2-06`: `sha256:3a33028306b06ee407e7fb2ead16e8cdcdf354215ed6f0ac4b04c12ae54b9e43`;
- `logixone/app:j11-s2-06-reference`: `sha256:1fbd714689ea031b6a5d6986435e168c764cf03c81b1c208b4815c3efcc20606`.

Cada imagen arrancó en una composición nueva e independiente con PostgreSQL, migrator one-shot y health `UP`. REST Assured obtuvo 2 de 2 pruebas de salud en ambas. La inspección del contenedor de referencia confirmó un solo JAR y el log `plugin_count=1 plugins=reference_plugin@1.0.0`.

## Fallos y correcciones

No hubo fallos de código, pruebas, migraciones, arranque ni salud durante el cierre. Una primera consulta auxiliar a `docker image inspect` usó una plantilla Go inválida para unir digests; se corrigió la plantilla de solo lectura y se repitió hasta obtener ambos IDs. No se modificó ni reconstruyó ningún artefacto como consecuencia.

## Guía para implementadores

Se agregó el capítulo [Contribuciones empresariales disponibles desde J11-S2-06](../implementation-guide/README.md#contribuciones-empresariales-disponibles-desde-j11-s2-06). Explica qué significa la vista efectiva, cómo interpretar una composición vacía, por qué la personalización aparece al final y por qué ni el menú ni la lista de permisos sustituyen autorización del servidor.

La UI y los overlays de pantalla siguen fuera de esta historia. La primera edición utilizable y su validación independiente permanecen asignadas a `J11-S2-08`.

## Criterios y continuidad

CA-01 a CA-16 están satisfechos. `J11-S2-07` queda habilitada para definir y componer contratos neutrales de pantalla; no se inició código ni se adelantó alcance de esa historia.

Las composiciones efímeras se identificaron exclusivamente como `logixone-s206-base` y `logixone-s206-reference`. Después de conservar la evidencia se retiraron con sus volúmenes y la comprobación final informó, para ambas, cero contenedores, cero volúmenes y cero redes. Los volúmenes eliminados contenían solo datos sintéticos de estas pruebas, no son recuperables y no pertenecían a ningún entorno previo.
