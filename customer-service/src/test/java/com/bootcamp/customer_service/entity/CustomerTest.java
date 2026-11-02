package com.bootcamp.customer_service.entity;

import com.bootcamp.common.exception.InvalidStatusTransitionException;
import com.bootcamp.common.exception.RecordNotFoundException;
import com.bootcamp.customer_service.entity.enums.CustomerType;
import com.bootcamp.customer_service.entity.enums.DocumentType;
import com.bootcamp.customer_service.entity.enums.KycStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The rules of the Customer aggregate, tested without Spring or a database.
class CustomerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 2, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void newCustomer_isActiveAndPending_withLowerCaseEmail() {
        Customer customer = individual("  Ahmet.Yilmaz@Mail.COM ");

        assertThat(customer.getEmail()).isEqualTo("ahmet.yilmaz@mail.com");
        assertThat(customer.getKycStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(customer.getType()).isEqualTo(CustomerType.INDIVIDUAL);
        assertThat(customer.getDisplayName()).isEqualTo("Ahmet Yilmaz");
    }

    @Test
    void corporateCustomer_usesCompanyNameAsDisplayName() {
        Customer customer = new CorporateCustomer("info@acme.com", "+902121112233", "Acme AS", "1234567890", "Kadikoy");

        assertThat(customer.getType()).isEqualTo(CustomerType.CORPORATE);
        assertThat(customer.getDisplayName()).isEqualTo("Acme AS");
    }

    // --- KYC ---

    @Test
    void approveKyc_setsVerifiedAndChangeTime() {
        Customer customer = individual("a@mail.com");

        customer.approveKyc(NOW);

        assertThat(customer.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(customer.getKycStatusChangedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectKyc_storesTrimmedReason() {
        Customer customer = individual("a@mail.com");

        customer.rejectKyc("  forged document ", NOW);

        assertThat(customer.getKycStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(customer.getKycRejectionReason()).isEqualTo("forged document");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectKyc_throws_andKeepsState_whenReasonIsBlank(String reason) {
        Customer customer = individual("a@mail.com");

        assertThatThrownBy(() -> customer.rejectKyc(reason, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(customer.getKycStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(customer.getKycStatusChangedAt()).isNull();
    }

    @Test
    void approveKyc_throws_whenCustomerIsRejected() {
        Customer customer = individual("a@mail.com");
        customer.rejectKyc("forged document", NOW);

        assertThatThrownBy(() -> customer.approveKyc(NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(customer.getKycRejectionReason()).isEqualTo("forged document");
    }

    // --- Addresses and documents ---

    @Test
    void addAddress_linksBothSidesOfTheRelation() {
        Customer customer = individual("a@mail.com");
        Address address = address(UUID.randomUUID());

        customer.addAddress(address);

        assertThat(customer.getAddresses()).containsExactly(address);
        // The owning side (address.customer) has no getter; ReflectionTestUtils is fine for this one check.
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(address, "customer")).isSameAs(customer);
    }

    @Test
    void getAddresses_isReadOnly_soRulesCannotBeBypassed() {
        Customer customer = individual("a@mail.com");

        assertThatThrownBy(() -> customer.getAddresses().add(address(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void removeAddress_removesOnlyTheGivenAddress() {
        Customer customer = individual("a@mail.com");
        Address keep = address(UUID.randomUUID());
        Address remove = address(UUID.randomUUID());
        customer.addAddress(keep);
        customer.addAddress(remove);

        customer.removeAddress(remove.getId());

        assertThat(customer.getAddresses()).containsExactly(keep);
    }

    @Test
    void removeAddress_throwsRecordNotFound_whenAddressIsNotThisCustomers() {
        Customer customer = individual("a@mail.com");
        customer.addAddress(address(UUID.randomUUID()));

        assertThatThrownBy(() -> customer.removeAddress(UUID.randomUUID()))
                .isInstanceOf(RecordNotFoundException.class);
        assertThat(customer.getAddresses()).hasSize(1);
    }

    @Test
    void addDocument_addsToCustomersDocuments() {
        Customer customer = individual("a@mail.com");

        customer.addDocument(new Document(DocumentType.ID_CARD, "id.pdf", "kyc/1/id.pdf", "application/pdf", 1024));

        assertThat(customer.getDocuments()).hasSize(1);
    }

    private static Customer individual(String email) {
        return new IndividualCustomer(email, "+905551112233", "Ahmet", "Yilmaz", "12345678901");
    }

    private static Address address(UUID id) {
        Address address = new Address("Home", "Bagdat Cd. 1", "Kadikoy", "Istanbul", "34710", "TR");
        address.setId(id);
        return address;
    }
}
