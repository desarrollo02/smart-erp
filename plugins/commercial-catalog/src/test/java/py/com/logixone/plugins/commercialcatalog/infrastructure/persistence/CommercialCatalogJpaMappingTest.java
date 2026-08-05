package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class CommercialCatalogJpaMappingTest {

    @Test
    void mapsInitialAggregateRootsOnlyInsideThePrivatePluginSchema() {
        List<Class<?>> entities = List.of(
                CatalogItemEntity.class,
                CatalogItemScopeEntity.class,
                CatalogItemIdentifierEntity.class,
                CatalogItemUnitConversionEntity.class,
                CatalogItemUnitPurposeEntity.class,
                CatalogItemCategoryEntity.class,
                CatalogItemTagEntity.class,
                CatalogItemVariantEntity.class,
                CatalogItemVariantAttributeEntity.class,
                PriceListEntity.class,
                PriceEntryEntity.class);
        assertEquals(11, entities.size());
        entities.forEach(entity -> {
            assertTrue(entity.isAnnotationPresent(Entity.class));
            assertEquals(CommercialCatalogPersistenceNames.SCHEMA,
                    entity.getAnnotation(Table.class).schema());
        });
    }

    @Test
    void keepsOptimisticVersionOnBothAggregateRoots() throws NoSuchFieldException {
        Field itemVersion = CatalogItemEntity.class.getDeclaredField("version");
        Field listVersion = PriceListEntity.class.getDeclaredField("version");
        assertTrue(itemVersion.isAnnotationPresent(Version.class));
        assertTrue(listVersion.isAnnotationPresent(Version.class));
    }

    @Test
    void repositoryContractsDoNotExposePhysicalDeletion() {
        List<Class<?>> repositories = List.of(
                py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository.class,
                py.com.logixone.plugins.commercialcatalog.application.port.PriceListRepository.class,
                py.com.logixone.plugins.commercialcatalog.application.port.CatalogCodeSequenceRepository.class);
        repositories.forEach(repository -> assertTrue(java.util.Arrays.stream(repository.getMethods())
                .map(method -> method.getName().toLowerCase(Locale.ROOT))
                .noneMatch(name -> name.contains("delete") || name.contains("remove"))));
    }
}
