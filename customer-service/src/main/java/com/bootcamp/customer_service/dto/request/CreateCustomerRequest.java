package com.bootcamp.customer_service.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// Polymorphic request body: the "type" field tells Jackson which record to create.
// {"type":"INDIVIDUAL", ...} -> CreateIndividualCustomerRequest, {"type":"CORPORATE", ...} -> CreateCorporateCustomerRequest.
// A missing or unknown type fails JSON parsing, which the global handler already returns as 400.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateIndividualCustomerRequest.class, name = "INDIVIDUAL"),
        @JsonSubTypes.Type(value = CreateCorporateCustomerRequest.class, name = "CORPORATE")
})
public sealed interface CreateCustomerRequest permits CreateIndividualCustomerRequest, CreateCorporateCustomerRequest {
    String email();

    String phoneNumber();
}
