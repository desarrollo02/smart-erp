package py.com.logixone.plugins.inventory.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class InventoryJpaMappingTest {

    @Test
    void mapsAllTenTablesOnlyInsideThePrivateInventorySchema() {
        List<Class<?>> entities = List.of(
                WarehouseEntity.class,
                StockLocationEntity.class,
                InventoryItemEntity.class,
                InventoryBalanceEntity.class,
                StockMovementEntity.class,
                StockMovementLineEntity.class,
                StockReservationEntity.class,
                ReservationOperationEntity.class,
                StockCountEntity.class,
                StockCountLineEntity.class);
        assertEquals(10, entities.size());
        entities.forEach(entity -> {
            assertTrue(entity.isAnnotationPresent(Entity.class));
            assertEquals(InventoryPersistenceNames.SCHEMA, entity.getAnnotation(Table.class).schema());
        });
    }

    @Test
    void appliesOptimisticVersioningOnlyToMutableRows() throws NoSuchFieldException {
        List<Class<?>> mutable = List.of(
                WarehouseEntity.class, StockLocationEntity.class, InventoryItemEntity.class,
                InventoryBalanceEntity.class, StockReservationEntity.class, StockCountEntity.class);
        for (Class<?> entity : mutable) {
            Field version = entity.getDeclaredField("version");
            assertTrue(version.isAnnotationPresent(Version.class), entity.getSimpleName());
        }
    }

    @Test
    void repositoryContractsRequireCompanyScopeAndExposeNoPhysicalDeletion() {
        List<Class<?>> repositories = List.of(
                py.com.logixone.plugins.inventory.application.port.WarehouseRepository.class,
                py.com.logixone.plugins.inventory.application.port.InventoryItemRepository.class,
                py.com.logixone.plugins.inventory.application.port.InventoryBalanceRepository.class,
                py.com.logixone.plugins.inventory.application.port.StockMovementRepository.class,
                py.com.logixone.plugins.inventory.application.port.StockReservationRepository.class,
                py.com.logixone.plugins.inventory.application.port.ReservationOperationRepository.class,
                py.com.logixone.plugins.inventory.application.port.StockCountRepository.class);
        repositories.forEach(repository -> assertTrue(java.util.Arrays.stream(repository.getMethods())
                .map(method -> method.getName().toLowerCase(Locale.ROOT))
                .noneMatch(name -> name.contains("delete") || name.contains("remove"))));
        repositories.forEach(repository -> java.util.Arrays.stream(repository.getMethods())
                .filter(method -> method.getName().startsWith("find"))
                .forEach(method -> assertEquals(
                        py.com.logixone.kernel.api.company.CompanyId.class,
                        method.getParameterTypes()[0], method.toString())));
    }
}
