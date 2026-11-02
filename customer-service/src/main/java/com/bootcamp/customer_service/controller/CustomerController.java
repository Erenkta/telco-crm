package com.bootcamp.customer_service.controller;

import com.bootcamp.common.dto.PageResponse;
import com.bootcamp.customer_service.dto.request.AddAddressRequest;
import com.bootcamp.customer_service.dto.request.AddDocumentRequest;
import com.bootcamp.customer_service.dto.request.ChangeStatusRequest;
import com.bootcamp.customer_service.dto.request.CreateCustomerRequest;
import com.bootcamp.customer_service.dto.request.CustomerSearchFilter;
import com.bootcamp.customer_service.dto.request.RejectKycRequest;
import com.bootcamp.customer_service.dto.request.UpdateContactRequest;
import com.bootcamp.customer_service.dto.response.AddressResponse;
import com.bootcamp.customer_service.dto.response.CustomerDetailResponse;
import com.bootcamp.customer_service.dto.response.CustomerSummaryResponse;
import com.bootcamp.customer_service.dto.response.DocumentResponse;
import com.bootcamp.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Every response that returns a customer carries an ETag (its version).
// Updates of the customer itself require If-Match: missing -> 428, stale -> 412.
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerDetailResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerDetailResponse created = customerService.create(request);
        return withETag(ResponseEntity.status(HttpStatus.CREATED), created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailResponse> getById(@PathVariable UUID id) {
        return withETag(ResponseEntity.ok(), customerService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CustomerSummaryResponse>> search(CustomerSearchFilter filter, Pageable pageable) {
        return ResponseEntity.ok(customerService.search(filter, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDetailResponse> updateContact(@PathVariable UUID id,
                                                                @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                                                @Valid @RequestBody UpdateContactRequest request) {
        long version = IfMatch.requireVersion(ifMatch);
        return withETag(ResponseEntity.ok(), customerService.updateContact(id, version, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CustomerDetailResponse> changeStatus(@PathVariable UUID id,
                                                               @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                                               @Valid @RequestBody ChangeStatusRequest request) {
        long version = IfMatch.requireVersion(ifMatch);
        return withETag(ResponseEntity.ok(), customerService.changeStatus(id, version, request.status()));
    }

    @PostMapping("/{id}/kyc/approve")
    public ResponseEntity<CustomerDetailResponse> approveKyc(@PathVariable UUID id,
                                                             @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        long version = IfMatch.requireVersion(ifMatch);
        return withETag(ResponseEntity.ok(), customerService.approveKyc(id, version));
    }

    @PostMapping("/{id}/kyc/reject")
    public ResponseEntity<CustomerDetailResponse> rejectKyc(@PathVariable UUID id,
                                                            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                                            @Valid @RequestBody RejectKycRequest request) {
        long version = IfMatch.requireVersion(ifMatch);
        return withETag(ResponseEntity.ok(), customerService.rejectKyc(id, version, request.reason()));
    }

    @PostMapping("/{id}/addresses")
    public ResponseEntity<AddressResponse> addAddress(@PathVariable UUID id, @Valid @RequestBody AddAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.addAddress(id, request));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    public ResponseEntity<Void> removeAddress(@PathVariable UUID id, @PathVariable UUID addressId) {
        customerService.removeAddress(id, addressId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<DocumentResponse> addDocument(@PathVariable UUID id, @Valid @RequestBody AddDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.addDocument(id, request));
    }

    private static ResponseEntity<CustomerDetailResponse> withETag(ResponseEntity.BodyBuilder builder,
                                                                   CustomerDetailResponse body) {
        return builder.eTag(IfMatch.etagOf(body.version())).body(body);
    }
}
