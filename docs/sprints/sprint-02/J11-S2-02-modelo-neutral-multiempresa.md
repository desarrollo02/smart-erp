# J11-S2-02 — Contratos y modelo neutral multiempresa

- Estado: Completada y verde
- Fecha de cierre: 2026-07-27
- Dependencia: `J11-S2-01` completada y verde

## Objetivo

Materializar en Java puro las identidades, estados, puertos y reglas deterministas aprobadas para empresas, activación y asignación obligatoria de personalización, sin introducir Jakarta, JPA, JDBC ni detalles de WildFly.

## Alcance por módulo

### `plugin-api`

- categoría explícita e inmutable `FUNCTIONAL` o `CUSTOMIZATION` dentro del descriptor público;
- validaciones neutrales que impidan categorías ausentes o desconocidas;
- compatibilidad binaria y de fuente evaluada antes de modificar el contrato existente.

### `kernel-api`

- identidad empresarial pública e inmutable;
- contrato mínimo de contexto empresarial que futuros plugins puedan consumir;
- ningún resultado interno de resolución ni detalle de persistencia expuesto a plugins.

### `kernel-domain`

- ciclo de vida de empresa;
- decisión deseada de activación por empresa/plugin;
- resolución de activación efectiva sobre un catálogo físico válido;
- invariantes de dependencias requeridas y aislamiento por empresa;
- asignación exclusiva empresa/personalización y elegibilidad operativa de la empresa;
- orden determinista que sitúa las personalizaciones después de los plugins funcionales;
- diagnósticos tipados y ordenados.

### `kernel-application`

- puertos de entrada y salida para consultar empresas y activaciones;
- modelos de comando/resultado sin DTO de infraestructura;
- vista neutral interna de la personalización asignada y de los plugins efectivos;
- orquestación neutral preparada para transacciones externas.

## Reglas mínimas a demostrar

- no existe `CompanyId` nulo, vacío, malformado o mutable;
- una empresa inactiva produce cero plugins efectivos;
- ausencia de decisión nunca equivale a activo;
- solo plugins del `PluginRegistry` pueden resultar efectivos;
- dependencias requeridas deben ser efectivas para la misma empresa;
- resultados y diagnósticos no dependen del orden de entrada;
- las colecciones devueltas son inmutables;
- datos de una empresa no participan en la resolución de otra.
- una empresa no resulta operativa sin exactamente una personalización asignada, presente y compatible;
- un plugin `CUSTOMIZATION` no puede asignarse a dos empresas ni activarse mediante el flujo común;
- los plugins `FUNCTIONAL` siempre preceden a la personalización empresarial en la composición efectiva.

## Fuera de alcance

- entidades JPA, `persistence.xml`, datasource o SQL;
- CDI, `@RequestScoped`, filtros servlet o headers;
- implementación de repositorios;
- endpoints, UI y contratos concretos de pantalla, que corresponden a `J11-S2-07`;
- asignación de permisos a usuarios.

## Criterios de aceptación

- **CA-01:** `kernel-api` expone únicamente contratos pequeños, Java puros y aprobados por el ADR.
- **CA-02:** los tipos de identidad y estado validan sus invariantes al construirse.
- **CA-03:** el dominio distingue activación deseada de activación efectiva.
- **CA-04:** empresa inactiva, decisión ausente y plugin ausente producen denegación segura.
- **CA-05:** activar con dependencia requerida inactiva genera diagnóstico estable.
- **CA-06:** desactivar con dependiente activo genera diagnóstico estable.
- **CA-07:** dependencias opcionales ausentes no bloquean activación.
- **CA-08:** el resultado es determinista e inmutable para cualquier orden de entrada.
- **CA-09:** pruebas con dos empresas demuestran aislamiento completo.
- **CA-10:** aplicación depende de puertos, no de adaptadores o entidades.
- **CA-11:** ArchUnit impide Jakarta/JPA/JDBC en API, dominio y aplicación.
- **CA-12:** pruebas de los módulos y `mvn verify` quedan verdes y documentadas.
- **CA-13:** `PluginDescriptor` declara una categoría validada sin introducir Jakarta en `plugin-api`.
- **CA-14:** el dominio exige exactamente una asignación de personalización por empresa operativa.
- **CA-15:** una personalización asignada a otra empresa nunca resulta efectiva ni visible.
- **CA-16:** resolución y diagnósticos mantienen orden funcional-primero/personalización-al-final de forma determinista.

## Gates

1. prueba inmediata de `kernel-api`;
2. prueba inmediata de `kernel-domain`;
3. prueba inmediata de `kernel-application`;
4. ArchUnit;
5. `mvnw.cmd -B verify`.

## Resultado

- `PluginDescriptor` exige `PluginKind`; `PluginApiVersion.CURRENT` es `0.2.0` y el plugin de referencia fue migrado al nuevo contrato sin constructor compatible implícito.
- El catálogo rechaza dependencias funcional→personalización y personalización→personalización, y ordena todos los plugins funcionales antes de las personalizaciones.
- `kernel-api` expone únicamente `CompanyId` y el puerto de lectura `CompanyContext`, ambos Java puros.
- `kernel-domain` modela empresa, decisión deseada, resolución efectiva, política de cambios, cuarentena y los códigos estables mínimos de ADR-0005.
- `kernel-application` aporta comandos neutrales, puertos de repositorio/generación y una consulta por empresa que no mezcla decisiones de otros ámbitos.
- ArchUnit prohíbe Jakarta, JDBC y PostgreSQL en las capas neutrales.
- El gate final terminó con 14 de 14 módulos y 83 pruebas verdes, incluidas 5 reglas ArchUnit.
- Los WAR predeterminado y `with-reference-plugin` contienen respectivamente cero y un JAR del plugin de referencia, sin duplicar `plugin-api` ni `kernel-api`.

## Cobertura de aceptación

| Criterio | Resultado |
|---|---|
| `CA-01` | `kernel-api` contiene dos contratos pequeños y depende solo de Java estándar. |
| `CA-02` | Constructores de empresa, identidad, activación y comandos rechazan valores inválidos. |
| `CA-03` | `PluginActivationDecision` conserva intención; `CompanyPluginResolution` expone efectividad derivada. |
| `CA-04` | Pruebas cubren empresa inactiva, fila ausente y plugin físicamente ausente. |
| `CA-05` | La política rechaza habilitación cuando falta una dependencia requerida deseada. |
| `CA-06` | La política rechaza deshabilitación frente a dependiente funcional o personalización asignada. |
| `CA-07` | Una dependencia opcional ausente no bloquea la habilitación. |
| `CA-08` | Catálogo, plugins efectivos y diagnósticos se ordenan y copian defensivamente. |
| `CA-09` | La matriz con dos `CompanyId` ignora decisiones ajenas y conserva la personalización propia. |
| `CA-10` | La aplicación depende de puertos y del dominio, sin adaptadores ni entidades. |
| `CA-11` | Cinco reglas ArchUnit quedaron verdes, incluida la prohibición de Jakarta/JDBC/PostgreSQL. |
| `CA-12` | Gates focalizados y `clean verify` integral quedaron verdes y documentados. |
| `CA-13` | Categoría obligatoria y API semántica `0.2.0` probadas en `plugin-api`. |
| `CA-14` | Personalización ausente, de categoría incorrecta, compartida o incompatible deja la empresa no operacional. |
| `CA-15` | Una asignación marcada como perteneciente a otra empresa no aporta composición parcial. |
| `CA-16` | Pruebas con entradas permutadas conservan el orden funcional-primero/personalización-al-final. |

Evidencia reproducible: [J11-S2-02 — Modelo neutral multiempresa](../../evidence/J11-S2-02-modelo-neutral-multiempresa.md).

## Siguiente historia permitida

`J11-S2-03` queda habilitada para implementar exclusivamente la migración aditiva `core` V2 y sus gates PostgreSQL.
