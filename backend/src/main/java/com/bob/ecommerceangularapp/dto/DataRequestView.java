package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.DataRequest;

import java.util.Date;

/**
 * A data-subject request as shown back to the requester. Deliberately omits {@code token} — that
 * value is the identity check, and echoing it into an API response would defeat the point of
 * mailing it.
 */
public record DataRequestView(
        Long id,
        String requestType,
        String status,
        String resultSummary,
        Date dateCreated,
        Date completedAt) {

    public static DataRequestView from(DataRequest request) {
        return new DataRequestView(request.getId(), request.getRequestType(), request.getStatus(),
                request.getResultSummary(), request.getDateCreated(), request.getCompletedAt());
    }
}
