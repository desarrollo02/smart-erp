# ADR-0029 - Confirmación del instalador en cada cierre de Sprint

- Estado: Aceptado
- Fecha: 2026-08-01
- Decisión de producto: al finalizar cada Sprint se preguntará si se creará un
  instalador nuevo
- Modifica: obligatoriedad de regeneración establecida por ADR-0026 y por la
  metodología anterior; conserva sus requisitos técnicos cuando la respuesta sea
  afirmativa

## Contexto

Desde Sprint 8 la metodología exigía regenerar siempre el instalador Windows como
último gate. Producto decidió que su necesidad debe evaluarse en cada cierre: un
Sprint puede justificar un nuevo entregable instalable, mientras otro puede cerrar
como incremento técnico o interno sin sustituir `current`.

Omitir el instalador sin registrar la decisión permitiría confundir un artefacto
anterior con el baseline nuevo. Generarlo antes de terminar pruebas, demo, PDF y
documentación obligaría a repetirlo ante cualquier corrección.

## Decisión

### 1. Pregunta obligatoria

Después de completar los gates funcionales y documentales del candidato, y antes
de declarar cerrado el Sprint, se formulará explícitamente al responsable de
producto:

> ¿Crearemos un nuevo instalador Windows para este Sprint?

La respuesta `SÍ` o `NO`, fecha, responsable y razón se registrarán en la evidencia
de cierre. Sin respuesta, el cierre queda pendiente de decisión.

### 2. Si la respuesta es SÍ

El instalador se vuelve el último gate técnico y se aplican íntegramente ADR-0026,
la épica y el runbook:

- congelar el baseline exacto;
- construir en temporal y sustituir sólo los derivados declarados de `current`;
- verificar preflight, consentimiento/UAC, instalación, reparación, actualización,
  cancelación, health y persistencia;
- registrar versión, digest, tamaño, SHA-256, firma, licencias y ambientes;
- no cerrar el Sprint hasta que la matriz acordada quede verde.

### 3. Si la respuesta es NO

- no se borra, reemplaza ni retoca `installer/windows/current`;
- el último instalador se marca documentalmente como perteneciente a su baseline y
  **no representativo del Sprint nuevo**;
- no puede entregarse o anunciarse como instalador de la versión recién cerrada;
- la evidencia explica cómo levantar manualmente el baseline y por qué no se creó
  un instalador;
- el Sprint puede cerrar si todos los demás gates están verdes y la decisión quedó
  registrada.

Una respuesta negativa no elimina el código fuente, manifiestos, pruebas ni deuda
de firma/matriz del instalador.

### 4. Momento de la pregunta

La pregunta no se adelanta durante el desarrollo porque una corrección posterior
puede invalidar el artefacto. Se realiza cuando el incremento está listo para su
cierre, con baseline candidato, demo, manuales, fotografía y PDF verificados.

Para Sprint 8 esta decisión no supone respuesta anticipada. Al llegar nuevamente a
su gate de cierre se preguntará y se actuará según la contestación.

## Consecuencias

- producto controla el coste de producir y validar instaladores;
- ningún instalador viejo se confunde con el baseline actual;
- un `NO` permite cerrar sin ejecutar una matriz Windows innecesaria;
- un `SÍ` conserva todos los controles técnicos y de seguridad existentes;
- el cierre incorpora una decisión humana adicional que debe quedar auditada.

## Referencias

- [ADR-0026 - Bootstrapper Windows nativo](0026-instalador-windows-bootstrapper-nativo.md)
- [Metodología del instalador por Sprint](../runbooks/metodologia-instalador-windows-cierre-sprint.md)
- [Épica del instalador Windows](../backlog/epica-instalador-windows-reproducible.md)

