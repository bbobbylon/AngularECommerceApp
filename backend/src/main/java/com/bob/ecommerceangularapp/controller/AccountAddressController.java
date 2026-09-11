package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.SavedAddressRequest;
import com.bob.ecommerceangularapp.entity.SavedAddress;
import com.bob.ecommerceangularapp.service.AddressBookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Account address book (gated under /api/account like the rest of the account API). */
@RestController
@RequestMapping("/api/account/addresses")
public class AccountAddressController {

    private final AddressBookService addressBookService;

    public AccountAddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @GetMapping
    public List<SavedAddress> list(@RequestParam String email) {
        return addressBookService.list(email);
    }

    @PostMapping
    public SavedAddress save(@RequestParam String email, @Valid @RequestBody SavedAddressRequest request) {
        return addressBookService.save(email, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestParam String email, @PathVariable Long id) {
        addressBookService.delete(email, id);
        return ResponseEntity.noContent().build();
    }
}
