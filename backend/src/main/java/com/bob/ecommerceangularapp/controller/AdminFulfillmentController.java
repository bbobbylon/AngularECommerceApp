package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.StockQuantity;
import com.bob.ecommerceangularapp.dto.WarehouseRequest;
import com.bob.ecommerceangularapp.dto.WarehouseStockRow;
import com.bob.ecommerceangularapp.entity.Warehouse;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.FulfillmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin management of warehouses + their per-SKU stock distribution (roadmap #20). Warehouse
 * configuration is a full-Admin concern (the {@code /api/admin/**} mutation catch-all applies);
 * the day-to-day shipping actions live in {@link AdminShipmentController}, which OrderManager
 * can also reach.
 */
@RestController
@RequestMapping("/api/admin/warehouses")
public class AdminFulfillmentController {

    private final FulfillmentService fulfillmentService;
    private final AuditLogService auditLogService;

    public AdminFulfillmentController(FulfillmentService fulfillmentService, AuditLogService auditLogService) {
        this.fulfillmentService = fulfillmentService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<Warehouse> list() {
        return fulfillmentService.listWarehouses();
    }

    @PostMapping
    public Warehouse save(Authentication authentication, @Valid @RequestBody WarehouseRequest request) {
        boolean isNew = request.id() == null;
        Warehouse saved = fulfillmentService.saveWarehouse(request);
        auditLogService.record(authentication, isNew ? "WAREHOUSE_CREATE" : "WAREHOUSE_UPDATE",
                "Warehouse", String.valueOf(saved.getId()), saved.getCode());
        return saved;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        fulfillmentService.deleteWarehouse(id);
        auditLogService.record(authentication, "WAREHOUSE_DELETE", "Warehouse", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stock")
    public List<WarehouseStockRow> stock(@PathVariable Long id) {
        return fulfillmentService.stockFor(id);
    }

    @PutMapping("/{id}/stock")
    public List<WarehouseStockRow> updateStock(Authentication authentication, @PathVariable Long id,
                                               @Valid @RequestBody List<StockQuantity> updates) {
        List<WarehouseStockRow> result = fulfillmentService.updateStock(id, updates);
        auditLogService.record(authentication, "WAREHOUSE_STOCK_UPDATE", "Warehouse", String.valueOf(id),
                updates.size() + " SKU(s)");
        return result;
    }
}
