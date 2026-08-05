# J11-S6-00 - Gobierno y planificación de `business_partners`

- Estado: Completada documentalmente
- Sprint: 6
- Fecha: 2026-07-29
- Tipo: gobierno, alcance y autorización
- Dependencia: gates técnicos G0-G6 de Sprint 5 verdes; G7 independiente pendiente
- ADR rectores: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md),
  [ADR-0012](../../adr/0012-composicion-unica-y-migraciones-de-plugins.md) y
  [ADR-0013](../../adr/0013-eventos-integracion-outbox-por-plugin.md)

## Objetivo

Definir cómo se descubrirá y construirá el primer plugin ERP productivo sin copiar
el legado, adelantar persistencia ni convertir el kernel en propietario de clientes
o proveedores. Esta historia autoriza únicamente la caracterización `J11-S6-01`.

## Identidad y frontera inicial

| Elemento | Decisión inicial |
|---|---|
| Plugin | `business_partners` |
| Tipo | `FUNCTIONAL` |
| Esquema futuro | `plg_business_partners` |
| Propietario conceptual | personas, organizaciones, roles comerciales, contactos y direcciones |
| Consumidores previstos | compras, ventas, logística, documentos y cuentas por cobrar/pagar mediante contratos públicos |
| Exclusiones | ventas, compras, saldo, crédito, stock, documentos, cobranzas, contabilidad y SIFEN |

Los términos de esta tabla son hipótesis de alcance, no clases ni tablas aprobadas.
`J11-S6-01` deberá confirmarlos, separarlos o rechazarlos con evidencia.

## Fuente de conocimiento

El proyecto `C:\cosme\multienvios\miaterra` se consulta en modo de solo lectura.
Se permite observar pantallas, controladores, entidades, consultas, validaciones,
permisos y reportes relacionados. Está prohibido modificarlo o copiar código de
forma mecánica.

Cada hallazgo debe registrar:

1. fuente concreta;
2. comportamiento observado;
3. requisito neutral propuesto;
4. confianza y dudas;
5. decisión de conservar, simplificar, separar o descartar.

Una clase o columna existente no se convierte automáticamente en parte del nuevo
modelo. Los nombres del legado son evidencia, no contratos públicos.

## Alcance de `J11-S6-01`

- glosario de términos y sinónimos;
- actores y permisos observados;
- alta, consulta, modificación, activación/inactivación y búsqueda;
- distinción entre persona y organización si existe;
- clasificación cliente/proveedor y coexistencia de roles;
- identificaciones tributarias y documentos, sin certificar reglas fiscales;
- contactos, teléfonos, correos y direcciones;
- relaciones con ventas, compras, logística, documentos y finanzas;
- reglas, invariantes, duplicados, estados y concurrencia observables;
- datos históricos que procesos posteriores deberán copiar como snapshot;
- riesgos, ambigüedades y decisiones que requieren intervención del producto.

## Fuera de alcance

- crear el módulo Maven o ejecutar el generador;
- escribir Java, SQL, migraciones, XHTML o CSS;
- decidir entidades JPA, cardinalidades definitivas o claves físicas;
- importar tablas o datos del legado;
- implementar cuentas corrientes, límites de crédito o listas de precio;
- diseñar documentos comerciales o integración SIFEN;
- materializar outbox sin productor, consumidor y evento reales;
- construir la personalización de una empresa antes de estabilizar contratos.

## Secuencia y gates

| Historia | Gate principal | Condición para avanzar |
|---|---|---|
| `J11-S6-00` | G0 gobierno | alcance y fuentes documentados |
| `J11-S6-01` | G0 caracterización | requisitos, invariantes y preguntas aceptados |
| `J11-S6-02` | G1 dominio/contratos | modelo neutral y pruebas unitarias verdes |
| `J11-S6-03` | G2 datos | migraciones y PostgreSQL/Testcontainers verdes |
| `J11-S6-04` | G3 aplicación/seguridad | casos de uso, permisos y auditoría verdes |
| `J11-S6-05` | G4 UI | JSF Material 3 y responsive verdes |
| `J11-S6-06` | G5 composición/operación | WAR/migrador, guía y Docker verdes |
| `J11-S6-07` | G6 cierre técnico | integración, demo, retrospectiva y PDF verdes |
| transversal | G7 independiente | validación externa antes de cierre formal/promoción |

No hay excepción de pruebas para Sprint 6. Después de `J11-S6-01`, cada cambio de
código ejecutará inmediatamente su prueba mínima y no avanzará con una prueba
relevante fallando.

## Riesgos a controlar

| Riesgo | Control |
|---|---|
| trasladar una entidad grande y acoplada | caracterizar casos de uso antes del modelo |
| confundir cliente/proveedor con personas distintas | estudiar roles y coexistencia |
| mover crédito o saldo al maestro | asignar cada regla al plugin financiero/comercial correcto |
| usar RUC como identidad técnica | separar identificador interno de identificaciones externas |
| perder historia al cambiar el maestro | definir qué consumidores conservan snapshots |
| deduplicar agresivamente datos reales | documentar candidatos y revisión humana |
| compartir tablas con plugins futuros | contratos/IDs públicos y propiedad exclusiva del esquema |
| crear personalización prematura | publicar primero slots y versiones estables |

## Demo visual objetivo

La demo de Sprint 6 deberá mostrar datos ficticios y operaciones reales del plugin:

1. catálogo físico y activación de `business_partners` por empresa;
2. lista y búsqueda de participantes;
3. alta y edición autorizadas;
4. cliente, proveedor y ambos roles cuando el modelo aceptado lo permita;
5. contactos y direcciones;
6. inactivación sin borrado de historia;
7. acceso denegado sin permiso o con plugin inactivo;
8. responsive a 375, 720 y 1280 px;
9. slot público visible para una personalización futura, sin implementar una
   diferencia empresarial ficticia.

La demo oficial se ejecutará en `J11-S6-07`; una vista temprana podrá observarse al
terminar `J11-S6-05`, pero no se presentará como cierre.

## Criterios de aceptación

- **CA-01:** alcance, exclusiones y autoridad están documentados.
- **CA-02:** el legado está declarado como fuente de solo lectura.
- **CA-03:** la secuencia impide diseñar persistencia antes de caracterizar.
- **CA-04:** los límites de kernel y plugins futuros son explícitos.
- **CA-05:** Sprint 6 vuelve al flujo incremental normal de pruebas.
- **CA-06:** la demo visual final y el PDF de cierre están incluidos.
- **CA-07:** G7 pendiente no se confunde con un gate técnico verde.
- **CA-08:** el único siguiente trabajo autorizado es `J11-S6-01`.

## Resultado

`J11-S6-00` queda completada documentalmente. No crea código ni afirma que el
dominio esté diseñado. Continúa `J11-S6-01` mediante inspección reproducible y de
solo lectura del legado.

