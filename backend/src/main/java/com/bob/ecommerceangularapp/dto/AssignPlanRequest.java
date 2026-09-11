package com.bob.ecommerceangularapp.dto;

/** Body for assigning (or, with a null {@code planId}, unassigning) a tenant's billing plan (roadmap #22). */
public record AssignPlanRequest(Long planId) {
}
