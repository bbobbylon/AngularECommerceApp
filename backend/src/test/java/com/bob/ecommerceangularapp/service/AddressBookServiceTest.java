package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.SavedAddressRepository;
import com.bob.ecommerceangularapp.dto.SavedAddressRequest;
import com.bob.ecommerceangularapp.entity.SavedAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for the address book. */
class AddressBookServiceTest {

    private static final Long TENANT_ID = 7L;

    private final SavedAddressRepository repo = mock(SavedAddressRepository.class);
    private final AddressBookService service = new AddressBookService(repo);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    private SavedAddress addr(Long id, String email, boolean def) {
        SavedAddress a = new SavedAddress();
        a.setId(id);
        a.setEmail(email);
        a.setDefaultAddress(def);
        return a;
    }

    @Test
    void save_keepsExactlyOneDefault() {
        SavedAddress other = addr(2L, "a@b.com", true);
        when(repo.save(any())).thenAnswer(inv -> {
            SavedAddress a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(1L);
            }
            return a;
        });
        when(repo.findByEmailIgnoreCaseAndTenantIdOrderByDefaultAddressDescIdDesc("a@b.com", TENANT_ID))
                .thenReturn(List.of(addr(1L, "a@b.com", true), other));

        SavedAddress saved = service.save("a@b.com",
                new SavedAddressRequest(null, "Home", "Me", "1 St", "Town", "CA", "US", "90210", true));

        assertThat(saved.isDefaultAddress()).isTrue();
        assertThat(other.isDefaultAddress()).isFalse(); // the previous default was cleared
    }

    @Test
    void delete_onlyOwnerCanDelete() {
        SavedAddress a = addr(5L, "a@b.com", false);
        when(repo.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(a));
        service.delete("a@b.com", 5L);
        verify(repo).delete(a);
    }

    @Test
    void delete_ignoresWrongOwner() {
        when(repo.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(addr(5L, "someone@else.com", false)));
        service.delete("a@b.com", 5L);
        verify(repo, never()).delete(any());
    }
}
