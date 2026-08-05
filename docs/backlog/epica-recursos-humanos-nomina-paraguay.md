# Épica - Recursos humanos, nómina y cumplimiento paraguayo

- Estado: Planificada; implementación no autorizada hasta completar sus
  predecesores
- Plugins: `human_resources`, `payroll`, `payroll_paraguay`
- Orden vigente del roadmap: 17, 18 y 19 de 19 reutilizables
- Decisión: [ADR-0030](../adr/0030-familia-recursos-humanos-nomina-paraguay.md)
- Fuente caracterizada: `C:\cosme\felsina\ingeniolafelsina`, solo lectura

## Objetivo

Administrar la historia laboral, el tiempo, la liquidación salarial y los
artefactos paraguayos con propietarios separados, seguridad reforzada y reglas
versionadas, sin copiar el módulo legado ni cruzar tablas de otros plugins.

## Incrementos previstos

### 1. `human_resources` básico

- legajo y relación laboral por empresa;
- contratos y condiciones con vigencia;
- cargos, departamentos y asignaciones organizativas;
- documentos, dependientes y contactos necesarios;
- ingreso, cambios, suspensión y salida con historia;
- permisos separados y auditoría sin datos sensibles en logs;
- directorio, alta y ficha JSF Material Design 3 responsive.

Este incremento no calcula nómina ni genera IPS/MTESS.

### 2. Ausencias y tiempo

- feriados y calendarios empresariales;
- vacaciones, permisos, ausencias y aprobaciones;
- jornadas, turnos, marcaciones e incidencias;
- importación idempotente de reloj con cuarentena de errores;
- tardanzas, horas trabajadas y horas extra aprobadas.

### 3. `payroll` neutral

- conceptos y reglas con versión/vigencia;
- períodos, novedades, simulación y aprobación;
- cierre inmutable, recibos y snapshots de cálculo;
- bonificaciones, descuentos, anticipos y pagos especiales aprobados;
- reintentos idempotentes hacia tesorería y contabilidad mediante contratos o
  eventos.

### 4. `payroll_paraguay`

- reglas nacionales confirmadas por versión y vigencia;
- aguinaldo y liquidaciones dentro del alcance aprobado;
- artefactos IPS/MTESS con formato, checksum y trazabilidad;
- evidencia de generación, envío y respuesta cuando aplique.

No se declara cumplimiento sin validación contra fuentes oficiales vigentes.

## Límites de propiedad

| Información o regla | Propietario |
|---|---|
| empresa, identidad de acceso y autorización transversal | kernel |
| clientes, proveedores y contactos comerciales | `business_partners` |
| legajo, relación laboral, contrato, organización y tiempo | `human_resources` |
| períodos, conceptos y resultados salariales neutrales | `payroll` |
| reglas y artefactos IPS/MTESS paraguayos | `payroll_paraguay` |
| cajas, bancos, ejecución y conciliación de pagos | `treasury` |
| obligaciones, vencimientos y programación de pago | `accounts_payable` |
| cuentas, asientos, períodos y cierres | `accounting` |

Persona empleada, participante comercial y usuario pueden vincularse por IDs
públicos, pero no se unifican ni comparten entidades. Departamento organizativo y
centro de costo se relacionan mediante un mapeo explícito, sin que un plugin posea
la tabla privada del otro.

## Seguridad y privacidad

- clasificar datos personales, bancarios, salariales, familiares y disciplinarios;
- aplicar mínimo privilegio y permisos distintos por operación/finalidad;
- revalidar actor, empresa, plugin y permiso en cada servicio;
- excluir valores sensibles de logs, URLs y mensajes generales;
- auditar consulta sensible, cambios, aprobación, cierre, anulación y exportación;
- probar aislamiento multiempresa y seguridad negativa;
- definir retención, enmascarado y protección antes de cargar datos reales;
- usar solamente datos ficticios en demos y evidencia versionada.

## Selectores y experiencia visual

Cada selector declarará fuente y propietario conforme a ADR-0028. Cargos,
departamentos y otros catálogos empresariales tendrán administración autorizada e
historia activo/inactivo. Estados cerrados, permisos, operaciones y códigos
oficiales no admitirán altas arbitrarias.

Cada historia visual cubrirá 375, 720 y 1280 px, teclado, foco, contraste, vacío,
inactivos, listas grandes, errores y denegación. Las pantallas y reportes no
mostrarán datos sensibles a actores sin permiso específico.

## Criterios de aceptación de la épica

- **HR-01:** `human_resources`, `payroll` y `payroll_paraguay` tienen API, esquema,
  migraciones, descriptor, permisos, menús y pruebas propios.
- **HR-02:** no existen relaciones JPA, SQL, repositorios ni DTO internos cruzados.
- **HR-03:** contratos y asignaciones preservan vigencia e historia.
- **HR-04:** marcaciones importadas son idempotentes, trazables y reconciliables.
- **HR-05:** vacaciones, permisos e incidencias tienen aprobación y efecto
  reconstruible.
- **HR-06:** una nómina cerrada conserva entradas, reglas, versión y resultados.
- **HR-07:** pagos y asientos se integran sin duplicación y sin cambiar propietarios.
- **HR-08:** IPS/MTESS identifica fuente oficial, versión, vigencia y checksum.
- **HR-09:** permisos y pruebas negativas impiden exposición entre empresas o roles.
- **HR-10:** logs, errores, URLs, demo y auditoría general no filtran datos sensibles.
- **HR-11:** los tres rangos responsive y navegación por teclado quedan verdes.
- **HR-12:** desactivar un plugin elimina sus aportes funcionales sin borrar historia.
- **HR-13:** migración, si se aprueba, reconcilia conteos/importes y ofrece rollback.
- **HR-14:** manuales, fotografía, demo, PDF y decisión del instalador se actualizan
  en cada Sprint de la familia.

## Fuera del primer incremento

- cálculo de nómina dentro de `human_resources`;
- uso de reglas o tasas obtenidas solo del legado;
- acceso directo a tablas de tesorería, cuentas por pagar o contabilidad;
- importación de datos reales sin autorización, perfilado y protección;
- autoservicio del empleado, reclutamiento, evaluación y capacitación hasta una
  caracterización futura;
- biometría o drivers nativos de reloj no evaluados;
- certificación legal implícita por reproducir reportes del legado.

## Inicio autorizado

La aprobación de esta épica modifica el roadmap, pero no autoriza crear código
ahora. ADR-0032 agregó `fuel_station` y ADR-0033 insertó después
`recurring_billing`; ADR-0034 agregó luego `vehicle_telemetry` como orden 7.
Primero deben cerrar Sprint 8 y los plugins 4 a 16. Cumplidas esas
condiciones, la primera historia será la caracterización detallada y el diseño del
núcleo `human_resources`, expresamente sin nómina.

## Referencias

- [Caracterización de Recursos Humanos](../knowledge-base/human-resources/legacy-characterization.md)
- [Roadmap productivo](epica-roadmap-plugins-productivos.md)
- [ADR-0028 - Gobierno de selectores](../adr/0028-gobierno-de-selectores-y-datos-administrables.md)
- [ADR-0032 - Plugin para estaciones de servicio](../adr/0032-plugin-estaciones-servicio-combustible.md)
- [ADR-0033 - Dominio de facturación recurrente](../adr/0033-dominio-facturacion-recurrente.md)
- [ADR-0034 - Plugin de telemetría vehicular](../adr/0034-plugin-telemetria-vehicular.md)
