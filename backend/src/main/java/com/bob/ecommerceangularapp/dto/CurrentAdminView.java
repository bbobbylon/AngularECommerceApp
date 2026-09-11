package com.bob.ecommerceangularapp.dto;

import java.util.List;

/** RBAC (roadmap #19): the calling admin's role(s), returned by {@code GET /api/admin/me}. */
public record CurrentAdminView(List<String> roles) {
}
