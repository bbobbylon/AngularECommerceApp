package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Tax rates are admin-managed config, not a public catalog resource — hidden from Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {

    List<TaxRate> findByActiveTrueAndTenantId(Long tenantId);

    List<TaxRate> findAllByTenantId(Long tenantId);

    Optional<TaxRate> findByIdAndTenantId(Long id, Long tenantId);
}
