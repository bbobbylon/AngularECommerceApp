package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.SavedAddressRepository;
import com.bob.ecommerceangularapp.dto.SavedAddressRequest;
import com.bob.ecommerceangularapp.entity.SavedAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Account address book. Addresses are keyed by (and scoped to) the customer's email; one may be the
 * default, which the checkout pre-selects. Mutations validate the address belongs to the caller's
 * email so a guessed id can't touch someone else's address.
 */
@Service
public class AddressBookService {

    private final SavedAddressRepository repository;

    public AddressBookService(SavedAddressRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SavedAddress> list(String email) {
        return email == null ? List.of()
                : repository.findByEmailIgnoreCaseOrderByDefaultAddressDescIdDesc(email.trim());
    }

    @Transactional
    public SavedAddress save(String email, SavedAddressRequest request) {
        String owner = email.trim();
        SavedAddress address = request.id() != null
                ? repository.findById(request.id())
                        .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(owner))
                        .orElseThrow(() -> new IllegalArgumentException("Address not found"))
                : new SavedAddress();
        address.setEmail(owner);
        address.setLabel(blankToNull(request.label()));
        address.setRecipientName(blankToNull(request.recipientName()));
        address.setStreet(request.street().trim());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setCountry(request.country().trim());
        address.setZipCode(request.zipCode().trim());
        address.setDefaultAddress(request.defaultAddress());
        SavedAddress saved = repository.save(address);

        if (saved.isDefaultAddress()) {
            // exactly one default per customer
            for (SavedAddress other : repository.findByEmailIgnoreCaseOrderByDefaultAddressDescIdDesc(owner)) {
                if (!other.getId().equals(saved.getId()) && other.isDefaultAddress()) {
                    other.setDefaultAddress(false);
                    repository.save(other);
                }
            }
        }
        return saved;
    }

    @Transactional
    public void delete(String email, Long id) {
        repository.findById(id)
                .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(email.trim()))
                .ifPresent(repository::delete);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
