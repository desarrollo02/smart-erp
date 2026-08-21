package py.com.logixone.plugins.sales.api;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesApiContractTest {
    @Test void identitiesAreOpaqueAndVersionIsStable(){var id=new SalesQuoteId(UUID.randomUUID()); assertEquals(id,SalesQuoteId.parse(id.toString())); assertEquals("1.0.0",SalesContractVersion.CURRENT); assertThrows(IllegalArgumentException.class,()->SalesOrderId.parse("bad"));}
}
