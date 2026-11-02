package com.bootcamp.customer_service.entity;

import com.bootcamp.customer_service.entity.enums.CustomerType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("INDIVIDUAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndividualCustomer extends Customer {

    private String firstName;

    private String lastName;

    // TCKN. Legal identity fields are not updatable: changing them would invalidate the KYC verification.
    private String nationalId;

    public IndividualCustomer(String email, String phoneNumber, String firstName, String lastName, String nationalId) {
        super(email, phoneNumber);
        this.firstName = firstName;
        this.lastName = lastName;
        this.nationalId = nationalId;
    }

    @Override
    public CustomerType getType() {
        return CustomerType.INDIVIDUAL;
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }
}
