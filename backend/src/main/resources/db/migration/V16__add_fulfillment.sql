-- V16: fulfillment + multi-warehouse (roadmap #20). Three new tables, no ALTERs: warehouse
-- (fulfillment locations), warehouse_stock (per-warehouse SKU distribution — the product/variant
-- units_in_stock stays the authoritative sellable total), and shipment (an order's fulfillment from
-- a warehouse, PENDING -> SHIPPED -> DELIVERED). Matches the Warehouse/WarehouseStock/Shipment
-- entities (ddl-auto=validate checks on boot).

create table warehouse (
    id bigint not null auto_increment,
    code varchar(32) not null,
    name varchar(255) not null,
    city varchar(255),
    state varchar(255),
    country varchar(255),
    priority integer not null,
    active bit not null,
    date_created datetime(6),
    primary key (id),
    constraint uk_warehouse_code unique (code)
) engine=InnoDB;

create table warehouse_stock (
    id bigint not null auto_increment,
    warehouse_id bigint not null,
    sku varchar(64) not null,
    quantity integer not null,
    primary key (id),
    constraint uk_warehouse_stock unique (warehouse_id, sku),
    constraint fk_warehouse_stock_warehouse foreign key (warehouse_id) references warehouse (id)
) engine=InnoDB;

create table shipment (
    id bigint not null auto_increment,
    order_id bigint not null,
    order_tracking_number varchar(255),
    warehouse_id bigint,
    carrier varchar(64),
    tracking_number varchar(128),
    status varchar(32) not null,
    shipped_at datetime(6),
    delivered_at datetime(6),
    note varchar(500),
    date_created datetime(6),
    primary key (id),
    constraint fk_shipment_warehouse foreign key (warehouse_id) references warehouse (id)
) engine=InnoDB;

create index idx_shipment_order on shipment (order_id);
