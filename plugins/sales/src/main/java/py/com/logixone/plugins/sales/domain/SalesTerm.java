package py.com.logixone.plugins.sales.domain;

import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;

/** Company-owned commercial term; historical documents retain a snapshot. */
public final class SalesTerm {
    private final CompanyId companyId;
    private final UUID id;
    private final String code;
    private String displayName;
    private int dueDays;
    private boolean active = true;
    private long version;

    private SalesTerm(CompanyId companyId, UUID id, String code, String displayName, int dueDays) {
        this.companyId=Objects.requireNonNull(companyId); this.id=Objects.requireNonNull(id);
        this.code=SalesValues.text(code,"term code",32); reviseValues(displayName,dueDays);
    }
    public static SalesTerm active(CompanyId companyId, UUID id, String code, String displayName, int dueDays){return new SalesTerm(companyId,id,code,displayName,dueDays);}
    public static SalesTerm restore(Snapshot snapshot){var term=new SalesTerm(snapshot.companyId(),snapshot.id(),snapshot.code(),snapshot.displayName(),snapshot.dueDays()); if(snapshot.version()<0) throw new IllegalArgumentException("Invalid version"); term.active=snapshot.active(); term.version=snapshot.version(); return term;}
    public void revise(String name,int days,long expected){verify(expected); reviseValues(name,days); version++;}
    public void deactivate(long expected){verify(expected); if(!active) throw new IllegalStateException("Term is inactive"); active=false; version++;}
    private void reviseValues(String name,int days){displayName=SalesValues.text(name,"term name",120); if(days<0||days>3650) throw new IllegalArgumentException("Invalid dueDays"); dueDays=days;}
    private void verify(long expected){if(expected!=version) throw new ConcurrentSalesChangeException(expected,version);}
    public PaymentTermSnapshot documentSnapshot(){return new PaymentTermSnapshot(id,code,displayName,dueDays,version);}
    public Snapshot snapshot(){return new Snapshot(companyId,id,code,displayName,dueDays,active,version);}
    public record Snapshot(CompanyId companyId,UUID id,String code,String displayName,int dueDays,boolean active,long version){}
}
