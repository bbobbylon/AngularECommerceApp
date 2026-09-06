package com.bob.ecommerceangularapp;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dto.ProductCardView;
import com.bob.ecommerceangularapp.service.ProductQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test against a <b>real MySQL 8.4</b> (the production engine) via
 * Testcontainers. The H2 slice tests build their schema with Hibernate; this one instead boots the
 * app the way production does — <b>Flyway enabled</b> and <b>{@code ddl-auto=validate}</b> — so it
 * genuinely exercises the {@code V1}-{@code V17} migrations and proves the JPA entities match the
 * migrated schema on the engine we ship. The {@code DataLoader} seeder also runs against MySQL.
 *
 * <p>{@code disabledWithoutDocker = true} auto-skips this when Docker isn't available, so the default
 * {@code ./mvnw test} still runs anywhere (the convention) while CI — which has Docker — runs it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void flywayMigratesAndSchemaValidatesAgainstMySql() {
        // Reaching this point already proves the context booted: Flyway applied V1-V17 and Hibernate's
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

    /**
     * The "genuinely proven" bar for roadmap #21 Milestone A: a second tenant's catalog is invisible to
     * the demo tenant (and vice versa) through the real request pipeline (TenantResolutionFilter ->
     * ProductQueryService's explicit tenant predicate), a cross-tenant single-item GET 404s via
     * TenantResourceGuardFilter (the findById gap a query-level predicate can't reach — Spring Data
     * REST's item resources go straight to the repository), and revenue aggregation is scoped per
     * tenant. Seeded via raw JDBC (bypasses Hibernate entirely, same as V17's own migration) rather
     * than JPA saves, purely to keep the fixture simple — no session/tenant-context dance needed.
     */
    @Test
    void secondTenantIsIsolatedFromTheDemoTenant() throws Exception {
        jdbcTemplate.update("insert into tenant (slug, display_name, active) values ('acme', 'Acme Co', true)");
        Long acmeTenantId = jdbcTemplate.queryForObject("select id from tenant where slug = 'acme'", Long.class);

        jdbcTemplate.update("insert into product_category (category_name, tenant_id) values (?, ?)",
                "Acme Widgets", acmeTenantId);
        Long acmeCategoryId = jdbcTemplate.queryForObject(
                "select id from product_category where category_name = 'Acme Widgets'", Long.class);

        jdbcTemplate.update("insert into product "
                        + "(sku, name, description, unit_price, image_url, active, units_in_stock, "
                        + "category_id, tenant_id, date_created, last_updated) "
                        + "values (?, ?, ?, ?, ?, true, 10, ?, ?, now(), now())",
                "ACME-0001", "Acme Exclusive Widget", "Only visible to the acme tenant",
                new BigDecimal("42.00"), "", acmeCategoryId, acmeTenantId);
        Long acmeProductId = jdbcTemplate.queryForObject("select id from product where sku = 'ACME-0001'", Long.class);

        jdbcTemplate.update("insert into orders (tenant_id, order_tracking_number, total_price, "
                        + "total_quantity, date_created, last_updated) values (?, ?, ?, 1, now(), now())",
                acmeTenantId, "ACME-TRACK-1", new BigDecimal("42.00"));

        // Faceted search scoped to acme sees the acme-only product...
        mockMvc.perform(get("/api/catalog/search").header("X-Tenant-Id", "acme").param("keyword", "Acme Exclusive"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Acme Exclusive Widget")));

        // ...but the demo tenant's search never sees it.
        mockMvc.perform(get("/api/catalog/search").header("X-Tenant-Id", "demo").param("keyword", "Acme Exclusive"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Acme Exclusive Widget"))));

        // The findById-shaped single-item resource: right tenant sees it...
        mockMvc.perform(get("/api/products/" + acmeProductId).header("X-Tenant-Id", "acme"))
                .andExpect(status().isOk());

        // ...wrong tenant gets a 404 (via TenantResourceGuardFilter), never a 403 (no existence leak).
        mockMvc.perform(get("/api/products/" + acmeProductId).header("X-Tenant-Id", "demo"))
                .andExpect(status().isNotFound());

        // Revenue aggregation is scoped: acme's order never counts toward the demo tenant's total.
        Long demoTenantId = jdbcTemplate.queryForObject("select id from tenant where slug = 'demo'", Long.class);
        assertThat(orderRepository.sumTotalRevenue(acmeTenantId)).isEqualByComparingTo(new BigDecimal("42.00"));
        assertThat(orderRepository.sumTotalRevenue(demoTenantId)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
