# Business Partners API

Contrato empresarial público y Java puro de `business_partners`.

- Versión del contrato: `1.0.0`.
- Dependencia permitida: `kernel-api` sólo para `CompanyId`.
- Prohibido: Jakarta, JPA, JDBC, Hibernate, JSF, entidades y adaptadores internos.

## Superficie pública

- `BusinessPartnerId`: UUID opaco y canónico;
- `BusinessPartnerKind`: persona natural u organización;
- `BusinessPartnerState`: activo o inactivo;
- `BusinessPartnerRole`: cliente o proveedor;
- `BusinessPartnerReference`: proyección mínima e inmutable;
- `BusinessPartnerDirectory`: consulta síncrona por empresa e ID;
- `BusinessPartnerContractVersion`: versión semántica vigente.

La proyección deliberadamente no contiene direcciones, correos, identificaciones
completas, datos financieros o tipos internos. Una ampliación requiere necesidad
real, compatibilidad explícita y versión del contrato.

## Prueba

```powershell
.\mvnw.cmd -B -pl plugins/business-partners-api -am test
```
