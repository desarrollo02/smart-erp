package py.com.logixone.plugins.sales.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.plugins.sales.api.*;
import py.com.logixone.plugins.sales.application.port.SalesIdGenerator;

@ApplicationScoped
public class UuidSalesIdGenerator implements SalesIdGenerator {
    public SalesQuoteId nextQuoteId(){return new SalesQuoteId(UUID.randomUUID());}
    public SalesOrderId nextOrderId(){return new SalesOrderId(UUID.randomUUID());}
    public UUID nextTermId(){return UUID.randomUUID();}
    public UUID nextTransitionId(){return UUID.randomUUID();}
}
