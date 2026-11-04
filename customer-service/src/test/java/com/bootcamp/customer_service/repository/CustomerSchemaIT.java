package com.bootcamp.customer_service.repository;

import com.bootcamp.customer_service.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The DB rules from V1__init.sql, tested with plain SQL so the JPA layer cannot hide a missing constraint.
// ddl-auto=validate does NOT check CHECK constraints; only a test like this does.
// @DataJpaTest rolls back each test's transaction.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CustomerSchemaIT {

    private static final String INSERT = """
            INSERT INTO customers (customer_type, email, phone_number, kyc_status, kyc_rejection_reason,
                                   first_name, last_name, national_id, company_name, tax_number, tax_office)
            VALUES (?, ?, '+905551112233', ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void validIndividualAndCorporateRows_areAccepted() {
        assertThatCode(() -> {
            jdbc.update(INSERT, "INDIVIDUAL", "ind@mail.com", "PENDING", null, "Ahmet", "Yilmaz", "12345678901", null, null, null);
            jdbc.update(INSERT, "CORPORATE", "corp@mail.com", "PENDING", null, null, null, null, "Acme AS", "1234567890", "Kadikoy");
        }).doesNotThrowAnyException();
    }

    @Test
    void individualWithCompanyFields_isRejected() {
        assertThatThrownBy(() -> jdbc.update(INSERT,
                "INDIVIDUAL", "mix@mail.com", "PENDING", null, "Ahmet", "Yilmaz", "12345678901", "Half Corp", null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_customers_type_fields");
    }

    @Test
    void corporateWithoutTaxNumber_isRejected() {
        assertThatThrownBy(() -> jdbc.update(INSERT,
                "CORPORATE", "corp2@mail.com", "PENDING", null, null, null, null, "Acme AS", null, "Kadikoy"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_customers_type_fields");
    }

    @Test
    void upperCaseEmail_isRejected() {
        assertThatThrownBy(() -> jdbc.update(INSERT,
                "INDIVIDUAL", "Upper@Mail.com", "PENDING", null, "Ahmet", "Yilmaz", "12345678902", null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_customers_email_lower");
    }

    @Test
    void rejectedWithoutReason_isRejected() {
        assertThatThrownBy(() -> jdbc.update(INSERT,
                "INDIVIDUAL", "rej@mail.com", "REJECTED", null, "Ahmet", "Yilmaz", "12345678903", null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_customers_rejection_reason");
    }

    @Test
    void nationalIdWithLetters_isRejected() {
        assertThatThrownBy(() -> jdbc.update(INSERT,
                "INDIVIDUAL", "tc@mail.com", "PENDING", null, "Ahmet", "Yilmaz", "1234567890A", null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_customers_national_id_format");
    }

    @Test
    void duplicateNationalId_isRejected() {
        jdbc.update(INSERT, "INDIVIDUAL", "dup1@mail.com", "PENDING", null, "Ahmet", "Yilmaz", "12345678904", null, null, null);

        assertThatThrownBy(() -> jdbc.update(INSERT,
                "INDIVIDUAL", "dup2@mail.com", "PENDING", null, "Mehmet", "Kaya", "12345678904", null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_customers_national_id");
    }
}
