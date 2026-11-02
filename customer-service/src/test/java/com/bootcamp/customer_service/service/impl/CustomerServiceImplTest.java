package com.bootcamp.customer_service.service.impl;

import com.bootcamp.common.exception.InvalidStatusTransitionException;
import com.bootcamp.common.exception.PreconditionFailedException;
import com.bootcamp.common.exception.RecordNotFoundException;
import com.bootcamp.customer_service.dto.request.AddAddressRequest;
import com.bootcamp.customer_service.dto.request.UpdateContactRequest;
import com.bootcamp.customer_service.dto.response.AddressResponse;
import com.bootcamp.customer_service.dto.response.CustomerDetailResponse;
import com.bootcamp.customer_service.entity.Address;
import com.bootcamp.customer_service.entity.Customer;
import com.bootcamp.customer_service.entity.IndividualCustomer;
import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import com.bootcamp.customer_service.entity.enums.KycStatus;
import com.bootcamp.customer_service.mapper.CustomerMapper;
import com.bootcamp.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pure unit test: no Spring context. The repository is a mock, the mapper is the real one
// (it has no dependencies, and mocking it would stop us from checking the actual response).
// The service is built by hand because not every dependency is a mock (real mapper, fixed clock).
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");
    private static final OffsetDateTime NOW_UTC = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

    @Mock
    private CustomerRepository customerRepository;

    private CustomerServiceImpl customerService;

    private UUID id;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, new CustomerMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        id = UUID.randomUUID();
        customer = customerWithVersion(id, 0L);
    }

    // --- KYC ---

    @Test
    void approveKyc_ReturnsApprovedCustomerWithProperKyc() {
        when(customerRepository.findWithAddressesById(id)).thenReturn(Optional.of(customer));

        CustomerDetailResponse response = customerService.approveKyc(id, 0L);

        assertThat(response.kycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(response.kycStatusChangedAt()).isEqualTo(NOW_UTC);
        assertThat(response.kycRejectionReason()).isNull();
        verify(customerRepository).flush(); // version increment + UPDATE happen on flush
    }

    @Test
    void approveKyc_throwsPreconditionFailed_andDoesNotTouchCustomer_whenIfMatchVersionIsStale() {
        when(customerRepository.findWithAddressesById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.approveKyc(id, 5L))
                .isInstanceOf(PreconditionFailedException.class)
                .hasMessageContaining("current version 0, If-Match 5");

        assertThat(customer.getKycStatus()).isEqualTo(KycStatus.PENDING);
        verify(customerRepository, never()).flush();
    }

    @Test
    void approveKyc_throwsRecordNotFound_whenCustomerDoesNotExist() {
        when(customerRepository.findWithAddressesById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.approveKyc(id, 0L))
                .isInstanceOf(RecordNotFoundException.class);
    }

    @Test
    void rejectKyc_rejectTheRequest_WhenRejectReasonIsEmpty() {
        when(customerRepository.findWithAddressesById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.rejectKyc(id, 0L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KYC rejection reason is required");

        verify(customerRepository, never()).flush();
    }

    @Test
    void rejectKyc_withdrawsApproval_whenCustomerIsVerified() {
        customer.approveKyc(NOW_UTC.minusDays(1));
        when(customerRepository.findWithAddressesById(id)).thenReturn(Optional.of(customer));

        CustomerDetailResponse response = customerService.rejectKyc(id, 0L, "  forged document  ");

        assertThat(response.kycStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(response.kycRejectionReason()).isEqualTo("forged document");
        assertThat(response.kycStatusChangedAt()).isEqualTo(NOW_UTC);
    }

    @Test
    void approveKyc_throwsInvalidStatusTransition_whenCustomerIsRejected() {
        customer.rejectKyc("forged document", NOW_UTC);
        when(customerRepository.findWithAddressesById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.approveKyc(id, 0L))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(customerRepository, never()).flush();
    }

    // --- Contact and status ---

    @Test
    void updateContact_storesEmailLowerCase() {
        when(customerRepository.findWithAddressesById(id)).thenReturn(Optional.of(customer));

        CustomerDetailResponse response = customerService.updateContact(id, 0L,
                new UpdateContactRequest("  New.Mail@Example.COM ", "+905550000000"));

        assertThat(response.email()).isEqualTo("new.mail@example.com");
        assertThat(response.phoneNumber()).isEqualTo("+905550000000");
    }

    @Test
    void changeStatus_suspendsActiveCustomer() {
        when(customerRepository.findWithAddressesById(id)).thenReturn(Optional.of(customer));

        CustomerDetailResponse response = customerService.changeStatus(id, 0L, CustomerStatus.SUSPENDED);

        assertThat(response.status()).isEqualTo(CustomerStatus.SUSPENDED);
    }

    // --- Addresses ---

    @Test
    void addAddress_loadsCustomerWithForceIncrementLock_andDefaultsCountryToTR() {
        when(customerRepository.findByIdForUpdate(id)).thenReturn(Optional.of(customer));

        AddressResponse response = customerService.addAddress(id,
                new AddAddressRequest("Home", "Bagdat Cd. 1", "Kadikoy", "Istanbul", "34710", null));

        assertThat(response.country()).isEqualTo("TR");
        assertThat(customer.getAddresses()).hasSize(1);
        verify(customerRepository).findByIdForUpdate(id); // not findById: child changes must bump the version
        verify(customerRepository).flush();
    }

    @Test
    void removeAddress_removesOwnAddress() {
        Address address = addressWithId(UUID.randomUUID());
        customer.addAddress(address);
        when(customerRepository.findByIdForUpdate(id)).thenReturn(Optional.of(customer));

        customerService.removeAddress(id, address.getId());

        assertThat(customer.getAddresses()).isEmpty(); // orphanRemoval turns this into a DELETE on flush
    }

    // Regression test for the IDOR bug: an address id that is not one of THIS customer's addresses
    // (another customer's address, or a random id) must be 404 and must not delete anything.
    @Test
    void removeAddress_throwsRecordNotFound_whenAddressBelongsToAnotherCustomer() {
        customer.addAddress(addressWithId(UUID.randomUUID()));
        UUID otherCustomersAddressId = UUID.randomUUID();
        when(customerRepository.findByIdForUpdate(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.removeAddress(id, otherCustomersAddressId))
                .isInstanceOf(RecordNotFoundException.class);
        assertThat(customer.getAddresses()).hasSize(1);
    }

    private static Customer customerWithVersion(UUID id, long version) {
        Customer customer = new IndividualCustomer(
                "ahmet@mail.com", "+905551112233", "Ahmet", "Yilmaz", "12345678901");
        customer.setId(id);
        ReflectionTestUtils.setField(customer, "version", version); // normally set by Hibernate
        return customer;
    }

    private static Address addressWithId(UUID addressId) {
        Address address = new Address("Home", "Bagdat Cd. 1", "Kadikoy", "Istanbul", "34710", "TR");
        address.setId(addressId);
        return address;
    }
}
