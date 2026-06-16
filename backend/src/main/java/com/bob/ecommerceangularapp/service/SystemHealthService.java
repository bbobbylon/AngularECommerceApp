package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dto.SystemHealth;
import com.bob.ecommerceangularapp.dto.SystemHealth.ComponentStatus;
import com.bob.ecommerceangularapp.email.EmailService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Builds the admin {@link SystemHealth} summary: which integrations are wired up, the build version,
 * and uptime. Optional integrations being "not ready" is expected (the app degrades gracefully), so
 * only an unreachable database flips the overall status to DOWN.
 */
@Service
public class SystemHealthService {

    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final Environment env;
    private final ObjectProvider<BuildProperties> buildProperties;

    public SystemHealthService(ProductRepository productRepository,
                               EmailService emailService,
                               Environment env,
                               ObjectProvider<BuildProperties> buildProperties) {
        this.productRepository = productRepository;
        this.emailService = emailService;
        this.env = env;
        this.buildProperties = buildProperties;
    }

    public SystemHealth current() {
        ComponentStatus database = checkDatabase();

        boolean emailReady = emailService.isEnabled();
        ComponentStatus email = new ComponentStatus("Email (SMTP)", emailReady,
                emailReady ? "Gmail SMTP configured" : "Not configured — emails are no-ops");

        boolean stripeReady = StringUtils.hasText(env.getProperty("stripe.key.secret"));
        ComponentStatus payments = new ComponentStatus("Payments (Stripe)", stripeReady,
                stripeReady ? "Stripe key present" : "Demo mode — no Stripe key");

        boolean oktaReady = StringUtils.hasText(
                env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"));
        ComponentStatus auth = new ComponentStatus("Auth (Okta OIDC)", oktaReady,
                oktaReady ? "JWT validation active" : "Open mode — no issuer configured");

        BuildProperties build = buildProperties.getIfAvailable();
        String version = build != null ? build.getVersion() : "dev";
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        String[] profiles = env.getActiveProfiles();
        String profile = profiles.length == 0 ? "default" : String.join(",", profiles);

        String status = database.ready() ? "UP" : "DOWN";
        return new SystemHealth(status, version, profile, uptimeSeconds,
                List.of(database, email, payments, auth));
    }

    private ComponentStatus checkDatabase() {
        try {
            long products = productRepository.count();
            return new ComponentStatus("Database (MySQL)", true, products + " products");
        } catch (Exception e) {
            return new ComponentStatus("Database (MySQL)", false, "Unreachable: " + e.getMessage());
        }
    }
}
