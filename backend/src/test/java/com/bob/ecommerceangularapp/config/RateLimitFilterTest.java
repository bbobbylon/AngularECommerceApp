package com.bob.ecommerceangularapp.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test (no Spring context) for the public-endpoint abuse protection. Drives the filter with
 * mock servlet objects to assert the three behaviours: pass-through for unguarded traffic, the body
 * size cap (413), and the per-IP/per-minute request cap (429).
 */
class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter();

    @Test
    void readsAndUnguardedWritesPassThrough() throws ServletException, IOException {
        // GET on a guarded prefix is not limited (only mutating methods are)...
        MockHttpServletResponse getResponse = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/api/reviews"), getResponse, new MockFilterChain());
        assertThat(getResponse.getStatus()).isEqualTo(200);

        // ...and a POST to an unguarded path (the core checkout flow) flows straight through.
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/checkout/purchase"), postResponse, new MockFilterChain());
        assertThat(postResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsOversizedBodyWith413() throws ServletException, IOException {
        MockHttpServletRequest request = request("POST", "/api/newsletter/subscribe");
        request.setContentType("application/json");
        request.setContent(new byte[64 * 1024 + 1]); // one byte over the 64 KB cap
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void enforcesPerMinuteLimitWith429() throws ServletException, IOException {
        MockHttpServletResponse response = null;
        // 30 requests are allowed; the 31st within the window is rejected.
        for (int i = 0; i < 31; i++) {
            response = new MockHttpServletResponse();
            filter.doFilter(request("POST", "/api/reviews"), response, new MockFilterChain());
        }

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("203.0.113.7");
        return request;
    }
}
