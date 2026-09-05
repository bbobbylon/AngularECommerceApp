package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.FaqEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Managed via ContentService + AdminContentController; not exposed by SDR. */
@RepositoryRestResource(exported = false)
public interface FaqEntryRepository extends JpaRepository<FaqEntry, Long> {

    List<FaqEntry> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<FaqEntry> findAllByOrderBySortOrderAscIdAsc();
}
