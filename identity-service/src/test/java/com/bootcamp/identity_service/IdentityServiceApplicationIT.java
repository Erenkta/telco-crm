package com.bootcamp.identity_service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class IdentityServiceApplicationIT {

    @Autowired
    private Flyway flyway;

    // If the context starts, Flyway ran on an empty DB and Hibernate's ddl-auto=validate accepted the schema.
    @Test
    void contextLoads_andAllMigrationsAreApplied() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
    }
}
