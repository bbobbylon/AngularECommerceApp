package com.bob.ecommerceangularapp.email;

import com.bob.ecommerceangularapp.entity.Product;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Sends transactional + marketing email via the configured SMTP server (Gmail by default).
 *
 * <p>Email "degrades gracefully" like Okta/Stripe: every method is a no-op until SMTP
 * credentials are present ({@code spring.mail.username} is set). This keeps the app fully
 * runnable for local dev with zero secrets, and means a mail outage can never break a
 * checkout or a preference save — failures are logged, never thrown.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String username;
    private final String from;
    private final String fromName;
    private final String frontendUrl;
    private final String apiUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${spring.mail.username:}") String username,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.mail.from-name:Luv2Shop}") String fromName,
                        @Value("${app.frontend-url:http://localhost:4250}") String frontendUrl,
                        @Value("${app.api-url:http://localhost:8585}") String apiUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.username = username;
        this.from = (from == null || from.isBlank()) ? username : from;
        this.fromName = fromName;
        this.frontendUrl = stripTrailingSlash(frontendUrl);
        this.apiUrl = stripTrailingSlash(apiUrl);
    }

    /** True only when SMTP credentials are configured, so real delivery will be attempted. */
    public boolean isEnabled() {
        return username != null && !username.isBlank() && mailSenderProvider.getIfAvailable() != null;
    }

    public void sendWelcome(String to, String name) {
        send(to, "Welcome to Luv2Shop 🛍️",
                EmailTemplates.welcome(name, frontendUrl + "/products", unsubscribeUrl(to, null)));
    }

    public void sendOrderConfirmation(String to, String name, String trackingNumber, BigDecimal orderTotal) {
        String total = "$" + orderTotal.setScale(2, java.math.RoundingMode.HALF_UP);
        send(to, "Your Luv2Shop order is confirmed ✅",
                EmailTemplates.orderConfirmation(name, trackingNumber, total, frontendUrl + "/members/orders"));
    }

    public void sendBackInStock(String to, String productName, Long productId) {
        send(to, "Back in stock: " + productName + " 🎉",
                EmailTemplates.backInStock(productName, frontendUrl + "/products/" + productId));
    }

    public void sendSettingsUpdated(String to, String name, boolean subscribed) {
        send(to, "Your Luv2Shop preferences were updated",
                EmailTemplates.settingsUpdated(name, subscribed, frontendUrl + "/account"));
    }

    public void sendWeeklyAd(String to, String name, List<Product> products, String unsubscribeToken) {
        send(to, "Your weekly Luv2Shop edit ✨",
                EmailTemplates.weeklyAd(name, products, frontendUrl, frontendUrl + "/sale",
                        unsubscribeUrl(to, unsubscribeToken)));
    }

    /** Backend unsubscribe link so it works straight from the inbox without loading the SPA. */
    private String unsubscribeUrl(String email, String token) {
        if (token != null && !token.isBlank()) {
            return apiUrl + "/api/newsletter/unsubscribe?token=" + token;
        }
        return apiUrl + "/api/newsletter/unsubscribe?email=" + email;
    }

    private void send(String to, String subject, String html) {
        if (!isEnabled()) {
            log.debug("Email disabled (no SMTP credentials) — skipping '{}' to {}", subject, to);
            return;
        }
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            JavaMailSender mailSender = mailSenderProvider.getObject();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            try {
                helper.setFrom(from, fromName);
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(from);
            }
            mailSender.send(message);
            log.info("Sent email '{}' to {}", subject, to);
        } catch (Exception e) {
            // Never let a mail failure break the calling flow (checkout, preference save, blast).
            log.warn("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
