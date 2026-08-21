package py.com.logixone.plugins.sales.infrastructure.persistence;
import jakarta.persistence.*; import py.com.logixone.plugins.sales.application.port.*;
final class SalesConflictMapper {private SalesConflictMapper(){} static SalesPersistenceException map(RuntimeException failure){if(failure instanceof SalesPersistenceException known)return known;if(failure instanceof OptimisticLockException)return new SalesPersistenceException(SalesPersistenceCode.VERSION_CONFLICT,failure);return new SalesPersistenceException(SalesPersistenceCode.CONSTRAINT_VIOLATION,failure);}}
