package com.bootcamp.customer_service.controller;

import com.bootcamp.customer_service.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full HTTP flow against a real Postgres: controller -> service -> JPA -> DB -> ProblemDetail.
// No @Transactional: every request commits, like in production. Each test creates its own customers
// with unique email / national id, so tests do not depend on each other.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CustomerControllerIT {

    private static final String CUSTOMERS_URL = "/api/v1/customers";

    @Autowired
    private MockMvc mockMvc;

    // Roadmap DoD: create -> add address -> approve KYC -> withdraw approval; then REJECTED -> VERIFIED is refused.
    @Test
    void kycFlow_createAddressApproveWithdraw_thenRejectedCannotBeVerified() throws Exception {
        String id = createIndividual(uniqueEmail());

        mockMvc.perform(post(CUSTOMERS_URL + "/{id}/addresses", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country").value("TR"));

        // Adding the address bumped the customer version (force increment): 0 -> 1
        String etag = etagOf(id);
        mockMvc.perform(post(CUSTOMERS_URL + "/{id}/kyc/approve", id).header(HttpHeaders.IF_MATCH, etag))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"2\""))
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.addresses", hasSize(1)));

        mockMvc.perform(post(CUSTOMERS_URL + "/{id}/kyc/reject", id)
                        .header(HttpHeaders.IF_MATCH, "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"forged document\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("REJECTED"))
                .andExpect(jsonPath("$.kycRejectionReason").value("forged document"));

        mockMvc.perform(post(CUSTOMERS_URL + "/{id}/kyc/approve", id).header(HttpHeaders.IF_MATCH, "\"3\""))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("REJECTED to VERIFIED")));
    }

    @Test
    void approveKyc_returns428_whenIfMatchIsMissing() throws Exception {
        String id = createIndividual(uniqueEmail());

        mockMvc.perform(post(CUSTOMERS_URL + "/{id}/kyc/approve", id))
                .andExpect(status().is(428))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // Operator B still holds ETag "0" after operator A approved.
    @Test
    void rejectKyc_returns412_whenIfMatchIsStale() throws Exception {
        String id = createIndividual(uniqueEmail());
        mockMvc.perform(post(CUSTOMERS_URL + "/{id}/kyc/approve", id).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isOk());

        mockMvc.perform(post(CUSTOMERS_URL + "/{id}/kyc/reject", id)
                        .header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"forged document\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.title").value("PRECONDITION_FAILED error occurred in customer-service"));
    }

    @Test
    void rejectKyc_returns400_whenReasonIsMissing() throws Exception {
        String id = createIndividual(uniqueEmail());

        mockMvc.perform(post(CUSTOMERS_URL + "/{id}/kyc/reject", id)
                        .header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason[0]").value("Rejection reason must not be empty"));
    }

    // Emails are stored lower-case, so the unique constraint is case-insensitive.
    @Test
    void create_returns409_whenEmailIsAlreadyUsedWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        createIndividual(email);

        mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(individualJson(email.toUpperCase(), uniqueNationalId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email is already in use"));
    }

    @Test
    void create_returns400_whenTypeIsUnknown() throws Exception {
        mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ALIEN\",\"email\":\"x@mail.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenCorporateTaxNumberIsInvalid() throws Exception {
        String json = """
                {"type":"CORPORATE","email":"%s","phoneNumber":"+902121112233",
                 "companyName":"Acme AS","taxNumber":"12AB","taxOffice":"Kadikoy"}
                """.formatted(uniqueEmail());

        mockMvc.perform(post(CUSTOMERS_URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.taxNumber[0]").value("Tax number must be 10 digits"));
    }

    // Regression test for the IDOR bug: customer A's URL must not reach customer B's address.
    @Test
    void removeAddress_returns404_andKeepsAddress_whenAddressBelongsToAnotherCustomer() throws Exception {
        String customerA = createIndividual(uniqueEmail());
        String customerB = createIndividual(uniqueEmail());
        MvcResult added = mockMvc.perform(post(CUSTOMERS_URL + "/{id}/addresses", customerB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson()))
                .andExpect(status().isCreated())
                .andReturn();
        String addressOfB = JsonPath.read(added.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete(CUSTOMERS_URL + "/{id}/addresses/{addressId}", customerA, addressOfB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(CUSTOMERS_URL + "/{id}", customerB))
                .andExpect(jsonPath("$.addresses", hasSize(1)));
    }

    @Test
    void removeAddress_returns204_whenAddressBelongsToCustomer() throws Exception {
        String id = createIndividual(uniqueEmail());
        MvcResult added = mockMvc.perform(post(CUSTOMERS_URL + "/{id}/addresses", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson()))
                .andReturn();
        String addressId = JsonPath.read(added.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete(CUSTOMERS_URL + "/{id}/addresses/{addressId}", id, addressId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CUSTOMERS_URL + "/{id}", id))
                .andExpect(jsonPath("$.addresses", hasSize(0)));
    }

    @Test
    void getById_returns404_whenCustomerDoesNotExist() throws Exception {
        mockMvc.perform(get(CUSTOMERS_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void search_findsByEmailIgnoringCase() throws Exception {
        String email = uniqueEmail();
        createIndividual(email);

        mockMvc.perform(get(CUSTOMERS_URL).param("email", email.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(email));
    }

    @Test
    void search_returns400_whenSortFieldIsNotWhitelisted() throws Exception {
        mockMvc.perform(get(CUSTOMERS_URL).param("sort", "nationalId"))
                .andExpect(status().isBadRequest());
    }

    private String createIndividual(String email) throws Exception {
        MvcResult result = mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(individualJson(email, uniqueNationalId())))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String etagOf(String id) throws Exception {
        return mockMvc.perform(get(CUSTOMERS_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
    }

    private static String individualJson(String email, String nationalId) {
        return """
                {"type":"INDIVIDUAL","email":"%s","phoneNumber":"+905551112233",
                 "firstName":"Test","lastName":"Customer","nationalId":"%s"}
                """.formatted(email, nationalId);
    }

    private static String addressJson() {
        return """
                {"title":"Home","line":"Bagdat Cd. 1","district":"Kadikoy","city":"Istanbul","postalCode":"34710"}
                """;
    }

    private static String uniqueEmail() {
        return "it-" + UUID.randomUUID().toString().substring(0, 8) + "@mail.com";
    }

    private static String uniqueNationalId() {
        return String.valueOf(10_000_000_000L + ThreadLocalRandom.current().nextLong(89_999_999_999L));
    }
}
