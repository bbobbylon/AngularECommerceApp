package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Platform-level billing-plan catalog (roadmap #22) — served via the platform controller, not SDR. */
@RepositoryRestResource(exported = false)
public interface BillingPlanRepository extends JpaRepository<BillingPlan, Long> {

    List<BillingPlan> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
