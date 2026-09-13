package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.ConsentRequest;
import com.bob.ecommerceangularapp.dto.ConsentView;
import com.bob.ecommerceangularapp.dto.DataExportView;
import com.bob.ecommerceangularapp.dto.DataRequestSubmission;
import com.bob.ecommerceangularapp.dto.DataRequestView;
import com.bob.ecommerceangularapp.entity.DataRequest;
import com.bob.ecommerceangularapp.service.DataRequestService;
import com.bob.ecommerceangularapp.service.PrivacyConsentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
import java.util.Optional;

/**
 * Public privacy surface (roadmap #24): the cookie/storage consent banner posts here, and data
 * subjects raise export/erasure requests here.
 *
 * <p>Every route is deliberately unauthenticated. Consent is given before anyone signs in, and the
 * whole point of the data-request flow is that it works for someone who no longer has (or never had)
 * an account — the emailed token is the identity check, not a session. {@code /api/privacy/**} is
 * rate-limited by {@code RateLimitFilter} for the same reason the newsletter endpoints are: it can
 * cause mail to be sent.
 *
 * <p>The confirm/erase/export routes return branded HTML rather than JSON, following
 * {@link NewsletterController}'s unsubscribe page — they are opened straight from an inbox, where
 * there is no SPA loaded to render a response.
 */
@RestController
@RequestMapping("/api/privacy")
public class PrivacyController {

    private final PrivacyConsentService privacyConsentService;
    private final DataRequestService dataRequestService;
    private final String frontendUrl;

    public PrivacyController(PrivacyConsentService privacyConsentService,
                             DataRequestService dataRequestService,
                             @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:4250}") String frontendUrl) {
        this.privacyConsentService = privacyConsentService;
        this.dataRequestService = dataRequestService;
        this.frontendUrl = frontendUrl;
    }

    // ----------------------------------------------------------------- consent

    /**
     * The current policy version. The banner re-prompts when this stops matching what the visitor
     * last agreed to — consent to an older policy is not consent to a new one.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> config() {
        return ResponseEntity.ok(Map.of("policyVersion", privacyConsentService.policyVersion()));
    }

    @PostMapping("/consent")
    public ResponseEntity<ConsentView> recordConsent(@Valid @RequestBody ConsentRequest request) {
        return ResponseEntity.ok(privacyConsentService.record(request));
    }

    /** 204 when this visitor has never chosen — the banner treats that as "show me". */
    @GetMapping("/consent")
    public ResponseEntity<ConsentView> currentConsent(@RequestParam String visitorId) {
        return privacyConsentService.currentFor(visitorId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ----------------------------------------------------------- data requests

    /**
     * Raises an export or erasure request. Always 202 with the same body shape, whether or not any
     * data exists for the address — a different response for "unknown email" would turn this into a
     * free lookup for whether someone shops here.
     */
    @PostMapping("/data-requests")
    public ResponseEntity<Map<String, Object>> submit(@Valid @RequestBody DataRequestSubmission submission) {
        DataRequestView view;
        try {
            view = dataRequestService.submit(submission);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        return ResponseEntity.accepted().body(Map.of(
                "message", "Check your inbox. We've sent a link to confirm this request — it expires in 48 hours.",
                "request", view));
    }

    /**
     * Opens the emailed link. Verifies the token and shows the subject what happens next.
     *
     * <p>Note what this does <em>not</em> do: erase anything. Mail scanners and link prefetchers
     * fetch URLs out of inboxes unprompted, so the destructive step is a POST the subject has to
     * press ({@link #erase}). An export is safe to offer straight from here because handing someone
     * their own data back changes nothing.
     */
    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam(required = false) String token) {
        Optional<DataRequest> found = dataRequestService.confirm(token);
        if (found.isEmpty()) {
            return html(page("Link expired or already used",
                    "This confirmation link is no longer valid. Privacy links work once and expire after 48 "
                            + "hours &mdash; raise a new request from your account page and we'll send a fresh one.",
                    link(frontendUrl + "/account", "Back to my account")));
        }
        DataRequest request = found.get();
        if (DataRequest.TYPE_EXPORT.equals(request.getRequestType())) {
            return html(page("Your data is ready",
                    "Identity confirmed. Download your data below &mdash; it's a JSON file containing "
                            + "everything we hold that's linked to your email address. The link works once.",
                    link("/api/privacy/export?token=" + request.getToken(), "Download my data (JSON)")));
        }
        return html(page("Confirm deletion",
                "Identity confirmed. Deleting removes your profile, saved addresses and cards, wishlist, "
                        + "alerts and marketing data. Your past orders are <strong>kept as financial records</strong>, "
                        + "as tax law requires &mdash; but your name, email and street address are erased from them. "
                        + "<strong>This cannot be undone.</strong>",
                eraseForm(request.getToken())));
    }

    /**
     * Performs the erasure. POST-only so that nothing but a deliberate press of the button can
     * trigger it — see {@link #confirm}.
     */
    @PostMapping("/erase")
    public ResponseEntity<String> erase(@RequestParam(required = false) String token) {
        Optional<String> summary = dataRequestService.erase(token);
        if (summary.isEmpty()) {
            return html(page("Link expired or already used",
                    "This deletion link is no longer valid. Privacy links work once and expire after 48 hours.",
                    link(frontendUrl + "/products", "Back to the shop")));
        }
        return html(page("Your data has been deleted",
                "Done. Here's exactly what happened:<br><br><span style=\"color:#1e2435;\">"
                        + escape(summary.get()) + "</span>",
                link(frontendUrl + "/products", "Back to the shop")));
    }

    /** The portable bundle, as a downloadable JSON file (GDPR Art. 20 asks for machine-readable). */
    @GetMapping("/export")
    public ResponseEntity<DataExportView> export(@RequestParam(required = false) String token) {
        return dataRequestService.export(token)
                .map(bundle -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"luv2shop-my-data.json\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(bundle))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.GONE).build());
    }

    // ------------------------------------------------------------------- pages

    private static ResponseEntity<String> html(String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body);
    }

    private static String link(String href, String label) {
        return "<a href=\"" + href + "\" style=\"display:inline-block;background:#ff5470;color:#fff;"
                + "text-decoration:none;font-weight:700;padding:12px 24px;border-radius:999px;\">" + label + "</a>";
    }

    private static String eraseForm(String token) {
        return "<form method=\"post\" action=\"/api/privacy/erase\" style=\"margin:0;\">"
                + "<input type=\"hidden\" name=\"token\" value=\"" + escape(token) + "\">"
                + "<button type=\"submit\" style=\"border:0;cursor:pointer;background:#d92d4e;color:#fff;"
                + "font-weight:700;font-size:15px;padding:13px 26px;border-radius:999px;\">"
                + "Yes, delete my data</button></form>";
    }

    private static String page(String heading, String message, String action) {
        return """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="robots" content="noindex">
                <title>Luv2Shop &mdash; Privacy</title></head>
                <body style="margin:0;font-family:'Segoe UI',Helvetica,Arial,sans-serif;background:#f5f7fd;color:#1e2435;">
                  <div style="max-width:560px;margin:64px auto;background:#fff;border:1px solid #e7ecf7;border-radius:16px;padding:40px;text-align:center;">
                    <div style="font-size:24px;font-weight:800;background:linear-gradient(125deg,#ff5470,#b15bff,#2bb9ff);-webkit-background-clip:text;background-clip:text;color:transparent;">&#128717;&#65039; Luv2Shop</div>
                    <h1 style="font-size:22px;margin:20px 0 12px;">%s</h1>
                    <p style="color:#515a73;line-height:1.7;margin:0 0 26px;text-align:left;">%s</p>
                    %s
                  </div>
                </body></html>""".formatted(heading, message, action);
    }

    /** These pages interpolate a server-built summary and a token; escape anyway, on principle. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
