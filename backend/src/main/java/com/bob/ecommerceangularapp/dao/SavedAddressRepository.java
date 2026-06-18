package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Account address book — served via the custom account controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {

    List<SavedAddress> findByEmailIgnoreCaseOrderByDefaultAddressDescIdDesc(String email);
}
