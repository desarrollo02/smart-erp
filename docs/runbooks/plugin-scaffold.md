# Generar un plugin neutral de Logixone

- Historia: `J11-S5-02`
- Fecha: 2026-07-29
- Alcance: plugins funcionales y personalizaciones empresariales

## Propósito

`tools/plugin-scaffold` crea el punto de partida mínimo y reproducible de un
plugin. No inventa tablas, entidades, permisos, menús ni pantallas. Tampoco edita
los POM del reactor: incorporar físicamente el módulo continúa siendo una decisión
explícita y revisable.

## 1. Construir la herramienta

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd -B -pl tools/plugin-scaffold -am package
```

Resultado esperado:

```text
tools/plugin-scaffold/target/plugin-scaffold-0.1.0-SNAPSHOT-executable.jar
```

## 2. Generar un plugin funcional

El padre del destino debe existir. Este ejemplo crea un módulo neutral de muestra:

```powershell
java -jar tools\plugin-scaffold\target\plugin-scaffold-0.1.0-SNAPSHOT-executable.jar `
  --project-root . `
  --output plugins\sample-capability `
  --artifact-id sample-capability `
  --plugin-id sample_capability `
  --package py.com.logixone.plugins.samplecapability `
  --display-name "Sample capability" `
  --kind functional
```

La versión inicial es `1.0.0`; puede fijarse con `--version`. Un resultado válido
emite `event=plugin_scaffold_created` y `file_count=7`.

## 3. Generar la personalización de una empresa

Cada empresa necesita un plugin de personalización distinto. Debe declarar el
funcional que modifica y el intervalo de versiones que entiende:

```powershell
java -jar tools\plugin-scaffold\target\plugin-scaffold-0.1.0-SNAPSHOT-executable.jar `
  --project-root . `
  --output plugins\acme-customization `
  --artifact-id acme-customization `
  --plugin-id acme_customization `
  --package py.com.logixone.plugins.acmecustomization `
  --display-name "ACME customization" `
  --kind customization `
  --target-plugin-id sample_capability `
  --target-min-version 1.0.0 `
  --target-max-version 2.0.0
```

El descriptor generado agrega una dependencia `REQUIRED` hacia
`sample_capability` en `[1.0.0, 2.0.0)`. Las modificaciones visuales futuras se
aportan mediante contratos públicos y overlays; no se copian ni reemplazan XHTML,
beans o entidades privadas de otro plugin.

## 4. Revisar los siete archivos

La salida contiene:

1. `pom.xml`;
2. `README.md`;
3. `docs/plugin-contract.md`;
4. una clase `PluginDefinition`;
5. su prueba unitaria;
6. `META-INF/beans.xml`;
7. `META-INF/services/py.com.logixone.plugin.api.PluginDefinition`.

El descriptor funcional nace con capacidades, permisos, menús, pantallas,
overlays y migraciones vacíos. Complete el checklist contractual antes de agregar
comportamiento.

La definición `@ApplicationScoped` se genera como clase no final para que sea
proxyable por CDI. Su prueba verifica anotación y proxyabilidad; no vuelva final la
clase ni sus métodos de negocio sin ejecutar un despliegue CDI real.

## 5. Incorporar el módulo al build

1. Agregar el módulo en `<modules>` del POM padre.
2. Registrar su coordenada en `<dependencyManagement>` si otros POM la consumen.
3. Agregar la dependencia a un perfil explícito de
   `distribution/logixone-plugin-set/pom.xml`.
4. Usar ese mismo perfil al construir WAR y migrador; no declarar dependencias
   separadas en sus POM.
5. Comprobar también la variante base, que debe permanecer sin el plugin.

## 6. Agregar comportamiento sólo desde requisitos

- Si necesita persistencia, crear `db/migration/<plugin_id>/` y usar únicamente
  el esquema `plg_<plugin_id>`.
- No crear relaciones JPA ni SQL hacia tablas privadas de otro plugin.
- Definir capacidades, permisos, menús y contratos públicos antes de la UI.
- Una pantalla debe usar Jakarta Faces, Material Design 3 y responder correctamente
  a los rangos compacto, medio y expandido del shell.
- Para documentos comerciales, analizar el manual técnico SIFEN como referencia
  estructural de persistencia; no copiar su modelo como dominio interno.

## 7. Validar

Ejecutar primero el módulo y luego los gates del corte:

```powershell
.\mvnw.cmd -B -pl plugins/sample-capability -am test
.\mvnw.cmd -B -pl tests/architecture-tests -am test
.\mvnw.cmd -B verify
```

Si cambió composición, persistencia o runtime, agregar PostgreSQL/Testcontainers,
inspección de WAR/migrador y Docker/Compose. Al cierre de Sprint también son
obligatorios la demo visual responsive y el PDF de estructura verificado.

## Fallos seguros

La herramienta rechaza opciones desconocidas o duplicadas, identidades inválidas
o reservadas, versiones incoherentes, personalizaciones sin objetivo, destinos
fuera del proyecto y carpetas ya existentes. Un fallo no debe dejar el módulo
parcial: revise el mensaje `plugin_scaffold_rejected` o
`plugin_scaffold_failed`, corrija la entrada y vuelva a ejecutar.
