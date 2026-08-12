# Base de conocimiento

Aquí se documenta el comportamiento relevante observado en las fuentes legadas
de solo lectura y el análisis controlado de manuales técnicos o regulatorios que
orientan contratos y persistencia.

Fuentes legadas autorizadas:

- `C:\cosme\multienvios\miaterra`;
- `C:\cosme\mega\miaterra` — copia actualizada de Miaterra, con raíz de código
  en `C:\cosme\mega\miaterra\fuente\tag`; consultar preferentemente para cambios
  recientes y registrar en cada análisis la ruta y revisión observadas;
- `C:\cosme\felsina\ingeniolafelsina`.

Ninguna fuente autorizada se modifica desde Logixone. Su código no constituye un
contrato ni se copia mecánicamente: primero se caracteriza el comportamiento y se
lo convierte en requisitos, decisiones y pruebas.

Cada análisis debe indicar la fuente revisada, el comportamiento observado, reglas inferidas, dudas, riesgos y pruebas de caracterización propuestas. No almacenar secretos ni copiar grandes bloques de código legado.

## Índice

- [SIFEN v150 como referencia estructural para documentos comerciales](sifen-v150-estructura-documentos.md)
- [Facturación masiva: caracterización del legado y frontera propuesta](commercial-documents/facturacion-masiva-legacy-characterization.md)
- [Facturación recurrente: planes, prorrateo y consumo medido](commercial-documents/recurring-billing-domain-analysis.md)
- [`business_partners`: caracterización de personas, clientes y proveedores](business-partners/legacy-characterization.md)
- [`commercial_catalog`: caracterización de ítems, unidades, clasificaciones, impuestos y precios](commercial-catalog/legacy-characterization.md)
- [`inventory`: caracterización de depósitos, ubicaciones, existencias, movimientos, reservas y conteos](inventory/legacy-characterization.md)
- [`purchasing`: caracterización de solicitudes, órdenes, recepciones y devoluciones](purchasing/legacy-characterization.md)
- [Perfil de origen para migrar Oracle Forms & Reports](legacy-migration/oracle-forms-reports-source-profile.md)
- [Recursos humanos y nómina: caracterización y factibilidad desde Ingenio La Felsina](human-resources/legacy-characterization.md)
- [Estaciones de servicio: consumo legado, regulación y frontera del plugin](fuel-station/legacy-characterization.md)
- [Telemetría vehicular: GPS, recorridos, sensores y frontera del plugin](vehicle-telemetry/legacy-characterization.md)
- [Taller y mantenimiento vehicular: solicitudes, órdenes, planes, repuestos y frontera F1/F2](vehicle-maintenance/legacy-characterization.md)
- [Cooperativas de ahorro y crédito: alcance regulatorio inicial](cooperative-savings-credit/regulatory-scope-analysis.md)
