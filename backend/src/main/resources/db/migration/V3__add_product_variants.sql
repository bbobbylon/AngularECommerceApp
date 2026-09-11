-- V3: product variants (SKU-level inventory). New side table — never ALTERs the populated `product`
-- table — plus two NULLABLE columns on `order_item` so a placed order records which variant shipped.
-- Adding nullable columns is safe on a populated table under MySQL strict mode (no default needed).
-- Generated to match the ProductVariant entity + the new OrderItem columns (ddl-auto=validate checks).

create table product_variant (
    active bit,
    sort_order integer,
    units_in_stock integer,
    unit_price decimal(38,2),
    id bigint not null auto_increment,
    product_id bigint,
    image_url varchar(255),
    sku varchar(255),
    variant_color varchar(255),
    variant_size varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_product_variant_product on product_variant (product_id);
alter table product_variant add constraint UK_product_variant_sku unique (sku);
alter table product_variant add constraint FK_product_variant_product foreign key (product_id) references product (id);

alter table order_item add column variant_sku varchar(255);
alter table order_item add column variant_label varchar(255);
