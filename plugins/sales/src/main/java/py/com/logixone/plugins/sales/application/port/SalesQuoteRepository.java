package py.com.logixone.plugins.sales.application.port;
import java.util.Optional; import py.com.logixone.kernel.api.company.CompanyId; import py.com.logixone.plugins.sales.api.SalesQuoteId; import py.com.logixone.plugins.sales.domain.SalesQuote;
public interface SalesQuoteRepository { Optional<SalesQuote> findById(CompanyId companyId,SalesQuoteId id); Optional<SalesQuote> findByNumber(CompanyId companyId,String number); SalesQuote insert(SalesQuote quote); SalesQuote update(SalesQuote quote,long expectedPersistedVersion); }
