package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.CsvImportResult;
import com.bob.ecommerceangularapp.dto.InventoryAdjustmentRequest;
import com.bob.ecommerceangularapp.dto.InventoryAdjustmentView;
import com.bob.ecommerceangularapp.dto.InventoryItemView;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SKU-level inventory management (roadmap #15): the merged product+variant stock view, CSV
 * export/import, and the per-SKU {@code InventoryAdjustment} ledger. Adjustments and imports are also
 * recorded to the cross-cutting global admin audit log (roadmap #19).
 */
@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    public AdminInventoryController(InventoryService inventoryService, AuditLogService auditLogService) {
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<InventoryItemView> list() {
        return inventoryService.list();
    }

    @GetMapping("/adjustments")
    public PageResponse<InventoryAdjustmentView> history(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(inventoryService.history(PageRequest.of(page, size)));
    }

    @PutMapping("/{sku}")
    public InventoryItemView adjust(Authentication authentication, @PathVariable String sku, @Valid @RequestBody InventoryAdjustmentRequest request) {
        InventoryItemView saved = inventoryService.adjust(sku, request.quantity(), request.note());
        auditLogService.record(authentication, "INVENTORY_ADJUST", "InventoryItem", sku, "delta=" + request.quantity());
        return saved;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] csv = inventoryService.exportCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("inventory.csv").build().toString())
                .body(csv);
    }

    @PostMapping("/import")
    public CsvImportResult importCsv(Authentication authentication, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Choose a CSV file to upload.");
        }
        try {
            CsvImportResult result = inventoryService.importCsv(file.getInputStream());
            auditLogService.record(authentication, "INVENTORY_CSV_IMPORT", "InventoryItem", null,
                    result.updated() + " updated, " + result.errors().size() + " error(s)");
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded CSV file.", e);
        }
    }
}
