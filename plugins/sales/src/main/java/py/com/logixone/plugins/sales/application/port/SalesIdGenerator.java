package py.com.logixone.plugins.sales.application.port;

import java.util.UUID;
import py.com.logixone.plugins.sales.api.SalesOrderId;
import py.com.logixone.plugins.sales.api.SalesQuoteId;

public interface SalesIdGenerator {
    SalesQuoteId nextQuoteId();
    SalesOrderId nextOrderId();
    UUID nextTermId();
    UUID nextTransitionId();
}
