# Épica — Kernel multiempresa y activación de plugins

- Estado: Capacidades implementadas y gates técnicos verdes; validación independiente G7 de la guía candidata pendiente
- Fecha: 2026-07-27
- Origen: ADR-0002, ADR-0003 y cierre de Sprint 1

## Problema

La distribución ya descubre y valida los plugins físicamente presentes, pero todavía no puede decidir cuáles están habilitados para una empresa. Sin esa capacidad, las contribuciones de menú, permisos y operaciones serían globales y el ERP repetiría el acoplamiento del sistema legado.

## Resultado esperado

El kernel debe representar empresas y conservar una decisión de activación independiente para cada par empresa/plugin. Además, cada empresa debe tener exactamente un plugin de personalización propio, obligatorio y aplicado como última capa. A partir de esas decisiones y del catálogo físico, el kernel debe calcular un estado efectivo seguro y filtrar o componer las contribuciones antes de ejecutar una operación o construir una interfaz.

## Reglas funcionales mínimas

1. Una empresa tiene identidad opaca y estado de ciclo de vida controlado por el kernel.
2. No existe empresa implícita, predeterminada ni derivada de datos aportados sin validar.
3. La activación se persiste por empresa y por `PluginId`; no modifica la composición física del WAR.
4. La ausencia de una decisión persistida no habilita silenciosamente un plugin.
5. Una empresa inactiva no puede ejecutar capacidades de plugins.
6. No puede activarse un plugin ausente de la distribución vigente.
7. Para activar un plugin, todas sus dependencias requeridas deben estar físicamente presentes, ser compatibles y estar activas para la misma empresa.
8. No puede desactivarse un plugin mientras otro plugin activo de esa empresa lo requiera.
9. Las dependencias opcionales no se activan automáticamente.
10. El cálculo efectivo es determinista y nunca mezcla decisiones de empresas distintas.
11. Un plugin no efectivo no aporta menús ni permisos operativos y toda operación protegida por ese plugin se deniega antes de ejecutar lógica funcional.
12. Desactivar o retirar físicamente un plugin no elimina sus decisiones, migraciones ni datos.
13. Cada empresa operativa tiene exactamente un plugin de tipo personalización asignado y cada plugin de personalización pertenece a una sola empresa.
14. El plugin de personalización asignado es obligatorio y no participa del flujo normal de desactivación; su sustitución es explícita, transaccional y auditable.
15. Todos los plugins funcionales efectivos se componen antes del plugin de personalización de la empresa.
16. Los plugins de personalización pertenecientes a otras empresas son invisibles e inejecutables para la empresa consultada.

## Restricciones de seguridad

- Sprint 2 no implementará login, sesión ni proveedor OIDC; esa decisión permanece en Sprint 3.
- No se aceptará un header HTTP arbitrario como fuente confiable de empresa activa.
- No se publicarán endpoints administrativos sin un modelo de autorización aprobado.
- Los casos de uso deben exigir empresa explícita o un puerto de contexto confiable y no pueden usar valores globales o `ThreadLocal` manual.
- Toda consulta y mutación persistente debe estar acotada por empresa y probar aislamiento negativo.
- Los errores públicos futuros usarán códigos estables y no divulgarán existencia de otras empresas, SQL ni datos internos.

## Restricciones de datos

- Las tablas pertenecen al esquema `core` y evolucionan mediante una migración nueva; V1 permanece inmutable.
- JPA valida el esquema y nunca ejecuta `create`, `update` ni `drop` en ambientes compartidos.
- La escritura de una decisión de activación debe ser transaccional, idempotente y resistente a actualizaciones concurrentes según la estrategia que acepte `J11-S2-01`.
- No existen asociaciones JPA hacia entidades de plugins.
- La desactivación es reversible y no constituye eliminación de datos empresariales.

## Decisiones que debe cerrar J11-S2-01

- representación y generación de `CompanyId`;
- ciclo de vida mínimo de una empresa;
- estado persistido y significado de una fila de activación ausente;
- política al retirar y volver a incorporar un plugin;
- concurrencia, idempotencia y versionado optimista;
- forma del puerto neutral de contexto empresarial;
- códigos de diagnóstico y eventos de auditoría disponibles antes de contar con identidad de usuario;
- frontera exacta entre `kernel-api`, dominio, aplicación e infraestructura.
- categoría explícita de plugin, relación uno a uno entre empresa y personalización y política ante ausencia o incompatibilidad del JAR obligatorio;
- semántica transaccional para reemplazar una personalización sin dejar un estado intermedio inválido;
- frontera y versionado del contrato público que permite personalizar pantallas sin importar internos de otro plugin.

## Fuera de esta épica

- usuarios, roles, autenticación, OIDC y sesión web;
- UI administrativa, menú visual o renderizado real de pantallas; el contrato neutral de personalización sí forma parte de la planificación;
- dominios de ventas, inventario, transporte o facturación;
- plugins persistentes y descubrimiento de sus migraciones;
- borrado físico de empresas o datos de plugins;
- selección dinámica de JAR o hot deployment.

## Medida de éxito

Dos empresas almacenadas en el mismo PostgreSQL pueden tener estados de activación y composiciones distintas sobre el mismo catálogo físico. Las pruebas demuestran que cada empresa usa exactamente su personalización obligatoria después de los plugins funcionales, que una capacidad o personalización de una empresa no se filtra ni se ejecuta para la otra, que las dependencias se respetan y que el resultado sobrevive a reinicios y recreación de contenedores.

El diseño especializado y sus límites se detallan en [Personalización obligatoria por empresa](epica-personalizacion-pantallas-por-empresa.md).

## Incremento implementado

`J11-S2-02` materializó la identidad UUID, el contexto neutral, la categoría de plugin, la intención de activación, la resolución efectiva por empresa, la cuarentena de personalización y las políticas puras de dependencias. `J11-S2-03` congeló el esquema `core` V2; `J11-S2-04` implementó repositorios JPA/JTA; `J11-S2-05` agregó casos de uso, auditoría y guarda; y `J11-S2-06` proyectó capacidades, permisos y menús únicamente desde plugins efectivos, con la personalización empresarial al final. Persisten fuera de este incremento la resolución runtime del contexto desde identidad autenticada, la concesión de permisos a usuarios y la UI.
