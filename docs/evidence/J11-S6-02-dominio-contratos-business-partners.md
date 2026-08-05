# Evidencia J11-S6-02 - Dominio y contratos de `business_partners`

- Fecha: 2026-07-29
- Estado: Verde
- Historia: [J11-S6-02](../sprints/sprint-06/J11-S6-02-dominio-contratos-business-partners.md)
- ADR: [ADR-0014](../adr/0014-modelo-participante-comercial-y-contrato-publico.md)

## Resultado

Se crearon `business-partners-api` y `business-partners`. La API pública contiene
únicamente identidad, clasificaciones, referencia mínima, versión y puerto de
lectura por empresa. La implementación contiene el descriptor y un agregado Java
puro sin persistencia o UI.

## Pruebas incrementales

| Corte | Comando | Resultado |
|---|---|---|
| API pública | `.\mvnw.cmd -B -pl plugins/business-partners-api -am test` | 5 propias + 6 de kernel, verdes |
| esqueleto CDI/SPI | `.\mvnw.cmd -B -pl plugins/business-partners -am test` | 2 propias; 5 módulos verdes |
| agregado y roles | mismo gate de plugin | 9 propias verdes |
| detalles neutrales | mismo gate de plugin | 14 propias verdes |
| SPI final | mismo gate de plugin | 15 propias verdes |
| límites | `.\mvnw.cmd -B -pl tests/architecture-tests -am test` | 16 módulos; 14 pruebas arquitectónicas verdes |
| reactor | `.\mvnw.cmd -B verify` | 20 módulos; 212 pruebas verdes |

Resultado consolidado de Surefire:

```text
REPORTS=53
TESTS=212
FAILURES=0
ERRORS=0
SKIPPED=0
```

No se ejecutaron PostgreSQL/Testcontainers, Docker/Compose o Playwright porque no
se agregaron migraciones, adaptadores, composición física, endpoints ni pantallas.
Esos gates pertenecen a J11-S6-03, J11-S6-05 y J11-S6-06/07.

## Gate documental G0

Se decodificaron como UTF-8 estricto todos los Markdown de `docs/` y de los dos
módulos nuevos, se buscaron secuencias dañadas y se resolvieron enlaces locales
desde cada origen:

```text
MARKDOWN_FILES=153
BAD_FILES=0
LOCAL_LINKS=572
BROKEN_LINKS=0
```

## Fallos encontrados y corrección

### Java 8 al invocar el generador

El primer comando usó `java` del `PATH` y recibió
`UnsupportedClassVersionError`: el JAR exige Java 21 y el sistema resolvió Java 8.
Se comprobó que `plugins/business-partners` no existía, demostrando el fallo seguro
del generador. La repetición invocó directamente
`.tools/jdk/jdk-21.0.11+10/bin/java.exe` y generó siete archivos.

### Tipo genérico de `ServiceLoader`

La primera prueba explícita del proveedor declaró `List<Class<?>>`, pero Java
retorna `List<Class<? extends PluginDefinition>>`. `testCompile` falló y bloqueó el
avance. Se corrigió únicamente la declaración y se repitió el mismo gate: las 15
pruebas del plugin quedaron verdes.

No se omitió, desactivó o relajó ninguna prueba.

### Bean CDI no proxyable generado como `final`

La revisión posterior al primer `verify` detectó que las plantillas del scaffold
producían clases `final` con `@ApplicationScoped`. Un alcance CDI normal necesita
un tipo proxyable y podía fallar al desplegar el plugin. Se retiró `final` de la
definición productiva y de las plantillas funcional/personalización, y las pruebas
generadas ahora exigen explícitamente una clase no final.

El gate combinado de scaffold + plugin dejó verdes 6 módulos, 9 pruebas de la
herramienta y 15 del plugin. Después se repitió `mvn verify`: 20 módulos y 212
pruebas volvieron a quedar verdes.

## Inspección de artefactos

```text
API_ENTRIES=15
API_JAKARTA=0
API_DOMAIN_INTERNAL=0
PLUGIN_ENTRIES=24
SPI=1
BEANS=1
MIGRATIONS=0
BASE_WAR_BUSINESS_PARTNERS=0
```

La API no contiene Jakarta ni el paquete interno `domain`. El JAR desplegable
registra exactamente una definición por SPI y un `beans.xml`, sin migraciones. El
WAR base no contiene `business-partners` ni su API porque la composición física se
realizará en `J11-S6-06`.

## Cobertura de comportamiento

- UUID canónico y referencia pública defensiva;
- contrato semántico `1.0.0`;
- normalización NFKC de código/nombre;
- cero roles y roles cliente/proveedor coexistentes;
- estado del participante y roles independientes;
- inactivación/reactivación sin baja física;
- conflicto optimista sin sobrescritura;
- identificación presentada/normalizada y candidato duplicado;
- código ISO de país más texto geográfico;
- primario único por categoría/finalidad;
- contactos nominales livianos y colecciones inmutables;
- descriptor vacío de contribuciones y equivalencia CDI/SPI;
- API/dominio libres de frameworks.

## Archivos principales

- `plugins/business-partners-api/`;
- `plugins/business-partners/`;
- `pom.xml`;
- `tests/architecture-tests/`;
- ADR-0014, historia, guía, arquitectura y documentos de Sprint 6.

## Conclusión

El corte neutral satisface J11-S6-02 sin adelantar persistencia ni interfaz. El
baseline verde permanece disponible y `J11-S6-03` queda habilitada.
