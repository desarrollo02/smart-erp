package py.com.logixone.plugins.sales.application.port;
import java.util.*; import py.com.logixone.kernel.api.company.CompanyId; import py.com.logixone.plugins.sales.domain.SalesTerm;
public interface SalesTermRepository { Optional<SalesTerm> findById(CompanyId companyId,UUID id); Optional<SalesTerm> findByCode(CompanyId companyId,String code); SalesTerm insert(SalesTerm term); SalesTerm update(SalesTerm term,long expectedPersistedVersion); }
