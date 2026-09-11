package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.StockNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Back-in-stock subscriptions — served via the custom controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface StockNotificationRepository extends JpaRepository<StockNotification, Long> {

    boolean existsByEmailIgnoreCaseAndProductIdAndVariantSkuAndNotifiedFalse(
            String email, Long productId, String variantSku);

    List<StockNotification> findByProductIdAndVariantSkuIsNullAndNotifiedFalse(Long productId);

    List<StockNotification> findByVariantSkuAndNotifiedFalse(String variantSku);
}
