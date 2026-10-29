package com.bootcamp.identity_service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Same image as docker-compose, so tests run against the Postgres version we deploy.
// Singleton container: the static field lives once per JVM, so every Spring test context
// (@SpringBootTest, @DataJpaTest, ...) gets the same running Postgres instead of starting its own.
// Testcontainers' Ryuk container removes it when the JVM exits.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
