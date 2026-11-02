package com.bootcamp.customer_service.service.impl;

import com.bootcamp.common.dto.PageResponse;
import com.bootcamp.common.exception.PreconditionFailedException;
import com.bootcamp.common.exception.RecordNotFoundException;
import com.bootcamp.customer_service.dto.request.AddAddressRequest;
import com.bootcamp.customer_service.dto.request.AddDocumentRequest;
import com.bootcamp.customer_service.dto.request.CreateCustomerRequest;
import com.bootcamp.customer_service.dto.request.CustomerSearchFilter;
import com.bootcamp.customer_service.dto.request.UpdateContactRequest;
import com.bootcamp.customer_service.dto.response.AddressResponse;
import com.bootcamp.customer_service.dto.response.CustomerDetailResponse;
import com.bootcamp.customer_service.dto.response.CustomerSummaryResponse;
import com.bootcamp.customer_service.dto.response.DocumentResponse;
import com.bootcamp.customer_service.entity.Address;
import com.bootcamp.customer_service.entity.Customer;
import com.bootcamp.customer_service.entity.Document;
import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import com.bootcamp.customer_service.mapper.CustomerMapper;
import com.bootcamp.customer_service.repository.CustomerRepository;
import com.bootcamp.customer_service.repository.CustomerSpecifications;
import com.bootcamp.customer_service.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper mapper;
    private final Clock clock;

    // Duplicate email / national id / tax number is not pre-checked with existsBy (check-then-act race).
    // The unique constraints are the single source of truth; the global handler maps them to 409.
    @Override
    @Transactional
    public CustomerDetailResponse create(CreateCustomerRequest request) {
        Customer saved = customerRepository.saveAndFlush(mapper.toEntity(request));
        return mapper.toDetail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getById(UUID id) {
        return mapper.toDetail(findWithAddressesOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerSummaryResponse> search(CustomerSearchFilter filter, Pageable pageable) {
        return new PageResponse<>(customerRepository
                .findAll(CustomerSpecifications.matching(filter), pageable)
                .map(mapper::toSummary));
    }

    @Override
    @Transactional
    public CustomerDetailResponse updateContact(UUID id, long expectedVersion, UpdateContactRequest request) {
        Customer customer = findForUpdate(id, expectedVersion);
        customer.changeContact(request.email(), request.phoneNumber());
        return flushAndMap(customer);
    }

    @Override
    @Transactional
    public CustomerDetailResponse changeStatus(UUID id, long expectedVersion, CustomerStatus status) {
        Customer customer = findForUpdate(id, expectedVersion);
        customer.changeStatus(status);
        return flushAndMap(customer);
    }

    @Override
    @Transactional
    public CustomerDetailResponse approveKyc(UUID id, long expectedVersion) {
        Customer customer = findForUpdate(id, expectedVersion);
        customer.approveKyc(now());
        return flushAndMap(customer);
    }

    @Override
    @Transactional
    public CustomerDetailResponse rejectKyc(UUID id, long expectedVersion, String reason) {
        Customer customer = findForUpdate(id, expectedVersion);
        customer.rejectKyc(reason, now());
        return flushAndMap(customer);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(UUID customerId, AddAddressRequest request) {
        Customer customer = findForChildChangeOrThrow(customerId);
        Address address = customer.addAddress(mapper.toEntity(request));
        customerRepository.flush(); // cascade inserts the address now, so createdAt/id are in the response
        return mapper.toResponse(address);
    }

    @Override
    @Transactional
    public void removeAddress(UUID customerId, UUID addressId) {
        // Goes through the aggregate: Customer.removeAddress only finds addresses of THIS customer (404 otherwise),
        // and orphanRemoval deletes the row when the transaction flushes.
        Customer customer = customerRepository.findWithAddressesByIdForUpdate(customerId)
                .orElseThrow(() -> notFound(customerId));
        customer.removeAddress(addressId);
    }

    @Override
    @Transactional
    public DocumentResponse addDocument(UUID customerId, AddDocumentRequest request) {
        Customer customer = findForChildChangeOrThrow(customerId);
        Document document = customer.addDocument(mapper.toEntity(request));
        customerRepository.flush();
        return mapper.toResponse(document);
    }

    // Two layers of protection for conditional updates:
    // 1. Here: the client's If-Match version vs the current row -> catches changes made between the client's
    //    GET and this request (another operator, minutes ago). @Version alone cannot see that.
    // 2. @Version on flush: catches a concurrent transaction committing between this read and our UPDATE
    //    -> ObjectOptimisticLockingFailureException -> 409 in the global handler.
    private Customer findForUpdate(UUID id, long expectedVersion) {
        Customer customer = findWithAddressesOrThrow(id);
        if (!Objects.equals(customer.getVersion(), expectedVersion)) {
            throw new PreconditionFailedException(String.format(
                    "Customer %s was modified by another request (current version %d, If-Match %d). Reload and try again.",
                    id, customer.getVersion(), expectedVersion));
        }
        return customer;
    }

    // Flush so the version increment happens now and the response (and ETag) carries the new version.
    private CustomerDetailResponse flushAndMap(Customer customer) {
        customerRepository.flush();
        return mapper.toDetail(customer);
    }

    private Customer findWithAddressesOrThrow(UUID id) {
        return customerRepository.findWithAddressesById(id).orElseThrow(() -> notFound(id));
    }

    // Force-increment read: the customer version goes up at commit, see CustomerRepository.findByIdForUpdate.
    private Customer findForChildChangeOrThrow(UUID id) {
        return customerRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
    }

    private static RecordNotFoundException notFound(UUID id) {
        return new RecordNotFoundException(String.format("Customer %s not found", id));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
