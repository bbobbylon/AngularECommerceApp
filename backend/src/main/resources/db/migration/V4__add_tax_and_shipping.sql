-- V4: tax rates + shipping methods, and the amounts they produce recorded on each order.
-- New tables (no risk to populated tables) + three NULLABLE columns on `orders` (safe ALTER under
-- MySQL strict mode). Generated to match the TaxRate/ShippingMethod entities + new Order columns
-- (ddl-auto=validate checks this on boot).

create table tax_rate (
    active bit,
    rate_percent decimal(38,2),
    id bigint not null auto_increment,
    country varchar(255),
    state varchar(255),
    primary key (id)
) engine=InnoDB;

create table shipping_method (
    active bit,
    sort_order integer,
    base_rate decimal(38,2),
    free_over_threshold decimal(38,2),
    id bigint not null auto_increment,
    code varchar(255),
    estimated_days varchar(255),
    name varchar(255),
    primary key (id)
) engine=InnoDB;

alter table shipping_method add constraint UK_shipping_method_code unique (code);

alter table orders add column shipping_amount decimal(38,2);
alter table orders add column tax_amount decimal(38,2);
alter table orders add column shipping_method varchar(255);
