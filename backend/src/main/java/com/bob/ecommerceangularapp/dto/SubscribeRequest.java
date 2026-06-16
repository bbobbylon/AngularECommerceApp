package com.bob.ecommerceangularapp.dto;

/** Payload for the newsletter signup box. */
public record SubscribeRequest(String email, String name) {
}
