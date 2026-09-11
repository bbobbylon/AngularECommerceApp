import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

import {
  AdminOrderView,
  AdminService,
  CreateShipmentPayload,
  FulfillmentOption,
  Shipment,
} from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-admin-orders',
  imports: [CommonModule, FormsModule, NgbPaginationModule],
  templateUrl: './admin-orders.html',
})
export class AdminOrders implements OnInit {

  readonly orders = signal<AdminOrderView[]>([]);
  readonly loading = signal(true);

  pageNumber = 1;
  pageSize = 15;
  totalElements = 0;

  readonly statuses = ['Received', 'Processing', 'Shipped', 'Delivered', 'Cancelled'];

  // ----- fulfillment (roadmap #20) — only one order's panel is open at a time -----
  expandedOrderId: number | null = null;
  readonly fulfillmentLoading = signal(false);
  shipmentsByOrder: Record<number, Shipment[] | undefined> = {};
  optionsByOrder: Record<number, FulfillmentOption[] | undefined> = {};
  shipmentForm: CreateShipmentPayload = this.emptyShipmentForm();
  readonly creatingShipment = signal(false);
  readonly updatingShipmentId = signal<number | null>(null);
  carrierFor: Record<number, string> = {};
  trackingFor: Record<number, string> = {};

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getOrders(this.pageNumber - 1, this.pageSize).subscribe({
      next: res => {
        this.orders.set(res.content);
        this.totalElements = res.totalElements;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load orders.');
      },
    });
  }

  updateStatus(order: AdminOrderView, status: string): void {
    this.admin.updateOrderStatus(order.id, status).subscribe({
      next: updated => {
        this.orders.update(list => list.map(o => (o.id === updated.id ? updated : o)));
        this.toast.success(`Order #${order.id} → ${status}`);
      },
      error: () => this.toast.error('Could not update order status.'),
    });
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'Delivered': return 'bg-success-subtle text-success-emphasis';
      case 'Shipped': return 'bg-info-subtle text-info-emphasis';
      case 'Cancelled': return 'bg-danger-subtle text-danger-emphasis';
      case 'Processing': return 'bg-warning-subtle text-warning-emphasis';
      default: return 'bg-secondary-subtle text-secondary';
    }
  }

  // ----- fulfillment (roadmap #20) -----

  toggleFulfillment(order: AdminOrderView): void {
    if (this.expandedOrderId === order.id) {
      this.expandedOrderId = null;
      return;
    }
    this.expandedOrderId = order.id;
    this.shipmentForm = this.emptyShipmentForm();
    if (this.shipmentsByOrder[order.id]) {
      return; // already loaded once — reuse the cache
    }
    this.fulfillmentLoading.set(true);
    this.admin.getShipments(order.id).subscribe({
      next: shipments => { this.shipmentsByOrder[order.id] = shipments; this.fulfillmentLoading.set(false); },
      error: () => { this.fulfillmentLoading.set(false); this.toast.error('Could not load shipments.'); },
    });
    this.admin.getFulfillmentOptions(order.id).subscribe({
      next: options => {
        this.optionsByOrder[order.id] = options;
        if (options.length > 0) {
          this.shipmentForm.warehouseId = options[0].warehouseId; // best coverage first
        }
      },
      error: () => { /* non-fatal — the warehouse dropdown just won't have a suggested default */ },
    });
  }

  createShipment(order: AdminOrderView): void {
    if (!this.shipmentForm.warehouseId) {
      this.toast.error('Choose a warehouse to ship from.');
      return;
    }
    this.creatingShipment.set(true);
    this.admin.createShipment(order.id, this.shipmentForm).subscribe({
      next: shipment => {
        this.shipmentsByOrder[order.id] = [shipment, ...(this.shipmentsByOrder[order.id] ?? [])];
        this.shipmentForm = this.emptyShipmentForm();
        this.creatingShipment.set(false);
        this.toast.success(`Shipment created for order #${order.id}.`);
        this.admin.getFulfillmentOptions(order.id).subscribe(options => {
          this.optionsByOrder[order.id] = options;
          if (options.length > 0) {
            this.shipmentForm.warehouseId = options[0].warehouseId;
          }
        });
        this.load();
      },
      error: err => {
        this.creatingShipment.set(false);
        this.toast.error(err?.error?.message ?? 'Could not create the shipment.');
      },
    });
  }

  advanceShipment(order: AdminOrderView, shipment: Shipment, target: 'SHIPPED' | 'DELIVERED'): void {
    this.updatingShipmentId.set(shipment.id);
    this.admin.updateShipmentStatus(shipment.id, target, this.carrierFor[shipment.id], this.trackingFor[shipment.id]).subscribe({
      next: updated => {
        this.shipmentsByOrder[order.id] = (this.shipmentsByOrder[order.id] ?? []).map(s => s.id === updated.id ? updated : s);
        delete this.carrierFor[shipment.id];
        delete this.trackingFor[shipment.id];
        this.updatingShipmentId.set(null);
        this.toast.success(`Shipment #${updated.id} → ${target}`);
        this.load();
      },
      error: err => {
        this.updatingShipmentId.set(null);
        this.toast.error(err?.error?.message ?? 'Could not update the shipment.');
      },
    });
  }

  shipmentBadgeClass(status: string): string {
    switch (status) {
      case 'DELIVERED': return 'bg-success-subtle text-success-emphasis';
      case 'SHIPPED': return 'bg-info-subtle text-info-emphasis';
      default: return 'bg-warning-subtle text-warning-emphasis';
    }
  }

  private emptyShipmentForm(): CreateShipmentPayload {
    return { warehouseId: 0, carrier: '', trackingNumber: '', note: '' };
  }
}
