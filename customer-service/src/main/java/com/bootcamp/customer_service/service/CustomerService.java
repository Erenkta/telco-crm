package com.bootcamp.customer_service.service;

import com.bootcamp.common.dto.PageResponse;
import com.bootcamp.customer_service.dto.request.AddAddressRequest;
import com.bootcamp.customer_service.dto.request.AddDocumentRequest;
import com.bootcamp.customer_service.dto.request.CreateCustomerRequest;
import com.bootcamp.customer_service.dto.request.CustomerSearchFilter;
import com.bootcamp.customer_service.dto.request.UpdateContactRequest;
import com.bootcamp.customer_service.dto.response.AddressResponse;
import com.bootcamp.customer_service.dto.response.CustomerDetailResponse;
import com.bootcamp.customer_service.dto.response.CustomerSummaryResponse;
import com.bootcamp.customer_service.dto.response.DocumentResponse;
import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// Methods with an expectedVersion parameter are conditional updates: the client sends the version it last saw
// (If-Match header) and the update is refused with 412 if the customer changed since then.
public interface CustomerService {

    CustomerDetailResponse create(CreateCustomerRequest request);

    CustomerDetailResponse getById(UUID id);

    PageResponse<CustomerSummaryResponse> search(CustomerSearchFilter filter, Pageable pageable);

    CustomerDetailResponse updateContact(UUID id, long expectedVersion, UpdateContactRequest request);

    CustomerDetailResponse changeStatus(UUID id, long expectedVersion, CustomerStatus status);

    CustomerDetailResponse approveKyc(UUID id, long expectedVersion);

    CustomerDetailResponse rejectKyc(UUID id, long expectedVersion, String reason);

    AddressResponse addAddress(UUID customerId, AddAddressRequest request);

    void removeAddress(UUID customerId, UUID addressId);

    DocumentResponse addDocument(UUID customerId, AddDocumentRequest request);
}
