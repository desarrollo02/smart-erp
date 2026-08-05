package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class BusinessPartnerJpaMappingTest {

    @Test
    void mapsExactlyNinePrivateEntitiesToThePluginSchema() {
        List<Class<?>> entities = List.of(
                BusinessPartnerEntity.class,
                BusinessPartnerRoleEntity.class,
                BusinessPartnerIdentificationEntity.class,
                BusinessPartnerAddressEntity.class,
                BusinessPartnerChannelEntity.class,
                BusinessPartnerContactEntity.class,
                BusinessPartnerContactChannelEntity.class,
                BusinessPartnerDefinitionEntity.class,
                BusinessPartnerDefinitionRevisionEntity.class);

        assertEquals(9, entities.size());
        entities.forEach(entity -> {
            assertTrue(entity.isAnnotationPresent(Entity.class));
            assertEquals(
                    BusinessPartnersPersistenceNames.SCHEMA,
                    entity.getAnnotation(Table.class).schema());
        });
    }

    @Test
    void keepsOptimisticVersionsOnIndependentAggregateRoots() throws NoSuchFieldException {
        Field version = BusinessPartnerEntity.class.getDeclaredField("version");
        assertTrue(version.isAnnotationPresent(Version.class));
        Field definitionVersion = BusinessPartnerDefinitionEntity.class.getDeclaredField("version");
        assertTrue(definitionVersion.isAnnotationPresent(Version.class));
    }

    @Test
    void repositoryContractDoesNotExposePhysicalDeletion() {
        assertTrue(java.util.Arrays.stream(
                        py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerRepository
                                .class.getMethods())
                .map(method -> method.getName().toLowerCase(Locale.ROOT))
                .noneMatch(name -> name.contains("delete") || name.contains("remove")));
    }
}
