# Recursos humanos y nómina: caracterización de Ingenio La Felsina

- Estado: Factibilidad y decisiones HR-D01 a HR-D10 aprobadas; implementación no
  autorizada hasta completar los predecesores
- Fecha de revisión: 2026-08-02
- Fuente: `C:\cosme\felsina\ingeniolafelsina`
- Alcance inspeccionado: `fuente/tag`, `reportes` y `scripts`
- Rama observada: `16772_main_generar_asiento_en_actualizacion_factura`
- Commit observado: `412b3cd978757b1b8a389f2007060a90f5c7322b`
- Tratamiento: Fuente de conocimiento de solo lectura
- Decisiones rectoras: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Propósito

Determinar si el comportamiento de recursos humanos observado en Ingenio La
Felsina justifica capacidades nuevas en Logixone, qué límites deberían tener y
qué acoplamientos del sistema legado no deben reproducirse.

Este documento caracteriza comportamiento. No certifica exactitud laboral,
previsional o tributaria, no declara vigente ningún cálculo y no autoriza copiar
código, tablas, reportes ni archivos de intercambio.

## Evidencia revisada

| Evidencia | Magnitud observada | Uso en el análisis |
|---|---:|---|
| Java bajo `py.com.ping.rrhh` | 154 archivos | Entidades, controladores, repositorios, DTO y reglas |
| JPA bajo `rrhh/jpa` | 64 archivos | Datos, relaciones y límites mezclados |
| CDI/controladores bajo `rrhh/cdi` | 58 archivos | Casos de uso y recorridos de pantalla |
| DAO bajo `rrhh/dao` | 9 archivos | Consultas y persistencia |
| DTO bajo `rrhh/dto` | 16 archivos | Importaciones, cálculos y salidas |
| XHTML bajo `webapp/rrhh` | 215 archivos | Tareas, formularios y reportes visibles |
| Artefactos de reporte relacionados | 69 archivos | Salarios, empleados, IPS, MTESS, vacaciones y aguinaldo |
| Script relacionado localizado | 1 archivo | Corrección reciente de monto unitario de planilla salarial |
| `WEB-INF/menuRecursosHumanos.xhtml` | 1 menú | Inventario de capacidades y permisos visibles |
| `pom.xml` de `fuente/tag` | Java 8, WildFly 25, Jakarta EE 8 y dependencias `javax.*` | Evidencia de incompatibilidad técnica directa |

Las cantidades describen la fotografía revisada del commit indicado. No son una
estimación de esfuerzo ni garantizan que cada archivo siga siendo funcional.

## Capacidades observadas

| Grupo | Comportamiento visible | Propietario candidato en Logixone |
|---|---|---|
| Personas empleadas | Alta y consulta de funcionarios, datos personales, ingreso y salida, situación, documentos, dependientes y cuentas bancarias | `human_resources` |
| Relación laboral | Contratos, cargo, categorías, departamento, tipo de salario, forma de cobro y motivo de salida | `human_resources` |
| Organización | Cargos, departamentos/centros y asociaciones operativas | `human_resources`, con referencias públicas hacia costos/contabilidad |
| Ausencias | Vacaciones, propuestas de vacaciones, permisos, feriados y amonestaciones | `human_resources` |
| Tiempo trabajado | Turnos, marcaciones, importación de reloj, tardanzas, horas trabajadas y horas extra | Capacidad interna de `human_resources`, preparada para una separación futura |
| Nómina | Planilla salarial, bonificaciones, complementos, descuentos, provisiones y salario neto | `payroll` |
| Pagos especiales | Anticipos, vacaciones pagadas, aguinaldo y liquidaciones de salida | `payroll` |
| Integración financiera | Cronogramas de pago, comprobantes, bancos, cuentas contables y listados bancarios | Contratos/eventos de `payroll` hacia `treasury` y `accounting` |
| Cumplimiento paraguayo | Planillas y salidas IPS y MTESS | `payroll_paraguay` versionado contra fuentes oficiales |
| Informes | Listas de empleados, movimientos, marcaciones, anticipos, salarios y salidas normativas | Cada plugin es dueño de sus informes; analítica cruzada usará proyecciones |

El menú aplica verificaciones de permiso por opción. Esto confirma que la
autorización es parte del comportamiento que debe preservarse, pero ocultar una
opción no basta: Logixone debe revalidar empresa y permiso en el servicio.

## Reglas e invariantes candidatas

Las siguientes reglas se infieren del modelo y los recorridos; requieren
confirmación con usuarios y pruebas de caracterización antes de implementar:

1. La persona y su relación laboral no son el mismo concepto. Un empleado puede
   conservar historia de contratos, cambios de cargo, categoría, departamento,
   remuneración y salida sin reescribir períodos ya liquidados.
2. Una nómina representa un ciclo con cabecera, participantes, conceptos,
   incidencias, descuentos, aportes, totales y estado. Sus resultados finalizados
   deben conservar los valores y reglas usados en el cálculo.
3. Marcaciones importadas, jornadas calculadas y novedades aprobadas son etapas
   diferentes. La importación debe ser idempotente, trazable y capaz de informar
   filas rechazadas sin duplicar incidencias.
4. Vacaciones, permisos, tardanzas, horas extra y ausencias deben tener rango,
   estado, responsable y evidencia suficiente para reconstruir su efecto en la
   liquidación.
5. Aguinaldo, vacaciones pagadas y liquidación de salida no deben modelarse como
   campos ocasionales de una planilla genérica sin ciclo de vida propio.
6. Finalizar, anular o recalcular una liquidación requiere permisos distintos,
   control de concurrencia, motivo y auditoría. Una ejecución repetida no debe
   duplicar pagos ni asientos.
7. La instrucción de pago y el asiento contable son efectos posteriores. Nómina
   publica contratos o eventos; no persiste entidades privadas de tesorería o
   contabilidad.
8. Los artefactos regulatorios deben conservar versión, período, checksum,
   estado de generación, envío/respuesta cuando aplique y una instantánea de los
   datos declarados.
9. Datos personales, bancarios, salariales, familiares y disciplinarios requieren
   permisos por finalidad, minimización, trazabilidad y exclusión de logs normales.
10. Desactivar el plugin para una empresa no elimina empleados, contratos,
    liquidaciones, documentos regulatorios ni evidencias históricas.

## Deuda del legado que no debe reproducirse

### Agregado de empleado sobredimensionado

`RhwEmpleado` concentra identidad, relación laboral, salario, jornada, IPS,
bonificaciones, descuentos y datos operativos. Esa forma impide evolucionar y
proteger cada conjunto con ciclos y permisos propios. Logixone debe separar perfil
personal, empleo, contrato/condición, asignación organizativa, compensación y
novedades con vigencia temporal.

### Relaciones JPA entre dominios

Entidades de RR. HH. importan directamente empresas, usuarios, ciudades,
profesiones, clientes, bancos, plan de cuentas, comprobantes, cronogramas de pago y
turnos de ventas. Entre los archivos Java revisados, 122 referencian utilitarios
compartidos, 66 administración base, 38 listeners, 13 ventas, 7 tesorería y 4
contabilidad. El nuevo diseño usará identificadores, contratos públicos y eventos;
no habrá asociaciones JPA ni consultas a tablas privadas de otro plugin.

### Tecnología incompatible

La fuente usa Java 8, WildFly 25, Jakarta EE 8, PrimeFaces 10 y varias dependencias
`javax.*`, además de implementaciones CDI incluidas explícitamente. Logixone usa
Java 21, Jakarta EE 11, WildFly 41, Jakarta Faces 4.1 y APIs del servidor con alcance
`provided`. Solo se trasladan comportamientos caracterizados y datos mapeados; no
clases, XHTML, POM, entidades ni configuración del legado.

### Reglas legales incrustadas

Campos y reportes mencionan IPS, MTESS, tasas, aportes, aguinaldo y liquidaciones.
La sola existencia de esos artefactos no prueba vigencia ni cumplimiento. Antes
de calcular o exportar se deben identificar las fuentes oficiales aplicables,
fechas de vigencia, fórmulas, catálogos, formatos, casos límite y checksums.

## Límite recomendado

El alcance es viable, pero no conviene agregar un único plugin monolítico llamado
`rrhh` que replique toda la carpeta legada. Se recomienda esta familia:

| Plugin | Responsabilidad | Dependencias funcionales candidatas |
|---|---|---|
| `human_resources` | Persona empleada, empleo, contratos, estructura organizativa, dependientes, documentos, vacaciones/permisos y control de tiempo | Kernel por empresa/identidad mediante contratos; `business_partners` solo si se aprueba una vinculación opcional por identificador |
| `payroll` | Períodos, conceptos, novedades, cálculo, aprobación, recibos, aguinaldo, vacaciones pagadas, anticipos y liquidaciones | Requiere API pública de `human_resources`; integra pagos/asientos mediante puertos o eventos cuando existan `treasury` y `accounting` |
| `payroll_paraguay` | Reglas y artefactos regulatorios locales de IPS/MTESS con versiones y vigencias explícitas | Requiere contratos públicos de `payroll`; no accede a sus tablas |

Cada plugin funcional tendrá su correspondiente módulo API Java puro, esquema
privado, descriptor, migraciones, permisos, menús y pruebas. La capacidad de
tiempo trabajado puede comenzar como subdominio interno de `human_resources`, con
un puerto para importadores de reloj, y separarse en el futuro si aparecen varios
proveedores, gran volumen o despliegues que no necesiten control horario.

### Relación con otros dominios

- Un empleado no se convierte automáticamente en cliente o proveedor. Una posible
  coincidencia con `business_partners` se expresa mediante un identificador
  opcional y un caso de uso explícito.
- La identidad de acceso y el legajo de empleado son conceptos diferentes. El
  vínculo opcional usa un identificador público del kernel, nunca una entidad JPA.
- Departamento organizativo y centro de costo pueden mapearse, pero ninguno debe
  poseer la tabla del otro.
- `payroll` solicita o anuncia obligaciones de pago; `treasury` decide su
  ejecución y conciliación.
- `accounting` consume hechos aprobados para contabilizar con su propia política.

## Incrementos sugeridos

1. **Gobierno y caracterización:** aprobar ADR, épica, glosario, permisos,
   tratamiento de datos sensibles, fuentes oficiales y matriz de migración.
2. **Núcleo de RR. HH.:** legajo, relación laboral, contrato con vigencia, cargos,
   departamentos, documentos, dependientes, altas/bajas y auditoría; sin cálculo
   de nómina.
3. **Ausencias y tiempo:** calendarios, vacaciones, permisos, jornadas,
   marcaciones importadas e incidencias aprobadas.
4. **Nómina neutral:** conceptos y fórmulas versionadas, períodos, novedades,
   simulación, aprobación, cierre, recibos e idempotencia.
5. **Paraguay:** aguinaldo y liquidaciones según alcance confirmado, IPS/MTESS y
   exportaciones verificadas contra especificaciones oficiales vigentes.
6. **Finanzas y migración:** pagos, contabilidad, bancos, conciliación y carga
   controlada de historia desde fuentes anonimizadas o protegidas.

Cada incremento requiere Jakarta Faces/Material Design 3 responsive, inventario de
selectores, autorización de servidor, pruebas de seguridad negativa y demo real.

## Riesgos principales

| Riesgo | Tratamiento requerido antes de codificar |
|---|---|
| Exposición de datos sensibles | Clasificación, permisos separados, auditoría, retención, enmascarado y pruebas de no filtración |
| Cálculos legales desactualizados | Fuentes oficiales vigentes, reglas con fecha/versión y pruebas de ejemplos aprobados |
| Duplicación de persona/empleado/usuario | Identidades separadas y vinculación explícita por ID público |
| Acoplamiento financiero | Puertos/eventos idempotentes; ninguna entidad o tabla cruzada |
| Importación de reloj inconsistente | Formato versionado, clave idempotente, cuarentena de errores y conciliación |
| Migración de datos heterogéneos | Perfilado, mapeo, limpieza, reconciliación y rollback documentados |
| Plugin inicial demasiado grande | Entregas verticales y separación de `payroll` y `payroll_paraguay` |

## Pruebas de caracterización propuestas

- alta, cambio y baja de una relación laboral preservando historia;
- solapamiento y vigencia de contratos, cargos y asignaciones;
- generación repetida de marcaciones sin duplicados y con errores parciales;
- cálculo de jornada diurna/nocturna, descanso, tardanza, feriado y hora extra;
- solicitud, aprobación y saldo de vacaciones o permisos;
- simulación y cierre de nómina con conceptos positivos y descuentos;
- recálculo antes del cierre y rechazo de mutación silenciosa después del cierre;
- aguinaldo, vacaciones pagadas y liquidación de salida con ejemplos confirmados;
- reintento idempotente de instrucción de pago y contabilización;
- acceso denegado entre empresas, por rol insuficiente y con plugin inactivo;
- verificación de que logs, errores, reportes no autorizados y auditoría general
  no revelen salarios, cuentas bancarias ni documentos personales;
- exportación IPS/MTESS validada contra la versión oficial elegida.

## Decisiones confirmadas de producto

El responsable de producto aprobó sin cambios las diez recomendaciones el
2026-08-02 y confirmó conservar el orden vigente. ADR-0030 materializó la familia
después de `accounting`; ADR-0032 agregó `fuel_station` y ADR-0033 insertó
posteriormente `recurring_billing` como orden 8, por lo que la familia ocupa ahora
los órdenes 16 a 18. ADR-0034 insertó luego `vehicle_telemetry` como orden 7, por
lo que la familia ocupa actualmente los órdenes 17 a 19, antes de la
personalización.

| ID | Decisión confirmada | Resultado aprobado |
|---|---|---|
| HR-D01 | ¿Se incorpora esta familia al roadmap? | Sí, mediante nueva épica y ADR; no alterar ADR-0011 implícitamente |
| HR-D02 | ¿El primer incremento incluye nómina? | No; comenzar con el núcleo de `human_resources` |
| HR-D03 | ¿RR. HH., nómina y cumplimiento local son plugins separados? | Sí: `human_resources`, `payroll` y `payroll_paraguay` |
| HR-D04 | ¿Cómo se vinculan persona, empleado y usuario? | Identidades separadas con enlaces opcionales por ID público |
| HR-D05 | ¿Quién posee departamentos y centros de costo? | RR. HH. posee estructura; contabilidad posee centros y recibe un mapeo explícito |
| HR-D06 | ¿Qué empresas, convenios y tipos de remuneración entran primero? | Limitar la primera versión a un conjunto confirmado y documentado |
| HR-D07 | ¿Qué relojes y formatos de marcación se soportan? | Definir adaptadores después de obtener muestras no sensibles |
| HR-D08 | ¿Qué versiones oficiales de IPS/MTESS y reglas laborales aplican? | Verificarlas antes del diseño de `payroll_paraguay` |
| HR-D09 | ¿Se migrará historia del legado? | Decidir períodos, calidad, reconciliación y custodia antes de extraer datos |
| HR-D10 | ¿En qué punto del roadmap se construirá? | ADR-0030 la agregó históricamente en 14 a 16; el orden vigente tras ADR-0034 es 17 a 19 y no comienza mientras Sprint 8 o cualquier predecesor siga pendiente |

## Validación documental

El gate G0 más reciente, posterior a la aprobación y al séptimo corte de J11-S8-C02, recorrió 258 archivos Markdown y 1037
enlaces locales: cero errores UTF-8, cero marcadores de texto dañado y cero
enlaces rotos. La aprobación de esta caracterización no requirió por sí misma
Maven, Docker ni Playwright porque sólo registra conocimiento; los gates ejecutables
del séptimo corte se documentan en la evidencia de J11-S8-C02.

## Conclusión y siguiente paso

La factibilidad funcional es **positiva con condiciones**: existe suficiente
comportamiento real para justificar la familia, pero copiar el módulo legado
recrearía acoplamientos, tecnología obsoleta y reglas legales no versionadas.

ADR-0030 y la nueva épica incorporan la familia al final de los reutilizables,
sin reordenar los trece anteriores. Sprint 8 continúa abierto en la corrección
`J11-S8-C02`; no está autorizado iniciar otro plugin. Después deben completarse
los predecesores 4 a 16. Recién entonces corresponderá planificar la primera
historia de `human_resources`, sin nómina.
