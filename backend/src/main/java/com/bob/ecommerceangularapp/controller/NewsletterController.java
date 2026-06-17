package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.SubscribeRequest;
import com.bob.ecommerceangularapp.service.NewsletterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;
    private final String adminToken;
    private final String frontendUrl;

    public NewsletterController(NewsletterService newsletterService,
                               @Value("${app.newsletter.admin-token:}") String adminToken,
                               @Value("${app.frontend-url:http://localhost:4250}") String frontendUrl) {
        this.newsletterService = newsletterService;
        this.adminToken = adminToken;
        this.frontendUrl = frontendUrl;
    }

    /** Signup box → subscribe + send a welcome email. */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(@Valid @RequestBody SubscribeRequest request) {
        try {
            newsletterService.subscribe(request.email(), request.name());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", "You're subscribed! Check your inbox for a welcome email."));
    }

    /**
     * One-click unsubscribe straight from an email link. Returns a small branded HTML page
     * (not JSON) so it renders nicely when opened in a browser from the inbox.
     */
    @GetMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam(required = false) String token,
                                              @RequestParam(required = false) String email) {
        boolean done = (token != null && !token.isBlank())
                ? newsletterService.unsubscribeByToken(token)
                : newsletterService.unsubscribeByEmail(email);

        String message = done
                ? "You've been unsubscribed. Sorry to see you go &mdash; you can resubscribe anytime."
                : "We couldn't find a matching subscription, but you won't receive further marketing email.";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(unsubscribePage(message));
    }

    /**
     * Manually trigger the weekly blast (useful for testing). Guarded by app.newsletter.admin-token:
     * when the token is unset the endpoint is disabled; otherwise the request must supply a match.
     */
    @PostMapping("/send-now")
    public ResponseEntity<Map<String, Object>> sendNow(@RequestParam(required = false) String token) {
        if (adminToken == null || adminToken.isBlank() || !adminToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Not authorized. Set app.newsletter.admin-token and pass ?token=..."));
        }
        int sent = newsletterService.sendWeeklyBlast();
        return ResponseEntity.ok(Map.of("message", "Weekly blast triggered.", "recipients", sent));
    }

    private String unsubscribePage(String message) {
        return """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Luv2Shop &mdash; Unsubscribe</title></head>
                <body style="margin:0;font-family:'Segoe UI',Helvetica,Arial,sans-serif;background:#f5f7fd;color:#1e2435;">
                  <div style="max-width:520px;margin:64px auto;background:#fff;border:1px solid #e7ecf7;border-radius:16px;padding:40px;text-align:center;">
                    <div style="font-size:24px;font-weight:800;background:linear-gradient(125deg,#ff5470,#b15bff,#2bb9ff);-webkit-background-clip:text;background-clip:text;color:transparent;">&#128717;&#65039; Luv2Shop</div>
                    <h1 style="font-size:22px;margin:20px 0 12px;">Email preferences updated</h1>
                    <p style="color:#515a73;line-height:1.6;margin:0 0 24px;">%s</p>
                    <a href="%s/products" style="display:inline-block;background:#ff5470;color:#fff;text-decoration:none;font-weight:700;padding:12px 24px;border-radius:999px;">Back to the shop</a>
                  </div>
                </body></html>""".formatted(message, frontendUrl);
    }
}
