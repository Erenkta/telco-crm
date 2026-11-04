package com.bootcamp.customer_service.entity;

import com.bootcamp.common.entity.BaseEntity;
import com.bootcamp.common.exception.RecordNotFoundException;
import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import com.bootcamp.customer_service.entity.enums.CustomerType;
import com.bootcamp.customer_service.entity.enums.KycStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// SINGLE_TABLE: IndividualCustomer and CorporateCustomer rows live in the same "customers" table.
// Hibernate writes the subclass name into customer_type and uses it to pick the class when reading.
// Rules about the customer's state live in this class (rich domain model); the service only calls these methods.
@Entity
@Table(name = "customers")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "customer_type", discriminatorType = DiscriminatorType.STRING, length = 20)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA only
public abstract class Customer extends BaseEntity {

    private String email;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus = KycStatus.PENDING;

    private String kycRejectionReason;

    private OffsetDateTime kycStatusChangedAt;

    @Version
    private Long version;

    // Inverse side: Address.customer owns the FK. cascade + orphanRemoval because an address has no life
    // without its customer; removing it from this list deletes the row.
    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    protected Customer(String email, String phoneNumber) {
        this.email = normalizeEmail(email);
        this.phoneNumber = phoneNumber;
    }

    public abstract CustomerType getType();

    public abstract String getDisplayName();

    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public List<Document> getDocuments() {
        return Collections.unmodifiableList(documents);
    }

    public void changeContact(String email, String phoneNumber) {
        this.email = normalizeEmail(email);
        this.phoneNumber = phoneNumber;
    }

    public void changeStatus(CustomerStatus target) {
        this.status = this.status.transitionTo(target);
    }

    public void approveKyc(OffsetDateTime now) {
        this.kycStatus = this.kycStatus.transitionTo(KycStatus.VERIFIED);
        this.kycRejectionReason = null;
        this.kycStatusChangedAt = now;
    }

    public void rejectKyc(String reason, OffsetDateTime now) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("KYC rejection reason is required");
        }
        this.kycStatus = this.kycStatus.transitionTo(KycStatus.REJECTED);
        this.kycRejectionReason = reason.trim();
        this.kycStatusChangedAt = now;
    }

    // Keeps both sides of the bidirectional relation in sync; JPA only looks at the owning side (address.customer).
    public Address addAddress(Address address) {
        address.assignTo(this);
        addresses.add(address);
        return address;
    }

    public void removeAddress(UUID addressId) {
        Address address = addresses.stream()
                .filter(a -> addressId.equals(a.getId())) // new, unflushed addresses have no id yet
                .findFirst()
                .orElseThrow(() -> new RecordNotFoundException(
                        String.format("Address %s not found for customer %s", addressId, getId())));
        addresses.remove(address);
        address.assignTo(null);
    }

    public Document addDocument(Document document) {
        document.assignTo(this);
        documents.add(document);
        return document;
    }

    // Stored lower-case (DB CHECK enforces it) so the unique constraint and the email filter are case-insensitive.
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
