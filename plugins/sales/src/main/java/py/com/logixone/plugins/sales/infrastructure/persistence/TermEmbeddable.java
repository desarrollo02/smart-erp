package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import py.com.logixone.plugins.sales.domain.PaymentTermSnapshot;

@Embeddable
public class TermEmbeddable {
 @Column(name="term_id_snapshot",nullable=false) UUID id;
 @Column(name="term_code_snapshot",nullable=false,length=32) String code;
 @Column(name="term_name_snapshot",nullable=false,length=120) String name;
 @Column(name="term_due_days_snapshot",nullable=false) int dueDays;
 @Column(name="term_source_version",nullable=false) long sourceVersion;
 public TermEmbeddable(){}
 static TermEmbeddable from(PaymentTermSnapshot s){var v=new TermEmbeddable();v.id=s.id();v.code=s.code();v.name=s.displayName();v.dueDays=s.dueDays();v.sourceVersion=s.sourceVersion();return v;}
 PaymentTermSnapshot snapshot(){return new PaymentTermSnapshot(id,code,name,dueDays,sourceVersion);}
}
