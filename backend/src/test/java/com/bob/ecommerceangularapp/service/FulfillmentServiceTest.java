package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ShipmentRepository;
import com.bob.ecommerceangularapp.dao.WarehouseRepository;
import com.bob.ecommerceangularapp.dao.WarehouseStockRepository;
import com.bob.ecommerceangularapp.dto.CreateShipmentRequest;
import com.bob.ecommerceangularapp.dto.FulfillmentOption;
import com.bob.ecommerceangularapp.dto.ShipmentView;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.Shipment;
import com.bob.ecommerceangularapp.entity.Warehouse;
import com.bob.ecommerceangularapp.entity.WarehouseStock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for fulfillment: shipment lifecycle, stock draw-down, coverage ranking. */
class FulfillmentServiceTest {

    private final WarehouseRepository warehouseRepo = mock(WarehouseRepository.class);
    private final WarehouseStockRepository stockRepo = mock(WarehouseStockRepository.class);
    private final ShipmentRepository shipmentRepo = mock(ShipmentRepository.class);
    private final OrderRepository orderRepo = mock(OrderRepository.class);
    private final ProductRepository productRepo = mock(ProductRepository.class);
    private final InventoryService inventoryService = mock(InventoryService.class);

    private final FulfillmentService service = new FulfillmentService(
            warehouseRepo, stockRepo, shipmentRepo, orderRepo, productRepo, inventoryService);

    private Warehouse warehouse(Long id, String code, boolean active) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setCode(code);
        w.setName(code + " warehouse");
        w.setActive(active);
        return w;
    }

    private Order order(Long id, String status, OrderItem... items) {
        Order order = new Order();
        order.setId(id);
        order.setOrderTrackingNumber("TRACK-" + id);
        order.setStatus(status);
        for (OrderItem item : items) {
            order.add(item);
        }
        return order;
    }

    private OrderItem line(Long productId, String variantSku, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setVariantSku(variantSku);
        item.setQuantity(quantity);
        return item;
    }

    private WarehouseStock stock(Warehouse w, String sku, int quantity) {
        WarehouseStock s = new WarehouseStock();
        s.setWarehouse(w);
        s.setSku(sku);
        s.setQuantity(quantity);
        return s;
    }

    @Test
    void createShipment_drawsDownWarehouseStockClampedAtZero() {
        Warehouse w = warehouse(1L, "ATL", true);
        Order order = order(10L, null, line(5L, "MUG-L", 3), line(6L, null, 2));
        Product p6 = new Product();
        p6.setSku("BOOK-1");
        WarehouseStock mugRow = stock(w, "MUG-L", 2);   // short pick: 2 on hand, 3 needed
        WarehouseStock bookRow = stock(w, "BOOK-1", 9);

        when(orderRepo.findById(10L)).thenReturn(Optional.of(order));
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(w));
        when(productRepo.findById(6L)).thenReturn(Optional.of(p6));
        when(stockRepo.findByWarehouseIdAndSku(1L, "MUG-L")).thenReturn(Optional.of(mugRow));
        when(stockRepo.findByWarehouseIdAndSku(1L, "BOOK-1")).thenReturn(Optional.of(bookRow));
        when(shipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShipmentView view = service.createShipment(10L, new CreateShipmentRequest(1L, null, null, null));

        assertThat(mugRow.getQuantity()).isZero();        // clamped, not negative
        assertThat(bookRow.getQuantity()).isEqualTo(7);
        assertThat(view.status()).isEqualTo("PENDING");   // no carrier yet
        assertThat(order.getStatus()).isEqualTo("Processing");
    }

    @Test
    void createShipment_withCarrierGoesStraightToShippedAndSyncsOrder() {
        Warehouse w = warehouse(1L, "ATL", true);
        Order order = order(10L, "Received", line(5L, "MUG-L", 1));
        when(orderRepo.findById(10L)).thenReturn(Optional.of(order));
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(w));
        when(stockRepo.findByWarehouseIdAndSku(1L, "MUG-L")).thenReturn(Optional.empty());
        when(shipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShipmentView view = service.createShipment(10L, new CreateShipmentRequest(1L, "UPS", "1Z999", null));

        assertThat(view.status()).isEqualTo("SHIPPED");
        assertThat(view.shippedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("Shipped");
    }

    @Test
    void createShipment_rejectsInactiveWarehouse() {
        Order order = order(10L, null, line(5L, "MUG-L", 1));
        when(orderRepo.findById(10L)).thenReturn(Optional.of(order));
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(warehouse(1L, "OLD", false)));

        assertThatThrownBy(() -> service.createShipment(10L, new CreateShipmentRequest(1L, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive");
        verify(shipmentRepo, never()).save(any());
    }

    @Test
    void createShipment_neverDowngradesOrderStatusOrTouchesCancelled() {
        Warehouse w = warehouse(1L, "ATL", true);
        when(warehouseRepo.findById(1L)).thenReturn(Optional.of(w));
        when(stockRepo.findByWarehouseIdAndSku(any(), any())).thenReturn(Optional.empty());
        when(shipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order delivered = order(10L, "Delivered", line(5L, "MUG-L", 1));
        when(orderRepo.findById(10L)).thenReturn(Optional.of(delivered));
        service.createShipment(10L, new CreateShipmentRequest(1L, null, null, null));
        assertThat(delivered.getStatus()).isEqualTo("Delivered"); // not dragged back to Processing

        Order cancelled = order(11L, "Cancelled", line(5L, "MUG-L", 1));
        when(orderRepo.findById(11L)).thenReturn(Optional.of(cancelled));
        service.createShipment(11L, new CreateShipmentRequest(1L, "UPS", "1Z", null));
        assertThat(cancelled.getStatus()).isEqualTo("Cancelled"); // off the ladder — untouched
    }

    @Test
    void updateShipmentStatus_movesForwardSetsTimestampsAndSyncsOrder() {
        Shipment shipment = new Shipment();
        shipment.setId(7L);
        shipment.setOrderId(10L);
        shipment.setStatus("PENDING");
        Order order = order(10L, "Processing", line(5L, "MUG-L", 1));
        when(shipmentRepo.findById(7L)).thenReturn(Optional.of(shipment));
        when(orderRepo.findById(10L)).thenReturn(Optional.of(order));
        when(shipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShipmentView shipped = service.updateShipmentStatus(7L, "SHIPPED", "FedEx", "FX123");
        assertThat(shipped.status()).isEqualTo("SHIPPED");
        assertThat(shipped.carrier()).isEqualTo("FedEx");
        assertThat(shipped.shippedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("Shipped");

        ShipmentView deliveredView = service.updateShipmentStatus(7L, "DELIVERED", null, null);
        assertThat(deliveredView.deliveredAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("Delivered");
    }

    @Test
    void updateShipmentStatus_rejectsBackwardOrUnknownMoves() {
        Shipment shipment = new Shipment();
        shipment.setId(7L);
        shipment.setOrderId(10L);
        shipment.setStatus("SHIPPED");
        when(shipmentRepo.findById(7L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.updateShipmentStatus(7L, "PENDING", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateShipmentStatus(7L, "TELEPORTED", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateShipmentStatus(7L, "SHIPPED", null, null))
                .isInstanceOf(IllegalArgumentException.class); // no self-transition
    }

    @Test
    void fulfillmentOptions_ranksFullCoverageFirst() {
        Order order = order(10L, null, line(5L, "MUG-L", 2), line(6L, "MUG-S", 1));
        Warehouse partial = warehouse(1L, "P", true);   // priority 0 but can't cover everything
        Warehouse full = warehouse(2L, "F", true);
        full.setPriority(1);
        Warehouse inactive = warehouse(3L, "X", false);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(order));
        when(warehouseRepo.findAllByOrderByPriorityAscNameAsc()).thenReturn(List.of(partial, full, inactive));
        when(stockRepo.findByWarehouseId(1L)).thenReturn(List.of(stock(partial, "MUG-L", 5)));
        when(stockRepo.findByWarehouseId(2L)).thenReturn(List.of(stock(full, "MUG-L", 2), stock(full, "MUG-S", 1)));

        List<FulfillmentOption> options = service.fulfillmentOptions(10L);

        assertThat(options).hasSize(2); // inactive warehouse excluded
        assertThat(options.get(0).code()).isEqualTo("F");
        assertThat(options.get(0).fullCoverage()).isTrue();
        assertThat(options.get(1).coveredLines()).isEqualTo(1);
    }

    @Test
    void trackShipments_requiresMatchingEmail() {
        Customer customer = new Customer();
        customer.setEmail("real@example.com");
        Order order = order(10L, null);
        order.setCustomer(customer);
        when(orderRepo.findByOrderTrackingNumber("TRACK-10")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.trackShipments("TRACK-10", "wrong@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doesn't match");

        when(shipmentRepo.findByOrderIdOrderByDateCreatedDesc(10L)).thenReturn(List.of());
        assertThat(service.trackShipments("TRACK-10", "REAL@example.com")).isEmpty(); // case-insensitive
    }

    @Test
    void deleteWarehouse_refusesWhenShipmentsReferenceIt() {
        when(shipmentRepo.existsByWarehouseId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.deleteWarehouse(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deactivate");
        verify(warehouseRepo, never()).deleteById(any());
    }
}
