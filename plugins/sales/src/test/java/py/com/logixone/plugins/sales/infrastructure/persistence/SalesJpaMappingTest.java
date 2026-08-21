package py.com.logixone.plugins.sales.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.persistence.*; import java.lang.reflect.Field; import java.util.*; import org.junit.jupiter.api.Test;

class SalesJpaMappingTest {
 @Test void mapsEightTablesOnlyInsidePrivateSchema(){var entities=List.<Class<?>>of(SalesTermEntity.class,SalesQuoteEntity.class,SalesQuoteLineEntity.class,SalesOrderEntity.class,SalesOrderLineEntity.class,SalesOrderReservationEntity.class,SalesOperationEntity.class,SalesTransitionHistoryEntity.class);assertEquals(8,entities.size());entities.forEach(type->{assertTrue(type.isAnnotationPresent(Entity.class));assertEquals(SalesPersistenceNames.SCHEMA,type.getAnnotation(Table.class).schema());});}
 @Test void rootsUseOptimisticVersioning() throws Exception {for(var type:List.of(SalesTermEntity.class,SalesQuoteEntity.class,SalesOrderEntity.class)){Field field=type.getDeclaredField("version");assertTrue(field.isAnnotationPresent(Version.class),type.getSimpleName());}}
 @Test void repositoriesAreCompanyScopedAndNeverDelete(){var repositories=List.<Class<?>>of(py.com.logixone.plugins.sales.application.port.SalesTermRepository.class,py.com.logixone.plugins.sales.application.port.SalesQuoteRepository.class,py.com.logixone.plugins.sales.application.port.SalesOrderRepository.class);repositories.forEach(type->Arrays.stream(type.getMethods()).forEach(method->{assertFalse(method.getName().toLowerCase(Locale.ROOT).contains("delete"));if(method.getName().startsWith("find"))assertEquals(py.com.logixone.kernel.api.company.CompanyId.class,method.getParameterTypes()[0]);}));}
}
