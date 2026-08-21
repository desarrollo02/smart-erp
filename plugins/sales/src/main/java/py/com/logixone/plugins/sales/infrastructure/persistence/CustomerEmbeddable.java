package py.com.logixone.plugins.sales.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.sales.domain.CustomerSnapshot;

@Embeddable
public class CustomerEmbeddable {
 @Column(name="customer_id",nullable=false) UUID id;
 @Column(name="customer_code_snapshot",nullable=false,length=64) String code;
 @Column(name="customer_name_snapshot",nullable=false,length=200) String name;
 @Column(name="customer_tax_id_snapshot",nullable=false,length=64) String taxId;
 @Column(name="customer_source_version",nullable=false) long sourceVersion;
 public CustomerEmbeddable(){}
 static CustomerEmbeddable from(CustomerSnapshot s){var v=new CustomerEmbeddable();v.id=s.id().value();v.code=s.code();v.name=s.displayName();v.taxId=s.taxId();v.sourceVersion=s.sourceVersion();return v;}
 CustomerSnapshot snapshot(){return new CustomerSnapshot(new BusinessPartnerId(id),code,name,taxId,sourceVersion);}
}
