# {{DISPLAY_NAME}}

- Artifact Maven: `{{ARTIFACT_ID}}`
- Plugin ID: `{{PLUGIN_ID}}`
- Tipo: `{{PLUGIN_KIND}}`
- Versión inicial: `{{PLUGIN_VERSION}}`
- Compatibilidad Plugin API: `[{{PLUGIN_API_MIN}},{{PLUGIN_API_MAX}})`

Este módulo fue creado por el generador versionado de Logixone. Es un punto de
partida neutral: no representa todavía una capacidad ERP terminada.

{{CUSTOMIZATION_DETAILS}}

## Registro explícito pendiente

Revise primero identidad, contratos y límites. Después integre el módulo en un
cambio coherente:

1. agregue `<module>plugins/{{ARTIFACT_ID}}</module>` al POM padre;
2. agregue `py.com.logixone:{{ARTIFACT_ID}}:${project.version}` a
   `dependencyManagement`;
3. cree un perfil con nombre empresarial explícito en
   `distribution/logixone-plugin-set/pom.xml` y agregue allí la dependencia;
4. construya WAR y migrador con exactamente ese mismo perfil;
5. inspeccione el JAR del WAR y el proveedor SPI del migrador;
6. ejecute pruebas del módulo, ArchUnit y `mvn verify`.

El generador no edita esos POM automáticamente. Una selección física de plugins es
una decisión revisable de la distribución, no un efecto lateral del scaffold.

## Persistencia

No existe migración inicial por defecto. Cuando el modelo del dominio esté
aprobado, agregue `db/migration/{{PLUGIN_ID}}/V1__<descripcion>.sql` y una
`MigrationContribution` cuyo esquema sea exactamente `plg_{{PLUGIN_ID}}`. No
modifique una migración ya aplicada y no acceda a tablas privadas de otro plugin.

## Interfaz

Las pantallas opcionales usan contratos públicos `ScreenDefinition`/`ScreenOverlay`
y el renderer JSF Material Design 3 del shell. Deben verificarse en 375, 720 y
1280 px. Un plugin nunca reemplaza XHTML ni importa beans privados de otro plugin.

Consulte [plugin-contract.md](docs/plugin-contract.md) antes de agregar lógica.
