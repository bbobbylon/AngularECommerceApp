package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "products", path = "products")
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryId(@Param("id") Long id, Pageable pageable);

    Page<Product> findByNameContaining(@Param("name") String name, Pageable pageable);

    /** On-sale products — anything with a pre-sale ("was") price set. Powers the /sale page. */
    Page<Product> findByOriginalPriceNotNull(Pageable pageable);

    // ----- admin dashboard metrics -----
    long countByActiveTrue();

    long countByUnitsInStockLessThan(int threshold);

    long countByOriginalPriceNotNull();
}
