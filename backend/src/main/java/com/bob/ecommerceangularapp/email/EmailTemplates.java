package com.bob.ecommerceangularapp.email;

import com.bob.ecommerceangularapp.entity.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Branded, inline-styled HTML for outgoing email. Built with plain text blocks (no extra
 * templating dependency) and placeholder replacement so the mail markup stays self-contained.
 * Colors mirror the Angular "fresh market" design system (accent #ff5470, teal #10b6a6).
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    private static final String LAYOUT = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
            </head>
            <body style="margin:0;padding:0;background:#f5f7fd;font-family:'Segoe UI',Helvetica,Arial,sans-serif;color:#1e2435;">
              <span style="display:none;max-height:0;overflow:hidden;opacity:0;">__PREHEADER__</span>
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f5f7fd;padding:24px 12px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e7ecf7;">
                    <tr><td style="background:linear-gradient(125deg,#ff5470 0%,#b15bff 50%,#2bb9ff 100%);padding:26px 32px;">
                      <span style="font-size:24px;font-weight:800;color:#ffffff;letter-spacing:-.5px;">&#128717;&#65039; Luv2<span style="color:#fff5b8;">Shop</span></span>
                    </td></tr>
                    <tr><td style="padding:32px;">__CONTENT__</td></tr>
                    <tr><td style="padding:22px 32px;background:#1b2133;color:#8b93ab;font-size:12px;line-height:1.6;">
                      <p style="margin:0 0 6px;color:#c7cde0;font-weight:600;">Luv2Shop &mdash; little things, big delight.</p>
                      __FOOTNOTE__
                      <p style="margin:8px 0 0;">&copy; 2026 Luv2Shop &middot; Demo project</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>""";

    private static String layout(String preheader, String content, String footnote) {
        return LAYOUT
                .replace("__PREHEADER__", esc(preheader))
                .replace("__CONTENT__", content)
                .replace("__FOOTNOTE__", footnote == null ? "" : footnote);
    }

    private static String button(String href, String label) {
        return "<a href=\"" + href + "\" style=\"display:inline-block;background:#ff5470;color:#ffffff;"
                + "text-decoration:none;font-weight:700;padding:13px 26px;border-radius:999px;font-size:15px;\">"
                + esc(label) + "</a>";
    }

    /** Welcome email sent when someone subscribes to the list (signup box or checkout opt-in). */
    public static String welcome(String name, String shopUrl, String unsubscribeUrl) {
        String content = """
                <h1 style="font-size:26px;margin:0 0 12px;">Welcome aboard, %s! &#127881;</h1>
                <p style="font-size:16px;line-height:1.6;color:#515a73;margin:0 0 18px;">
                  You're on the list. Each week we'll send a fresh, handpicked edit of books, coffee mugs,
                  mouse pads &amp; luggage &mdash; plus members-only deals before anyone else.
                </p>
                <p style="margin:0 0 26px;">%s</p>
                <p style="font-size:14px;color:#8b93ab;margin:0;">Curated for the desk, the commute, and everywhere in between.</p>
                """.formatted(esc(firstName(name)), button(shopUrl, "Start shopping"));
        return layout("Welcome to Luv2Shop — your weekly edit starts now.", content, unsubscribeFootnote(unsubscribeUrl));
    }

    /** Confirmation email after an order is placed. */
    public static String orderConfirmation(String name, String trackingNumber, String orderTotal, String ordersUrl) {
        String content = """
                <h1 style="font-size:26px;margin:0 0 12px;">Thanks for your order, %s! &#128230;</h1>
                <p style="font-size:16px;line-height:1.6;color:#515a73;margin:0 0 18px;">
                  We're packing it up now. Here are your details:
                </p>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f7fd;border-radius:12px;padding:18px 20px;margin:0 0 24px;">
                  <tr><td style="font-size:14px;color:#8b93ab;padding:4px 0;">Tracking number</td>
                      <td style="font-size:14px;font-weight:700;text-align:right;">%s</td></tr>
                  <tr><td style="font-size:14px;color:#8b93ab;padding:4px 0;">Order total</td>
                      <td style="font-size:14px;font-weight:700;text-align:right;color:#ec3a5c;">%s</td></tr>
                </table>
                <p style="margin:0 0 8px;">%s</p>
                """.formatted(esc(firstName(name)), esc(trackingNumber), esc(orderTotal), button(ordersUrl, "View my orders"));
        return layout("Your Luv2Shop order is confirmed.", content, null);
    }

    /** "It's back!" notification when a product the customer wanted is in stock again. */
    public static String backInStock(String productName, String productUrl) {
        String content = """
                <h1 style="font-size:26px;margin:0 0 12px;">It's back in stock! &#127881;</h1>
                <p style="font-size:16px;line-height:1.6;color:#515a73;margin:0 0 18px;">
                  Good news — <strong>%s</strong> is available again. Popular items sell out fast, so grab
                  yours before it's gone.
                </p>
                <p style="margin:0 0 8px;">%s</p>
                """.formatted(esc(productName), button(productUrl, "Shop now"));
        return layout("The item you wanted is back in stock.", content, null);
    }

    /** Nudge to come back and finish a cart that was left at checkout. */
    public static String abandonedCart(int itemCount, String total, String cartUrl) {
        String items = itemCount == 1 ? "1 item" : itemCount + " items";
        String content = """
                <h1 style="font-size:26px;margin:0 0 12px;">You left something behind &#128722;</h1>
                <p style="font-size:16px;line-height:1.6;color:#515a73;margin:0 0 18px;">
                  Your cart still has <strong>%s</strong> (%s) waiting. We saved it for you — pick up right
                  where you left off before it's gone.
                </p>
                <p style="margin:0 0 8px;">%s</p>
                """.formatted(items, esc(total), button(cartUrl, "Return to my cart"));
        return layout("Your cart is waiting for you.", content, null);
    }

    /** Confirmation that account/email preferences were updated. */
    public static String settingsUpdated(String name, boolean subscribed, String accountUrl) {
        String status = subscribed
                ? "You're <strong style=\"color:#10b6a6;\">subscribed</strong> to our weekly deals email."
                : "You've been <strong>unsubscribed</strong> from the weekly deals email.";
        String content = """
                <h1 style="font-size:24px;margin:0 0 12px;">Your preferences are saved &#9989;</h1>
                <p style="font-size:16px;line-height:1.6;color:#515a73;margin:0 0 18px;">Hi %s, %s</p>
                <p style="margin:0 0 8px;">%s</p>
                """.formatted(esc(firstName(name)), status, button(accountUrl, "Manage account"));
        return layout("Your Luv2Shop preferences were updated.", content, null);
    }

    /** Weekly marketing blast featuring a handful of products / current deals. */
    public static String weeklyAd(String name, List<Product> products, String frontendUrl,
                                  String saleUrl, String unsubscribeUrl) {
        StringBuilder grid = new StringBuilder();
        for (Product p : products) {
            grid.append(productRow(p, frontendUrl));
        }
        String content = """
                <p style="text-transform:uppercase;letter-spacing:.12em;font-size:12px;font-weight:700;color:#ff5470;margin:0 0 6px;">This week's edit</p>
                <h1 style="font-size:26px;margin:0 0 12px;">Hey %s, fresh picks just landed &#10024;</h1>
                <p style="font-size:16px;line-height:1.6;color:#515a73;margin:0 0 22px;">
                  Handpicked goods for the desk and the commute. Tap through for this week's best prices.
                </p>
                %s
                <p style="margin:24px 0 4px;text-align:center;">%s</p>
                """.formatted(esc(firstName(name)), grid, button(saleUrl, "Shop the sale"));
        return layout("Your weekly Luv2Shop edit is here.", content, unsubscribeFootnote(unsubscribeUrl));
    }

    private static String productRow(Product p, String frontendUrl) {
        String link = frontendUrl + "/products/" + p.getId();
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e7ecf7;border-radius:12px;margin:0 0 14px;">
                  <tr>
                    <td width="120" style="padding:14px;"><img src="%s" alt="%s" width="100" style="width:100px;height:auto;border-radius:8px;background:#f4f6fc;"></td>
                    <td style="padding:14px 16px 14px 0;vertical-align:middle;">
                      <p style="font-weight:700;font-size:15px;margin:0 0 6px;">%s</p>
                      <p style="margin:0 0 10px;font-size:15px;">%s</p>
                      <a href="%s" style="color:#ec3a5c;font-weight:700;text-decoration:none;font-size:14px;">Shop now &rarr;</a>
                    </td>
                  </tr>
                </table>""".formatted(p.getImageUrl(), esc(p.getName()), esc(p.getName()), priceHtml(p), link);
    }

    private static String priceHtml(Product p) {
        BigDecimal original = p.getOriginalPrice();
        String now = money(p.getUnitPrice());
        if (original != null && original.compareTo(p.getUnitPrice()) > 0) {
            return "<span style=\"color:#ec3a5c;font-weight:700;\">" + now + "</span> "
                    + "<span style=\"text-decoration:line-through;color:#8b93ab;font-size:13px;\">" + money(original) + "</span>";
        }
        return "<span style=\"color:#1e2435;font-weight:700;\">" + now + "</span>";
    }

    private static String unsubscribeFootnote(String unsubscribeUrl) {
        if (unsubscribeUrl == null || unsubscribeUrl.isBlank()) {
            return "";
        }
        return "<p style=\"margin:0;\">You're receiving this because you subscribed to Luv2Shop. "
                + "<a href=\"" + unsubscribeUrl + "\" style=\"color:#8bb4ff;\">Unsubscribe</a>.</p>";
    }

    private static String money(BigDecimal value) {
        return "$" + value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "there";
        }
        return name.trim().split("\\s+")[0];
    }

    /** Minimal HTML escaping for interpolated text (names, product names, totals). */
    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
