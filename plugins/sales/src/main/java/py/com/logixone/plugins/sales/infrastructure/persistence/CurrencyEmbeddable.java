package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.sales.domain.CurrencySnapshot;

@Embeddable
public class CurrencyEmbeddable {
 @Column(name="currency_code",nullable=false,length=3) String code;
 @Column(name="currency_minor_unit",nullable=false) int minorUnit;
 @Column(name="currency_name_snapshot",nullable=false,length=160) String name;
 @Column(name="currency_release_id",nullable=false,length=64) String releaseId;
 public CurrencyEmbeddable(){}
 static CurrencyEmbeddable from(CurrencySnapshot s){var v=new CurrencyEmbeddable();v.code=s.code().value();v.minorUnit=s.minorUnit();v.name=s.displayName();v.releaseId=s.releaseId();return v;}
 CurrencySnapshot snapshot(){return new CurrencySnapshot(new CurrencyCode(code),minorUnit,name,releaseId);}
}
