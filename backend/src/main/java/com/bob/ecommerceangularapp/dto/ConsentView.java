package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.ConsentRecord;

import java.util.Date;

/** One recorded consent decision, as returned to the storefront and included in a data export. */
public record ConsentView(
        Long id,
        boolean necessary,
        boolean functional,
        boolean analytics,
        boolean marketing,
        String policyVersion,
        String source,
        Date dateCreated) {

    public static ConsentView from(ConsentRecord record) {
        return new ConsentView(record.getId(), record.isNecessary(), record.isFunctional(),
                record.isAnalytics(), record.isMarketing(), record.getPolicyVersion(),
                record.getSource(), record.getDateCreated());
    }
}
