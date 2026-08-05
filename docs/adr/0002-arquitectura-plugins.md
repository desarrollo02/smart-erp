# ADR-0002 — Arquitectura de plugins

- Estado: Aceptado
- Fecha: 2026-07-23
- Historia: `J11-S1-01`

## Contexto

El ERP debe crecer mediante plugins sin repetir el descubrimiento global, las entidades compartidas, los controladores centrales y las dependencias cíclicas del legado. Al mismo tiempo, el proyecto no necesita todavía instalación en caliente ni aislamiento mediante classloaders.

## Decisión

Se adopta un monolito modular con composición física de plugins durante el build y activación lógica por empresa durante la ejecución.

### Composición física

- Un plugin desplegable es un JAR Maven incluido como dependencia de `distribution/logixone-war` y empaquetado bajo `WEB-INF/lib`.
- La distribución determina qué plugins están físicamente presentes.
- Agregar o retirar un plugin requiere reconstruir y redesplegar el WAR.
- No se implementarán hot install, hot unload, OSGi, PF4J ni classloaders personalizados en la primera arquitectura.
- La distribución debe poder construirse con el plugin de referencia presente y ausente sin modificar el kernel.

### Contrato técnico

`plugin-api` será un módulo Java puro con el SPI técnico mínimo. No dependerá de Jakarta, WildFly, Hibernate, JSF, PrimeFaces ni infraestructura.

Cada plugin declarará mediante un descriptor inmutable:

- identificador estable y único;
- versión semántica;
- versión o rango compatible del API de plugins;
- dependencias requeridas y opcionales;
- capacidades;
- permisos;
- contribuciones de menú;
- migraciones;
- metadatos operativos mínimos.

La detección CDI pertenecerá a `kernel-infrastructure-jakarta`, que adaptará beans desplegados al contrato neutral. CDI no formará parte del contrato de dominio.

### Registro y compatibilidad

Al arrancar, el registro construirá un grafo dirigido y rechazará de forma determinista:

- identificadores duplicados;
- dependencias requeridas ausentes;
- ciclos;
- versiones incompatibles;
- descriptores inválidos.

Un fallo de composición impedirá declarar lista la aplicación. El orden de inicialización y migración será el orden topológico del grafo válido.

### Activación por empresa

- La presencia física es global para la distribución.
- La activación es una decisión persistida por empresa y propiedad del kernel.
- No se puede desactivar un plugin requerido por otro plugin activo en esa empresa.
- Un plugin desactivado no aporta menú ni operaciones funcionales para esa empresa.
- Sus endpoints pueden seguir registrados físicamente, pero deben negar la operación antes de ejecutar lógica de negocio.
- Desactivar o retirar el JAR no elimina datos ni migraciones aplicadas.

### Límites entre plugins

- Ningún plugin importa implementaciones, DTO internos o entidades de otro plugin.
- No existen asociaciones JPA entre plugins.
- La comunicación usa identificadores, contratos públicos, puertos o eventos.
- Si un dominio necesita exponer un contrato empresarial, lo hace en un módulo `<plugin>-api` separado de `<plugin>-impl` y libre de JPA e infraestructura.
- La comunicación síncrona se reserva para una respuesta inmediata necesaria; la propagación desacoplada usa eventos.
- ArchUnit verificará dependencias prohibidas.

### Responsabilidades del kernel

El kernel se limita a identidad, empresas, contexto de ejecución, seguridad, configuración, auditoría, registro de plugins, compatibilidad y composición de contribuciones. Ventas, inventario, transporte, facturación y demás capacidades empresariales pertenecen a plugins.

## Alternativas consideradas

### Microservicios desde el inicio

Se descartan por su coste operativo, consistencia distribuida y complejidad prematura. Los límites definidos permitirán separar un módulo en el futuro si existe una necesidad demostrada.

### Carga dinámica de JAR

Se descarta porque añade problemas de classloading, CDI, JPA, seguridad, actualización y recuperación que no son necesarios para validar el producto.

### Un único módulo con paquetes por dominio

Se descarta porque Maven y las pruebas arquitectónicas no podrían hacer cumplir con la misma fuerza los límites físicos.

## Consecuencias

- Instalar físicamente un plugin implica un nuevo artefacto y un nuevo digest de imagen.
- Activar un plugin existente no exige reconstrucción.
- El kernel y `plugin-api` deberán mantener compatibilidad explícita.
- Los contratos empresariales públicos aumentan el número de módulos, pero reducen el acoplamiento accidental.
- La aplicación debe probar composición, compatibilidad y autorización por empresa.

## Resolución de implementación para Sprint 1

`J11-S1-04` concretó el contrato neutral sin cambiar la decisión arquitectónica:

- `PluginDefinition` expone un `PluginDescriptor` sin depender de CDI ni Jakarta.
- Los identificadores usan `snake_case` en minúsculas, con un máximo que permite formar `plg_<plugin_id>` dentro del límite de PostgreSQL.
- Las versiones siguen SemVer 2.0.0; la metadata de build no altera precedencia.
- La compatibilidad utiliza rangos explícitos `[mínimo inclusivo, máximo exclusivo)` para impedir que una versión mayor se acepte de manera implícita.
- Las dependencias opcionales ausentes no invalidan el catálogo; si están presentes deben cumplir el rango y participan del orden topológico.
- `PluginCatalogResolver`, dentro de `kernel-domain`, devuelve el orden completo o diagnósticos tipados y deterministas; nunca un orden parcial utilizable.
- ArchUnit `1.4.2` verifica que los contratos y el dominio neutral no dependan de Jakarta, implementaciones de servidor o plugins.
- El perfil Maven `with-reference-plugin` permite probar la composición física; `J11-S1-05` agregó el descubrimiento CDI y la implementación funcional mínima del plugin.

`J11-S1-05` materializó el runtime previsto:

- `PluginRegistry`, en `kernel-application`, transforma definiciones en un catálogo validado, inmutable y consultable por identidad.
- `CdiPluginCatalog`, en `kernel-infrastructure-jakarta`, descubre `Instance<PluginDefinition>` al observar la inicialización de `ApplicationScoped`.
- Cero beans de plugin es un estado válido; una distribución sin plugins no requiere condiciones especiales en el kernel.
- Un catálogo inválido lanza `InvalidPluginCatalogException` con diagnósticos tipados y no publica un orden parcial.
- El plugin de referencia es un bean CDI propio del JAR y el kernel no importa su clase.
- El Dockerfile acepta únicamente `none` y `with-reference-plugin` como selección de composición; el argumento no es configuración de entorno ni activación empresarial.
- WildFly 41 demostró en ejecución `plugin_count=0` sin el JAR y `plugin_count=1 plugins=reference_plugin@1.0.0` con el JAR.

## Verificación

- `J11-S1-04` probó descriptores, duplicados, dependencias ausentes, ciclos y compatibilidad.
- `J11-S1-05` probó descubrimiento CDI y build con el plugin de referencia presente y ausente.
- Sprint 2 probará activación persistida y filtrado por empresa.
