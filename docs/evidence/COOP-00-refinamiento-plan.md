# COOP-00 — Refinamiento del plan cooperativo

- Fecha: 2026-08-04
- Estado: historia refinada; ejecución y decisiones de producto pendientes
- Historia: [COOP-00](../backlog/COOP-00-gobierno-alcance-matriz-normativa.md)
- ADR: [ADR-0037](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md)

## Trabajo realizado

1. Se convirtió COOP-00 en una historia verificable con entradas, entregables,
   secuencia, gates G0–G5 y quince criterios de aceptación.
2. Se definieron COOP-D01 a COOP-D15 para tipo/nivel, multiempresa, socios,
   aportes, gobierno, productos, tasas, crédito, garantías, tesorería, LA/FT,
   regulación, migración e integraciones.
3. Se creó un registro inicial COOP-N01 a COOP-N15 de fuentes oficiales y se
   separó `REFERENCIA_CONSULTADA` de `FUENTE_CONGELADA`.
4. Se documentó la matriz requisito–fuente–plugin–dato–regla–permiso–prueba y los
   responsables que COOP-00 deberá completar.
5. Se trazó el grafo de seis plugins, tres fundaciones genéricas, eventos
   contables y conector de soporte opcional.
6. Se detallaron los flujos candidatos de depósito, desembolso y cobranza sin
   usar tablas cruzadas ni `accounts_receivable` para la cartera.
7. Se definieron perfiles físicos conceptuales y gates arquitectónicos; no se
   agregaron al POM.

## Fuentes adicionales verificadas

La continuación contrastó en el portal oficial del INCOOP:

- [Resolución N.º 22.957/2020](https://www.incoop.gov.py/?p=9815), que publica la
  matriz basada en riesgo LA/FT para cooperativas que capten ahorros o concedan
  créditos;
- [Central de Riesgos](https://www.incoop.gov.py/?page_id=5072) y
  [Resolución N.º 14.877/2016](https://www.incoop.gov.py/?p=5695), que identifican
  servicio, manual y formato de operaciones crediticias;
- [Comunicado N.º 36/2024](https://www.incoop.gov.py/?p=13400), que publicó una
  tabla de obligaciones con plazo actualizada a julio de 2024;
- [Comunicado N.º 12/2026](https://www.incoop.gov.py/?p=14008), que informa como
  operativos Central de Riesgos, SICOOP y Alerta Temprana y menciona coordinación
  VPN para esta última.

Estos hallazgos agregan requisitos de formatos, calendarios y canales, pero no
autorizan hardcodearlos ni automatizar VPN, credenciales o envíos. COOP-00 deberá
confirmar la edición y topología aplicables a la cooperativa real.

## Estado de decisiones

COOP-D01 a COOP-D15 permanecen `Pendiente`. No se recibió todavía estatuto,
reglamentos, tipo/nivel, productos, plan de cuentas, matriz LA/FT, sistemas,
fuentes de migración ni autorización de acceso de una cooperativa concreta.

La historia refinada permite pedir esas entradas sin improvisar el modelo, pero
no habilita COOP-01 ni ningún código.

## Revisión documental

Se actualizaron índices de backlog, arquitectura, conocimiento y evidencia; la
épica, ADR, manual técnico y guía de implementación enlazan el refinamiento. El
manual de usuario y los runbooks operativos no cambian porque sigue sin existir
capacidad ejecutable.

## Pruebas no aplicables

No se ejecutaron Maven, JUnit, ArchUnit, PostgreSQL, Docker, Compose ni Playwright.
No cambiaron código, POM, migraciones, descriptor, UI o composición. El gate
aplicable es G0 documental.

## Resultado G0

Se ejecutó `tmp/validate_docs.py` con el runtime Python bundled de Codex después
de completar todos los enlaces e índices. Resultado sobre **287 archivos
Markdown**:

- enlaces locales rotos: 0;
- errores UTF-8: 0;
- archivos con mojibake: 0;
- coincidencias con secretos locales: 0.
