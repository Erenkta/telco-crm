package com.bootcamp.customer_service.mapper;

import com.bootcamp.customer_service.dto.request.AddAddressRequest;
import com.bootcamp.customer_service.dto.request.AddDocumentRequest;
import com.bootcamp.customer_service.dto.request.CreateCorporateCustomerRequest;
import com.bootcamp.customer_service.dto.request.CreateCustomerRequest;
import com.bootcamp.customer_service.dto.request.CreateIndividualCustomerRequest;
import com.bootcamp.customer_service.dto.response.AddressResponse;
import com.bootcamp.customer_service.dto.response.CustomerDetailResponse;
import com.bootcamp.customer_service.dto.response.CustomerSummaryResponse;
import com.bootcamp.customer_service.dto.response.DocumentResponse;
import com.bootcamp.customer_service.entity.Address;
import com.bootcamp.customer_service.entity.CorporateCustomer;
import com.bootcamp.customer_service.entity.Customer;
import com.bootcamp.customer_service.entity.Document;
import com.bootcamp.customer_service.entity.IndividualCustomer;
import org.springframework.stereotype.Component;

// Hand-written instead of MapStruct: both sides are polymorphic (sealed request, Customer subclasses),
// and a switch over the types reads more clearly than MapStruct's @SubclassMapping setup.
@Component
public class CustomerMapper {

    private static final String DEFAULT_COUNTRY = "TR";

    public Customer toEntity(CreateCustomerRequest request) {
        // Sealed interface: the compiler checks that every request type is handled.
        return switch (request) {
            case CreateIndividualCustomerRequest r ->
                    new IndividualCustomer(r.email(), r.phoneNumber(), r.firstName(), r.lastName(), r.nationalId());
            case CreateCorporateCustomerRequest r ->
                    new CorporateCustomer(r.email(), r.phoneNumber(), r.companyName(), r.taxNumber(), r.taxOffice());
        };
    }

    public Address toEntity(AddAddressRequest r) {
        String country = r.country() == null ? DEFAULT_COUNTRY : r.country();
        return new Address(r.title(), r.line(), r.district(), r.city(), r.postalCode(), country);
    }

    public Document toEntity(AddDocumentRequest r) {
        return new Document(r.documentType(), r.fileName(), r.storageKey(), r.contentType(), r.sizeBytes());
    }

    public CustomerSummaryResponse toSummary(Customer c) {
        return new CustomerSummaryResponse(c.getId(), c.getType(), c.getDisplayName(), c.getEmail(),
                c.getPhoneNumber(), c.getStatus(), c.getKycStatus(), c.getVersion(), c.getCreatedAt());
    }

    // Reads the addresses and documents collections: call it only inside a transaction where they can be loaded.
    public CustomerDetailResponse toDetail(Customer c) {
        IndividualCustomer individual = c instanceof IndividualCustomer i ? i : null;
        CorporateCustomer corporate = c instanceof CorporateCustomer k ? k : null;

        return new CustomerDetailResponse(
                c.getId(), c.getType(), c.getEmail(), c.getPhoneNumber(), c.getStatus(),
                c.getKycStatus(), c.getKycRejectionReason(), c.getKycStatusChangedAt(),
                individual == null ? null : individual.getFirstName(),
                individual == null ? null : individual.getLastName(),
                individual == null ? null : individual.getNationalId(),
                corporate == null ? null : corporate.getCompanyName(),
                corporate == null ? null : corporate.getTaxNumber(),
                corporate == null ? null : corporate.getTaxOffice(),
                c.getAddresses().stream().map(this::toResponse).toList(),
                c.getDocuments().stream().map(this::toResponse).toList(),
                c.getVersion(), c.getCreatedAt(), c.getUpdatedAt());
    }

    public AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getTitle(), a.getLine(), a.getDistrict(), a.getCity(),
                a.getPostalCode(), a.getCountry());
    }

    public DocumentResponse toResponse(Document d) {
        return new DocumentResponse(d.getId(), d.getDocumentType(), d.getFileName(), d.getStorageKey(),
                d.getContentType(), d.getSizeBytes(), d.getCreatedAt());
    }
}
