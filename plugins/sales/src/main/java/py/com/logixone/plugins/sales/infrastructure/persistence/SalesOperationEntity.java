package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*; import java.io.Serializable; import java.time.Instant; import java.util.*;

@Entity @Table(name="sales_operation",schema=SalesPersistenceNames.SCHEMA) @IdClass(SalesOperationEntity.Key.class)
public class SalesOperationEntity {
 @Id @Column(name="company_id") UUID companyId; @Id @Column(name="idempotency_key",length=128) String idempotencyKey;
 @Column(name="operation_type",nullable=false,length=64) String operationType; @Column(name="request_fingerprint",nullable=false,length=64) String fingerprint;
 @Column(name="aggregate_type",nullable=false,length=32) String aggregateType; @Column(name="aggregate_id",nullable=false) UUID aggregateId; @Column(name="created_at",nullable=false) Instant createdAt;
 public SalesOperationEntity(){}
 static SalesOperationEntity from(py.com.logixone.plugins.sales.application.port.SalesOperationRecord value){var e=new SalesOperationEntity();e.companyId=value.companyId().value();e.idempotencyKey=value.idempotencyKey();e.operationType=value.operationType();e.fingerprint=value.fingerprint();e.aggregateType=value.aggregateType();e.aggregateId=value.aggregateId();e.createdAt=value.createdAt();return e;}
 py.com.logixone.plugins.sales.application.port.SalesOperationRecord record(){return new py.com.logixone.plugins.sales.application.port.SalesOperationRecord(new py.com.logixone.kernel.api.company.CompanyId(companyId),idempotencyKey,operationType,fingerprint,aggregateType,aggregateId,createdAt);}
 public static final class Key implements Serializable {public UUID companyId;public String idempotencyKey;public Key(){}public Key(UUID companyId,String idempotencyKey){this.companyId=companyId;this.idempotencyKey=idempotencyKey;}@Override public boolean equals(Object o){return this==o||o instanceof Key k&&Objects.equals(companyId,k.companyId)&&Objects.equals(idempotencyKey,k.idempotencyKey);}@Override public int hashCode(){return Objects.hash(companyId,idempotencyKey);}}
}
