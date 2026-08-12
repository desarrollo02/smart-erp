package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class PurchasingJpaMappingTest {
    @Test
    void mapsElevenTablesOnlyInsideThePrivatePurchasingSchema() {
        List<Class<?>> entities = List.of(
                PurchaseRequestEntity.class, PurchaseRequestLineEntity.class,
                PurchaseOrderEntity.class, PurchaseOrderLineEntity.class,
                PurchaseOrderAllocationEntity.class, GoodsReceiptEntity.class,
                GoodsReceiptLineEntity.class, SupplierReturnEntity.class,
                SupplierReturnLineEntity.class, PurchasingOperationEntity.class,
                PurchasingImportEntity.class);
        assertEquals(11, entities.size());
        entities.forEach(entity -> {
            assertTrue(entity.isAnnotationPresent(Entity.class));
            assertEquals(PurchasingPersistenceNames.SCHEMA,
                    entity.getAnnotation(Table.class).schema());
        });
    }

    @Test
    void appliesOptimisticVersioningToTheFourAggregateRoots() throws NoSuchFieldException {
        for (Class<?> entity : List.of(
                PurchaseRequestEntity.class, PurchaseOrderEntity.class,
                GoodsReceiptEntity.class, SupplierReturnEntity.class)) {
            Field version = entity.getDeclaredField("version");
            assertTrue(version.isAnnotationPresent(Version.class), entity.getSimpleName());
        }
    }

    @Test
    void repositoryContractsRequireCompanyScopeAndExposeNoPhysicalDeletion() {
        List<Class<?>> repositories = List.of(
                py.com.logixone.plugins.purchasing.application.port.PurchaseRequestRepository.class,
                py.com.logixone.plugins.purchasing.application.port.PurchaseOrderRepository.class,
                py.com.logixone.plugins.purchasing.application.port.GoodsReceiptRepository.class,
                py.com.logixone.plugins.purchasing.application.port.SupplierReturnRepository.class);
        repositories.forEach(repository -> assertTrue(Arrays.stream(repository.getMethods())
                .map(method -> method.getName().toLowerCase(Locale.ROOT))
                .noneMatch(name -> name.contains("delete") || name.contains("remove"))));
        repositories.forEach(repository -> Arrays.stream(repository.getMethods())
                .filter(method -> method.getName().startsWith("find"))
                .forEach(method -> assertEquals(
                        py.com.logixone.kernel.api.company.CompanyId.class,
                        method.getParameterTypes()[0], method.toString())));
    }
}
