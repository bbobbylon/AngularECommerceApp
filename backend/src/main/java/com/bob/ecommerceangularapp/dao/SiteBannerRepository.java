package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.SiteBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Managed via ContentService (singleton row, upserted) + AdminContentController; not exposed by SDR. */
@RepositoryRestResource(exported = false)
public interface SiteBannerRepository extends JpaRepository<SiteBanner, Long> {
}
