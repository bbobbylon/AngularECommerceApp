package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ShipmentRepository;
import com.bob.ecommerceangularapp.dao.WarehouseRepository;
import com.bob.ecommerceangularapp.dao.WarehouseStockRepository;
import com.bob.ecommerceangularapp.dto.CreateShipmentRequest;
import com.bob.ecommerceangularapp.dto.FulfillmentOption;
import com.bob.ecommerceangularapp.dto.ShipmentView;
import com.bob.ecommerceangularapp.dto.StockQuantity;
import com.bob.ecommerceangularapp.dto.WarehouseRequest;
import com.bob.ecommerceangularapp.dto.WarehouseStockRow;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.Shipment;
import com.bob.ecommerceangularapp.entity.Warehouse;
import com.bob.ecommerceangularapp.entity.WarehouseStock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fulfillment + multi-warehouse (roadmap #20). Warehouses hold a per-SKU stock distribution that
 * shipments draw down from when an order is fulfilled; the product/variant {@code unitsInStock}
 * remains the sellable total checkout decrements (see {@link ProductVariantService}) — the two
 * ledgers answer different questions ("can we sell it" vs. "which building ships it"), mirroring
 * how the audit log and inventory-adjustment ledgers stay separate.
 *
 * <p>Shipment lifecycle: {@code PENDING} → {@code SHIPPED} → {@code DELIVERED}, forward-only.
 * Creating a shipment draws down warehouse stock (clamped at zero, like the checkout variant
 * decrement) and nudges the order's fulfillment status forward ("Processing" on creation, "Shipped"/
 * "Delivered" as the shipment progresses) — never backward, and never off "Cancelled".
 */
@Service
public class FulfillmentService {

    static final String PENDING = "PENDING";
    static final String SHIPPED = "SHIPPED";
    static final String DELIVERED = "DELIVERED";

    /** Order-status ladder for forward-only sync; "Cancelled" is deliberately absent (never touched). */
    private static final List<String> ORDER_STATUS_LADDER = List.of("Received", "Processing", "Shipped", "Delivered");

    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository stockRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public FulfillmentService(WarehouseRepository warehouseRepository,
                              WarehouseStockRepository stockRepository,
                              ShipmentRepository shipmentRepository,
                              OrderRepository orderRepository,
                              ProductRepository productRepository,
                              InventoryService inventoryService) {
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    // ---------- warehouses ----------

    @Transactional(readOnly = true)
    public List<Warehouse> listWarehouses() {
        return warehouseRepository.findAllByOrderByPriorityAscNameAsc();
    }

    @Transactional
    public Warehouse saveWarehouse(WarehouseRequest request) {
        Warehouse warehouse = request.id() == null
                ? new Warehouse()
                : warehouseRepository.findById(request.id())
                        .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + request.id()));
        warehouse.setCode(request.code().trim().toUpperCase());
        warehouse.setName(request.name().trim());
        warehouse.setCity(blankToNull(request.city()));
        warehouse.setState(blankToNull(request.state()));
        warehouse.setCountry(blankToNull(request.country()));
        warehouse.setPriority(request.priority() == null ? 0 : request.priority());
        warehouse.setActive(request.active() == null || request.active());
        return warehouseRepository.save(warehouse);
    }

    /** Shipments keep their warehouse for history, so a warehouse that has ever shipped can only be deactivated. */
    @Transactional
    public void deleteWarehouse(Long id) {
        if (shipmentRepository.existsByWarehouseId(id)) {
            throw new IllegalArgumentException("This warehouse has shipments; deactivate it instead of deleting.");
        }
        stockRepository.deleteByWarehouseId(id);
        warehouseRepository.deleteById(id);
    }

    // ---------- warehouse stock ----------

    /**
     * Every sellable SKU (the roadmap-#15 merged product/variant view) with this warehouse's
     * quantity — zero rows included so the admin can distribute stock to a new warehouse in place.
     */
    @Transactional(readOnly = true)
    public List<WarehouseStockRow> stockFor(Long warehouseId) {
        requireWarehouse(warehouseId);
        Map<String, Integer> quantities = new LinkedHashMap<>();
        stockRepository.findByWarehouseId(warehouseId)
                .forEach(s -> quantities.put(s.getSku(), s.getQuantity()));
        return inventoryService.list().stream()
                .map(item -> new WarehouseStockRow(item.sku(), item.productName(), item.variantLabel(),
                        quantities.getOrDefault(item.sku(), 0)))
                .toList();
    }

    @Transactional
    public List<WarehouseStockRow> updateStock(Long warehouseId, List<StockQuantity> updates) {
        Warehouse warehouse = requireWarehouse(warehouseId);
        for (StockQuantity update : updates) {
            String sku = update.sku().trim();
            WarehouseStock row = stockRepository.findByWarehouseIdAndSku(warehouseId, sku)
                    .orElseGet(() -> {
                        WarehouseStock s = new WarehouseStock();
                        s.setWarehouse(warehouse);
                        s.setSku(sku);
                        return s;
                    });
            row.setQuantity(Math.max(0, update.quantity()));
            stockRepository.save(row);
        }
        return stockFor(warehouseId);
    }

    // ---------- shipments ----------

    @Transactional(readOnly = true)
    public List<ShipmentView> shipmentsForOrder(Long orderId) {
        return shipmentRepository.findByOrderIdOrderByDateCreatedDesc(orderId).stream()
                .map(FulfillmentService::toView).toList();
    }

    /**
     * Customer-facing lookup, keyed like returns: the order's tracking number plus a matching email.
     * The mismatch message deliberately doesn't disclose whether the order exists.
     */
    @Transactional(readOnly = true)
    public List<ShipmentView> trackShipments(String orderTrackingNumber, String email) {
        Order order = orderRepository.findByOrderTrackingNumber(orderTrackingNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("That email doesn't match this order."));
        String orderEmail = order.getCustomer() == null ? null : order.getCustomer().getEmail();
        if (orderEmail == null || !orderEmail.equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("That email doesn't match this order.");
        }
        return shipmentsForOrder(order.getId());
    }

    /**
     * Ranks every active warehouse by how many of the order's lines it can fully cover from its
     * current stock (best first: full coverage, then covered-line count, then warehouse priority).
     */
    @Transactional(readOnly = true)
    public List<FulfillmentOption> fulfillmentOptions(Long orderId) {
        Order order = requireOrder(orderId);
        Map<String, Integer> needed = neededBySku(order);
        List<FulfillmentOption> options = new ArrayList<>();
        for (Warehouse warehouse : warehouseRepository.findAllByOrderByPriorityAscNameAsc()) {
            if (!warehouse.isActive()) {
                continue;
            }
            Map<String, Integer> held = new LinkedHashMap<>();
            stockRepository.findByWarehouseId(warehouse.getId())
                    .forEach(s -> held.put(s.getSku(), s.getQuantity()));
            int covered = (int) needed.entrySet().stream()
                    .filter(e -> held.getOrDefault(e.getKey(), 0) >= e.getValue())
                    .count();
            options.add(new FulfillmentOption(warehouse.getId(), warehouse.getCode(), warehouse.getName(),
                    needed.size(), covered, covered == needed.size() && !needed.isEmpty()));
        }
        options.sort(Comparator.comparing(FulfillmentOption::fullCoverage, Comparator.reverseOrder())
                .thenComparing(FulfillmentOption::coveredLines, Comparator.reverseOrder()));
        return options;
    }

    /**
     * Fulfills an order from a warehouse: draws down the warehouse's stock for every order line
     * (clamped at zero — a short pick doesn't fail the shipment, matching the checkout decrement's
     * clamp) and creates the shipment. With a carrier or tracking number already supplied it goes
     * straight to {@code SHIPPED}; otherwise it starts {@code PENDING} (picked/packed, not yet
     * handed to a carrier). The order's status is nudged forward accordingly.
     */
    @Transactional
    public ShipmentView createShipment(Long orderId, CreateShipmentRequest request) {
        Order order = requireOrder(orderId);
        Warehouse warehouse = requireWarehouse(request.warehouseId());
        if (!warehouse.isActive()) {
            throw new IllegalArgumentException("Warehouse " + warehouse.getCode() + " is inactive.");
        }

        neededBySku(order).forEach((sku, quantity) ->
                stockRepository.findByWarehouseIdAndSku(warehouse.getId(), sku).ifPresent(row -> {
                    row.setQuantity(Math.max(0, row.getQuantity() - quantity));
                    stockRepository.save(row);
                }));

        Shipment shipment = new Shipment();
        shipment.setOrderId(order.getId());
        shipment.setOrderTrackingNumber(order.getOrderTrackingNumber());
        shipment.setWarehouse(warehouse);
        shipment.setCarrier(blankToNull(request.carrier()));
        shipment.setTrackingNumber(blankToNull(request.trackingNumber()));
        shipment.setNote(blankToNull(request.note()));

        boolean handedToCarrier = shipment.getCarrier() != null || shipment.getTrackingNumber() != null;
        if (handedToCarrier) {
            shipment.setStatus(SHIPPED);
            shipment.setShippedAt(new Date());
            advanceOrderStatus(order, "Shipped");
        } else {
            shipment.setStatus(PENDING);
            advanceOrderStatus(order, "Processing");
        }
        return toView(shipmentRepository.save(shipment));
    }

    /** Moves a shipment forward (PENDING → SHIPPED → DELIVERED); carrier/tracking can be filled in here. */
    @Transactional
    public ShipmentView updateShipmentStatus(Long shipmentId, String status, String carrier, String trackingNumber) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

        String target = status == null ? "" : status.trim().toUpperCase();
        int currentRank = shipmentRank(shipment.getStatus());
        int targetRank = shipmentRank(target);
        if (targetRank < 0 || target.equals(PENDING)) {
            throw new IllegalArgumentException("Unknown shipment status: " + status);
        }
        if (targetRank <= currentRank) {
            throw new IllegalArgumentException("A shipment can only move forward (currently "
                    + shipment.getStatus() + ").");
        }

        if (blankToNull(carrier) != null) {
            shipment.setCarrier(carrier.trim());
        }
        if (blankToNull(trackingNumber) != null) {
            shipment.setTrackingNumber(trackingNumber.trim());
        }
        shipment.setStatus(target);
        if (SHIPPED.equals(target)) {
            shipment.setShippedAt(new Date());
        } else {
            if (shipment.getShippedAt() == null) {
                shipment.setShippedAt(new Date());
            }
            shipment.setDeliveredAt(new Date());
        }

        orderRepository.findById(shipment.getOrderId()).ifPresent(order ->
                advanceOrderStatus(order, SHIPPED.equals(target) ? "Shipped" : "Delivered"));
        return toView(shipmentRepository.save(shipment));
    }

    // ---------- helpers ----------

    /** Each order line's fulfillment SKU: the variant SKU when bought by variant, else the product's own. */
    private Map<String, Integer> neededBySku(Order order) {
        Map<String, Integer> needed = new LinkedHashMap<>();
        for (OrderItem item : order.getOrderItems()) {
            String sku = item.getVariantSku();
            if (sku == null || sku.isBlank()) {
                sku = productRepository.findById(item.getProductId()).map(Product::getSku).orElse(null);
            }
            if (sku != null) {
                needed.merge(sku, item.getQuantity(), Integer::sum);
            }
        }
        return needed;
    }

    /** Forward-only order-status sync; never downgrades and never touches a status off the ladder (e.g. Cancelled). */
    private void advanceOrderStatus(Order order, String target) {
        String current = order.getStatus();
        int currentRank = current == null || current.isBlank() ? -1 : ladderRank(current);
        if (current != null && !current.isBlank() && currentRank < 0) {
            return; // Cancelled or a custom status an admin set by hand — leave it alone.
        }
        if (ladderRank(target) > currentRank) {
            order.setStatus(target);
            orderRepository.save(order);
        }
    }

    private static int ladderRank(String status) {
        for (int i = 0; i < ORDER_STATUS_LADDER.size(); i++) {
            if (ORDER_STATUS_LADDER.get(i).equalsIgnoreCase(status)) {
                return i;
            }
        }
        return -1;
    }

    private static int shipmentRank(String status) {
        return switch (status == null ? "" : status) {
            case PENDING -> 0;
            case SHIPPED -> 1;
            case DELIVERED -> 2;
            default -> -1;
        };
    }

    private Warehouse requireWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id));
    }

    private Order requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ShipmentView toView(Shipment s) {
        Warehouse w = s.getWarehouse();
        return new ShipmentView(s.getId(), s.getOrderId(), s.getOrderTrackingNumber(),
                w == null ? null : w.getId(), w == null ? null : w.getCode(), w == null ? null : w.getName(),
                s.getCarrier(), s.getTrackingNumber(), s.getStatus(),
                s.getShippedAt(), s.getDeliveredAt(), s.getNote(), s.getDateCreated());
    }
}
