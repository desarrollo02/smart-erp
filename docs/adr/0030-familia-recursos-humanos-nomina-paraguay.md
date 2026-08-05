# ADR-0030 - Familia de recursos humanos, nómina y cumplimiento paraguayo

- Estado: Aceptado
- Fecha: 2026-08-02
- Decisión de producto: aprobar HR-D01 a HR-D10 y agregar la familia después de
  los trece plugins ya ordenados
- Modifica: cantidad, orden y fuera de alcance de
  [ADR-0011](0011-roadmap-dependencias-plugins-productivos.md), ampliado antes por
  [ADR-0027](0027-terminal-punto-venta-y-ampliacion-roadmap.md)

> Nota vigente: esta ADR conserva la decisión histórica que agregó la familia en
> los órdenes 14–16. [ADR-0032](0032-plugin-estaciones-servicio-combustible.md)
> insertó después `fuel_station`; [ADR-0033](0033-dominio-facturacion-recurrente.md)
> agregó `recurring_billing` como orden 8 y
> [ADR-0034](0034-plugin-telemetria-vehicular.md) insertó luego
> `vehicle_telemetry` como orden 7. El roadmap actual tiene diecinueve
> reutilizables y la familia ocupa los órdenes 17–19.

## Contexto

Producto autorizó usar `C:\cosme\felsina\ingeniolafelsina` como segunda fuente
legada de conocimiento de solo lectura. Su caracterización revela empleados,
contratos, estructura organizativa, ausencias, marcaciones, nómina, aguinaldo,
liquidaciones, anticipos, IPS y MTESS. También revela un agregado de empleado
sobredimensionado, relaciones JPA con administración, ventas, tesorería y
contabilidad, y tecnología Java 8/Jakarta EE 8 incompatible con el baseline.

Incorporar toda esa superficie como un único plugin `rrhh` recrearía el
acoplamiento del legado. Incorporar cálculos o formatos IPS/MTESS sin versión y
fuente oficial convertiría comportamiento histórico en una promesa de
cumplimiento que la evidencia no sostiene.

La secuencia vigente ya contiene trece plugins reutilizables. Producto confirmó
que no se interrumpe: Sprint 8 y todos los predecesores conservan su orden, y el
primer incremento futuro de esta familia será RR. HH. sin nómina.

## Decisión

### 1. Familia, cantidad y orden

Se agregan tres plugins funcionales reutilizables:

14. `human_resources`;
15. `payroll`;
16. `payroll_paraguay`.

La personalización `<empresa>_customization` permanece siempre al final. Al
aceptarse esta ADR, el roadmap pasó a **dieciséis plugins reutilizables**. En ese
corte, una distribución con los dieciséis y personalizaciones para `N` empresas
podía contener `16 + N` plugins productivos; ADR-0032 y ADR-0033 actualizan luego
la fórmula hasta `18 + N`. La presencia física no implica activación para cada
empresa.

Ubicar la familia después de `accounting` preserva sin desplazamientos los trece
predecesores aprobados. El orden de construcción no obliga a que RR. HH. dependa de
contabilidad. Nómina publicará hechos e instrucciones mediante contratos o eventos
para que tesorería y contabilidad sigan siendo dueñas de pagos y asientos.

### 2. Responsabilidad de `human_resources`

Será dueño de:

- legajo de la persona empleada y su relación laboral por empresa;
- contratos y condiciones con vigencia e historia;
- cargos, departamentos y estructura organizativa;
- documentos, dependientes y datos de contacto necesarios;
- altas, cambios, suspensiones y bajas con motivo y auditoría;
- vacaciones, permisos, feriados e incidencias laborales;
- jornadas, marcaciones y tiempo trabajado como subdominio inicial separable.

No calculará nómina, no ejecutará pagos, no creará asientos y no generará
declaraciones regulatorias. Persona empleada, participante comercial y usuario de
acceso son identidades distintas, vinculables opcionalmente por identificadores
públicos y casos de uso explícitos.

### 3. Responsabilidad de `payroll`

Será dueño de:

- conceptos y reglas neutrales versionadas;
- períodos, novedades, simulación, aprobación y cierre;
- resultados por empleado, recibos y snapshots de las reglas aplicadas;
- bonificaciones, descuentos, anticipos, vacaciones pagadas, aguinaldo y
  liquidaciones dentro del alcance confirmado por historia;
- idempotencia de recálculo, cierre e integración financiera.

Requerirá el contrato público de `human_resources`. La dependencia exacta con
tesorería, cuentas por pagar o contabilidad se diseñará como puertos y eventos
acíclicos; no se autorizan entidades, repositorios, DTO internos, joins ni SQL
entre esquemas.

### 4. Responsabilidad de `payroll_paraguay`

Será el adaptador nacional para reglas y artefactos de IPS/MTESS que producto
apruebe. Dependerá de contratos públicos de `payroll`, no de sus tablas. Cada
regla, catálogo o formato conservará fuente oficial, versión, vigencia y checksum.
Cada artefacto generado conservará período, versión, checksum, estado y evidencia
de envío/respuesta cuando aplique.

El plugin no se implementará ni se presentará como conforme hasta verificar las
especificaciones oficiales vigentes, ejemplos y casos límite con responsables
laborales/contables autorizados.

### 5. Datos sensibles, historia y seguridad

Cada plugin tendrá módulo API Java puro, esquema privado, migraciones inmutables,
descriptor, permisos, menús y pruebas. El diseño debe:

- clasificar datos personales, bancarios, salariales, familiares y disciplinarios;
- separar permisos de consulta, administración, aprobación, cierre, anulación y
  exportación;
- revalidar actor, empresa, plugin y permiso en el servicio;
- excluir datos sensibles de logs y mensajes generales;
- conservar historia efectiva sin reescribir contratos o liquidaciones cerradas;
- impedir cruces entre empresas y probar seguridad negativa;
- conservar datos al desactivar o retirar físicamente un plugin.

El tratamiento de retención, enmascarado y protección de campos se aprobará antes
de persistir datos reales.

### 6. Interfaz y selectores

Las pantallas usarán Jakarta Faces 4.1, Material Design 3 y contratos neutrales
renderizados por el shell. Cada selector declarará fuente, propietario y clase.
Los catálogos empresariales ofrecerán administración autorizada; estados cerrados,
permisos y códigos oficiales no aceptarán altas arbitrarias.

Cada incremento demostrará compacto, medio y expandido, teclado, foco, contraste,
vacíos, inactivos, listas grandes, error y acceso denegado sin exponer datos
reales en la demo.

### 7. Orden interno de entrega

1. gobierno, glosario, fuentes oficiales, seguridad y migración;
2. `human_resources` básico sin nómina;
3. ausencias y control de tiempo;
4. `payroll` neutral;
5. `payroll_paraguay`;
6. integración financiera y migración histórica autorizada.

No se crea ahora ningún módulo Maven. Sprint 8 continúa abierto en
`J11-S8-C02`; después deben construirse en orden los plugins 4 a 16. Una nueva
historia de implementación de `human_resources` solo podrá planificarse cuando sus
predecesores estén verdes y se hayan caracterizado permisos, datos y migración.

## Consecuencias

### Positivas

- RR. HH. puede existir sin activar nómina o adaptadores paraguayos;
- reglas locales volátiles no contaminan el dominio neutral;
- empleados, pagos y asientos conservan propietarios distintos;
- la historia laboral y salarial se diseña explícitamente;
- el roadmap previo no se reordena ni se interrumpe.

### Costes y riesgos

- el roadmap crece de trece a dieciséis plugins reutilizables;
- los datos requieren controles de privacidad y seguridad superiores a un maestro
  comercial ordinario;
- nómina exige reglas temporales, reproducibilidad e idempotencia estrictas;
- IPS/MTESS requieren mantenimiento normativo continuo y verificación oficial;
- la migración desde el legado puede requerir limpieza y reconciliación extensas.

## Alternativas descartadas

### Copiar el módulo `rrhh` de Ingenio La Felsina

Se descarta por incompatibilidad Java/Jakarta, agregado sobredimensionado,
dependencias `javax.*` y relaciones directas con tablas y entidades ajenas.

### Un único plugin para RR. HH., nómina, IPS y MTESS

Se descarta porque impide activación independiente y mezcla historia laboral,
cálculo financiero y cumplimiento por país.

### Agregar RR. HH. antes de terminar la secuencia vigente

Se descarta para no interrumpir Sprint 8 ni desplazar contratos ya aprobados. El
beneficio de adelantar el legajo no compensa mantener dos frentes productivos con
gates pendientes.

### Representar empleado como participante comercial o usuario

Se descarta porque los ciclos de vida, permisos y datos sensibles son diferentes.
Los vínculos opcionales no convierten una identidad en otra.

## Condiciones antes de implementar

1. cerrar Sprint 8 y completar en orden los plugins 4 a 16;
2. caracterizar el primer alcance con usuarios de RR. HH. sin datos reales;
3. aprobar clasificación, permisos, retención, auditoría y seguridad negativa;
4. definir contratos públicos y evitar ciclos con identidad, participantes,
   tesorería y contabilidad;
5. obtener muestras protegidas de relojes o migración solo con autorización;
6. verificar reglas y formatos paraguayos contra fuentes oficiales vigentes;
7. planificar demo real, manuales, fotografía, PDF y decisión de instalador del
   Sprint correspondiente.

## Referencias

- [Caracterización de Ingenio La Felsina](../knowledge-base/human-resources/legacy-characterization.md)
- [ADR-0011 - Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0034 — Plugin de telemetría vehicular](0034-plugin-telemetria-vehicular.md)
- [ADR-0013 - Eventos e idempotencia](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0032 - Plugin para estaciones de servicio de combustible](0032-plugin-estaciones-servicio-combustible.md)
- [ADR-0033 - Dominio independiente de facturación recurrente](0033-dominio-facturacion-recurrente.md)
- [ADR-0016 - Autorización y auditoría de plugins](0016-autorizacion-y-auditoria-operaciones-plugin.md)
- [ADR-0017 - Interacción visual neutral](0017-interaccion-visual-neutral-de-plugins.md)
- [ADR-0028 - Gobierno de selectores](0028-gobierno-de-selectores-y-datos-administrables.md)
- [Épica de recursos humanos, nómina y Paraguay](../backlog/epica-recursos-humanos-nomina-paraguay.md)
