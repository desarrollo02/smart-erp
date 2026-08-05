# J11-S2-07 — Contrato y composición de personalizaciones de pantalla

- Fecha: 2026-07-27
- Estado: Verde; 18 de 18 criterios de aceptación satisfechos
- Ambiente: Windows 11 amd64, Java Temurin 21.0.11+10, Maven Wrapper 3.9.16, Docker Engine 29.6.2, Compose 5.3.1 y WildFly 41
- PostgreSQL probado: 18.4 real, imagen fijada por digest

## Resultado

`plugin-api` 0.3.0 define un contrato Java puro, inmutable y versionado para que un plugin funcional publique pantallas extensibles y para que el único plugin `CUSTOMIZATION` efectivo de una empresa publique overlays tipados. `CompanyScreenComposer` consume la composición empresarial ya resuelta, aplica todos los funcionales antes de la personalización y produce un resultado determinista sin consultar Jakarta, JPA, HTTP ni tablas de plugins.

La personalización puede cambiar etiqueta o ayuda, ocultar, deshabilitar, endurecer `required`, mover dentro de una región y aportar fragmentos propios en slots explícitos. No existen operaciones inversas para mostrar, habilitar o volver opcional un control estándar. Un overlay inválido se rechaza completo: nunca se devuelve una pantalla estándar o parcialmente modificada que pudiera ocultar la cuarentena de la empresa.

## Contratos públicos implementados

- `ScreenId` identifica de forma estable al plugin propietario y la pantalla.
- `ScreenElementId`, `ScreenRegionId` y `ScreenSlotId` evitan referencias a clases, beans o rutas internas.
- `ScreenTextKey` acepta claves de recursos y rechaza expresiones EL o rutas.
- `ScreenFragmentId` conserva el propietario del contenido agregado.
- `ScreenDefinition` publica versión semántica, elementos y slots inmutables.
- `ScreenElementDefinition` enumera las operaciones que el propietario autoriza.
- `ScreenOverlay` declara identidad, pantalla objetivo, rango compatible y cambios no vacíos.
- `ScreenChange` modela por tipos `Label`, `Help`, `Hide`, `Disable`, `Require`, `Move` y `SlotContent`.
- `PluginDescriptor` conserva las definiciones y overlays mediante copias defensivas; el constructor anterior permanece como compatibilidad aditiva con listas vacías.

`PluginApiVersion.CURRENT` pasó de `0.2.0` a `0.3.0`, y los descriptores vivos declaran compatibilidad `[0.3.0,0.4.0)`. No fue necesario un ADR nuevo: ADR-0005 ya autorizó expresamente esta frontera, la propiedad del contrato, la capa final y las operaciones monotónicas. La historia materializa esa decisión sin cambiar el baseline técnico.

## Validación de catálogo y composición

`PluginCatalogResolver` rechaza antes del runtime:

- definiciones, elementos, slots u overlays duplicados;
- una definición publicada por un plugin no funcional;
- un overlay publicado por un plugin no `CUSTOMIZATION`;
- un `ScreenId` cuyo propietario no coincide con el descriptor;
- un overlay sin dependencia requerida sobre el propietario objetivo;
- IDs de overlay repetidos en el catálogo.

`CompanyScreenComposer` vuelve a validar defensivamente objetivo, versión, dependencia compatible, elemento, slot, operación permitida, región/posición, capacidad, propietario del fragmento y conflictos. Los diagnósticos son códigos estables. La validación ocurre sobre todo el conjunto antes de aplicar un solo cambio.

La composición parte de `CompanyContributions`; por tanto, no duplica activación, exclusividad, dependencias ni cuarentena. Una empresa no operativa produce cero pantallas. Dos empresas sobre el mismo catálogo usan exclusivamente su personalización asignada y no comparten estado mutable.

## Plugins físicos de referencia

- `reference-plugin` publica `reference_plugin:dashboard@1.0.0`, tres elementos y el slot `dashboard_extensions`.
- `reference-customization-a` depende obligatoriamente de `reference_plugin`, cambia etiqueta/ayuda, endurece `required`, mueve un elemento y aporta `tax_notice`.
- `reference-customization-b` depende obligatoriamente de `reference_plugin`, cambia etiqueta, oculta un elemento, deshabilita otro y aporta `company_notice`.
- ArchUnit impide que las personalizaciones importen la implementación del plugin funcional.

El perfil `with-screen-customization-plugins` incorpora físicamente los tres JAR. El perfil existente `with-reference-plugin` continúa incorporando solo el funcional y el build predeterminado no incorpora ninguno.

## Pruebas inmediatas

```powershell
.\mvnw.cmd -B -pl plugin-api -am test
.\mvnw.cmd -B -pl kernel-domain -am test
.\mvnw.cmd -B -pl kernel-application -am test
.\mvnw.cmd -B `
  -pl plugins/reference-plugin,plugins/reference-customization-a,plugins/reference-customization-b `
  -am test
.\mvnw.cmd -B -pl tests/architecture-tests -am test
```

Resultados finales:

- `plugin-api`: 16 pruebas verdes, incluidas identidad, inmutabilidad y conjunto exacto de operaciones.
- `kernel-domain`: 27 pruebas verdes, incluidas las reglas estructurales del catálogo.
- `kernel-application`: 37 pruebas verdes; 7 escenarios nuevos cubren las operaciones permitidas, dos empresas, determinismo, inmutabilidad y fallos atómicos.
- plugins de referencia: 4 pruebas verdes entre el funcional y las personalizaciones A/B.
- `architecture-tests`: 9 pruebas verdes, compuestas por 7 reglas ArchUnit y 2 integraciones con definiciones físicas reales.

## Gate integral con PostgreSQL

```powershell
.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" clean verify
```

Resultado: código 0, 16 de 16 módulos y 136 pruebas verdes:

- 122 pruebas del baseline normal;
- 7 escenarios de migración sobre PostgreSQL 18.4;
- 7 escenarios de persistencia y aplicación sobre PostgreSQL 18.4.

No se omitió, desactivó ni relajó ninguna prueba. `plugin-api`, `kernel-api`, dominio y aplicación permanecen libres de Jakarta, JPA y JDBC.

## Matriz de distribución

Se construyeron desde limpio las tres variantes:

```powershell
.\mvnw.cmd -B -Pwith-screen-customization-plugins `
  -pl distribution/logixone-war -am clean package
.\mvnw.cmd -B -Pwith-reference-plugin `
  -pl distribution/logixone-war -am clean package
.\mvnw.cmd -B -pl distribution/logixone-war -am clean package
```

La inspección binaria confirmó:

- personalizaciones: exactamente `reference-plugin`, `reference-customization-a` y `reference-customization-b`;
- referencia: exactamente un `reference-plugin`;
- base: cero plugins de referencia y exactamente las seis bibliotecas propias del núcleo y shell.

Alternar los perfiles siempre usa `clean`, por lo que el WAR expandido no conserva JAR de la variante anterior.

## Imágenes y Compose

El `Dockerfile` admite explícitamente `none`, `with-reference-plugin` y `with-screen-customization-plugins`; cualquier otro valor sigue fallando con código 64. Los builders ejecutaron `mvn verify` y produjeron:

- `logixone/app:j11-s2-07`: `sha256:4a92a109892e4f3d7c8cf06b2dde4d312545fe6e5dc021c2f8bf9b70a68602d0`;
- `logixone/app:j11-s2-07-screens`: `sha256:caa1769e0ea7162957609ec6b25d519bbdfed815c8a00ea93cb5511d8add01e0`.

La imagen base arrancó en `logixone-s207-base` y registró `plugin_count=0`. La imagen de pantallas arrancó por separado en `logixone-s207-screens`, superó liveness/readiness `200 UP`, y registró exactamente:

```text
plugin_count=3 plugins=reference_plugin@1.0.0,reference_custom_a@1.0.0,reference_custom_b@1.0.0
```

El WAR copiado temporalmente desde ese contenedor contenía exactamente los tres JAR esperados. La copia se eliminó después de la inspección.

## PostgreSQL y JTA dentro de WildFly

El arnés opt-in se construyó y desplegó solo sobre la composición base efímera:

```powershell
.\mvnw.cmd -B -Pjta-runtime-harness `
  -pl tests/runtime-persistence-harness -am package

.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:18088" `
  "-Dlogixone.jta-probe=true" verify
```

Resultado: código 0, 6 de 6 pruebas y cero omitidas. El catálogo temporal publicó un funcional, dos personalizaciones A/B y el fixture de rollback. El escenario empresarial comprobó dentro de WildFly y la misma base PostgreSQL:

- empresa A: etiqueta A, elemento visible y requerido, acción habilitada y solo fragmento A;
- empresa B: etiqueta B, elemento oculto y no requerido por el estándar, acción deshabilitada y solo fragmento B;
- ambas conservan el mismo `ScreenId` funcional, aislamiento de empresa y orden funcional→personalización;
- commit, rollback y rollback obligatorio por fallo de auditoría permanecen atómicos.

El arnés sigue ausente del WAR y de las imágenes normales.

## Seguridad y límites

- Los overlays solo controlan metadatos neutrales de presentación; no ejecutan una operación del plugin.
- No existe operación para eliminar autorización, validación de negocio, auditoría o `PluginOperationGuard`.
- Ocultar, deshabilitar o requerir un elemento no reemplaza controles del servidor.
- No se agregó endpoint administrativo o funcional de producción.
- No se implementó renderizado JSF/PrimeFaces; por tanto, Playwright no aplica todavía.
- No se registraron secretos, SQL, nombres comerciales ni datos personales.

## Fallos encontrados y correcciones

1. Una aserción de test intentó usar el método inexistente `Set.allOf`; se sustituyó por `EnumSet.allOf` y la prueba del módulo se repitió inmediatamente hasta quedar verde.
2. El `Dockerfile` todavía no reconocía el nuevo perfil físico; se agregó la rama explícita y ambas imágenes se construyeron con éxito.
3. Un comando auxiliar apuntó inicialmente a un nombre antiguo del módulo del arnés; se corrigió a `tests/runtime-persistence-harness` y el build terminó verde.
4. La primera invocación REST/JTA dejó sin comillas el argumento Maven con URL; Maven no ejecutó pruebas. Se repitió con `-Dlogixone.base-uri=...` entre comillas y obtuvo 6 de 6.
5. Una inspección combinada del contenedor tuvo comillas incompatibles entre PowerShell y `sh`; no ejecutó ninguna acción. Se separó en consultas simples de solo lectura.

No quedó un fallo funcional pendiente.

## Guía para implementadores

Se actualizaron los capítulos 8 y 9 de la [Guía de implementación del ERP por empresa](../implementation-guide/README.md): cómo declarar dependencia, publicar una pantalla, autorizar operaciones, construir un overlay propio, probarlo y preparar un futuro adaptador JSF/PrimeFaces. La guía diferencia expresamente el contrato disponible de la UI todavía inexistente y prohíbe reemplazar XHTML o consumir clases internas.

La primera edición completa, el ejemplo ficticio de extremo a extremo y la validación por un implementador independiente continúan asignados a `J11-S2-08`.

## Gate documental G0

La validación estricta final recorrió 66 archivos Markdown y 178 enlaces locales. Resultado: cero archivos UTF-8 inválidos, cero caracteres de reemplazo, cero enlaces rotos y estados coherentes entre la historia, el Sprint y la historia siguiente.

## Criterios y continuidad

CA-01 a CA-18 están satisfechos. `J11-S2-08` queda habilitada, pero no se inició. Esa historia repetirá el baseline integral desde limpio y cerrará el Sprint; no necesita rediseñar el contrato entregado aquí.

Las composiciones efímeras se identificaron exclusivamente como `logixone-s207-base` y `logixone-s207-screens`. Después de conservar la evidencia se retiraron junto con sus redes y volúmenes. La comprobación final informó cero contenedores y cero volúmenes para ambos proyectos. Los volúmenes eliminados contenían solo datos sintéticos creados durante esta certificación, no pertenecían a un entorno previo y no son recuperables.
