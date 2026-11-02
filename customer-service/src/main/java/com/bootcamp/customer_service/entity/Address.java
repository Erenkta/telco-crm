package com.bootcamp.customer_service.entity;

import com.bootcamp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    // Owning side of Customer <-> Address: this field is what Hibernate writes into addresses.customer_id.
    // LAZY because @ManyToOne defaults to EAGER, which would load the customer for every address.
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String title;

    private String line;

    private String district;

    private String city;

    private String postalCode;

    private String country;

    public Address(String title, String line, String district, String city, String postalCode, String country) {
        this.title = title;
        this.line = line;
        this.district = district;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    // Only Customer.addAddress/removeAddress call this, so both sides of the relation stay in sync.
    void assignTo(Customer customer) {
        this.customer = customer;
    }
}
