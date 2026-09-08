package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Read-only reference data (seeded once), exposed via Spring Data REST for checkout's country dropdown. */
@RepositoryRestResource(collectionResourceRel = "countries", path = "countries")
public interface CountryRepository extends JpaRepository<Country, Integer> {
}
