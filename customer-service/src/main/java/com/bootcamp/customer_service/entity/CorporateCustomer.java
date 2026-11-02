package com.bootcamp.customer_service.entity;

import com.bootcamp.customer_service.entity.enums.CustomerType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CORPORATE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorporateCustomer extends Customer {

    private String companyName;

    // VKN. Not updatable for the same reason as IndividualCustomer.nationalId.
    private String taxNumber;

    private String taxOffice;

    public CorporateCustomer(String email, String phoneNumber, String companyName, String taxNumber, String taxOffice) {
        super(email, phoneNumber);
        this.companyName = companyName;
        this.taxNumber = taxNumber;
        this.taxOffice = taxOffice;
    }

    @Override
    public CustomerType getType() {
        return CustomerType.CORPORATE;
    }

    @Override
    public String getDisplayName() {
        return companyName;
    }
}
