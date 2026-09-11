package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Managed via CouponController (validate) + AdminCouponController (CRUD); not exposed by SDR. */
@RepositoryRestResource(exported = false)
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Coupon findByCodeIgnoreCaseAndTenantId(String code, Long tenantId);

    List<Coupon> findAllByTenantId(Long tenantId);

    Optional<Coupon> findByIdAndTenantId(Long id, Long tenantId);
}
