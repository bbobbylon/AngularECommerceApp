-- V12: inventory adjustment audit log. New table only (no risk to existing tables). Matches the
-- InventoryAdjustment entity (ddl-auto=validate checks on boot). Product/variant stock columns
-- (units_in_stock) already exist since V1/V3 — this just records the history of changes to them.

create table inventory_adjustment (
    delta integer not null,
    new_quantity integer not null,
    previous_quantity integer not null,
    date_created datetime(6) not null,
    id bigint not null auto_increment,
    note varchar(500),
    product_name varchar(255),
    source varchar(255),
    sku varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create index idx_inventory_adjustment_sku on inventory_adjustment (sku);
