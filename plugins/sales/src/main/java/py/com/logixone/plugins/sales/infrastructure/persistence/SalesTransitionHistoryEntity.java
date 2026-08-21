package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*; import java.io.Serializable; import java.time.Instant; import java.util.*;

@Entity @Table(name="sales_transition_history",schema=SalesPersistenceNames.SCHEMA) @IdClass(SalesTransitionHistoryEntity.Key.class)
public class SalesTransitionHistoryEntity {
 @Id @Column(name="company_id") UUID companyId; @Id @Column(name="transition_id") UUID transitionId;
 @Column(name="aggregate_type",nullable=false,length=32) String aggregateType; @Column(name="aggregate_id",nullable=false) UUID aggregateId;
 @Column(name="from_state",nullable=false,length=24) String fromState; @Column(name="to_state",nullable=false,length=24) String toState;
 @Column(name="actor_id",nullable=false) UUID actorId; @Column(name="transition_reason",length=240) String reason;
 @Column(name="occurred_at",nullable=false) Instant occurredAt; @Column(name="idempotency_key",nullable=false,length=128) String idempotencyKey;
 public SalesTransitionHistoryEntity(){}
 static SalesTransitionHistoryEntity from(py.com.logixone.plugins.sales.application.port.SalesTransitionRecord value){var e=new SalesTransitionHistoryEntity();e.companyId=value.companyId().value();e.transitionId=value.transitionId();e.aggregateType=value.aggregateType();e.aggregateId=value.aggregateId();e.fromState=value.fromState();e.toState=value.toState();e.actorId=value.actorId().value();e.reason=value.reason().orElse(null);e.occurredAt=value.occurredAt();e.idempotencyKey=value.idempotencyKey();return e;}
 public static final class Key implements Serializable {public UUID companyId;public UUID transitionId;public Key(){}@Override public boolean equals(Object o){return this==o||o instanceof Key k&&Objects.equals(companyId,k.companyId)&&Objects.equals(transitionId,k.transitionId);}@Override public int hashCode(){return Objects.hash(companyId,transitionId);}}
}
