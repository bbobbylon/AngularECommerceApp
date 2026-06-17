package com.bob.ecommerceangularapp;

import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dto.ProductCardView;
import com.bob.ecommerceangularapp.service.ProductQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration test against a <b>real MySQL 8.4</b> (the production engine) via
 * Testcontainers. The H2 slice tests build their schema with Hibernate; this one instead boots the
 * app the way production does — <b>Flyway enabled</b> and <b>{@code ddl-auto=validate}</b> — so it
 * genuinely exercises the {@code V1}/{@code V2} migrations and proves the JPA entities match the
 * migrated schema on the engine we ship. The {@code DataLoader} seeder also runs against MySQL.
 *
 * <p>{@code disabledWithoutDocker = true} auto-skips this when Docker isn't available, so the default
 * {@code ./mvnw test} still runs anywhere (the convention) while CI — which has Docker — runs it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@TestPropertySource(properties = {
        // Override the H2 test profile: run the production-style migration + validation path.
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.docker.compose.enabled=false",
})
class MySqlIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductQueryService productQueryService;

    @Test
    void flywayMigratesAndSchemaValidatesAgainstMySql() {
        // Reaching this point already proves the context booted: Flyway applied V1+V2 and Hibernate's
        // ddl-auto=validate matched the entities against the migrated MySQL schema. Confirm the data
        // layer round-trips by reading the catalog the DataLoader seeded.
        assertThat(productRepository.count()).isPositive();
    }

    @Test
    void facetedSearchRunsAgainstMySql() {
        Page<ProductCardView> result =
                productQueryService.search(null, null, null, null, null, null, null, PageRequest.of(0, 5));
        assertThat(result.getTotalElements()).isPositive();
        assertThat(result.getContent()).isNotEmpty();
    }
}
