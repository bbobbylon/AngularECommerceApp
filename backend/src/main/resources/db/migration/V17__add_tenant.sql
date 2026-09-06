-- V17: multi-tenancy foundation (roadmap #21, Milestone A). New tenant table, seeded with the one
-- "demo" tenant that owns every existing row. Nullable tenant_id + a real FK (a deliberate departure
-- from this schema's mostly-FK-less convention, since this is the single most important isolation
-- boundary) added to the core catalog/checkout entities only: product, product_category,
-- product_variant, customer, orders, order_item, address. The remaining ~23 entities are extended in
-- a later migration (Milestone B) once the rest of the app is scoped. Matches the Tenant entity plus
-- the tenantId field added to each of the 7 entities above (ddl-auto=validate checks on boot).

create table tenant (
    id bigint not null auto_increment,
    slug varchar(63) not null,
    display_name varchar(255) not null,
    contact_email varchar(255),
    active bit not null,
    plan varchar(64),
    date_created datetime(6),
    primary key (id),
    constraint uk_tenant_slug unique (slug)
) engine=InnoDB;

insert into tenant (slug, display_name, contact_email, active, plan, date_created)
values ('demo', 'Luv2Shop', 'support@luv2shop.example', true, null, now());

alter table product add column tenant_id bigint;
alter table product add constraint fk_product_tenant foreign key (tenant_id) references tenant (id);
create index idx_product_tenant on product (tenant_id);
update product set tenant_id = (select id from tenant where slug = 'demo');

alter table product_category add column tenant_id bigint;
alter table product_category add constraint fk_product_category_tenant foreign key (tenant_id) references tenant (id);
create index idx_product_category_tenant on product_category (tenant_id);
update product_category set tenant_id = (select id from tenant where slug = 'demo');

alter table product_variant add column tenant_id bigint;
alter table product_variant add constraint fk_product_variant_tenant foreign key (tenant_id) references tenant (id);
create index idx_product_variant_tenant on product_variant (tenant_id);
update product_variant set tenant_id = (select id from tenant where slug = 'demo');

alter table customer add column tenant_id bigint;
alter table customer add constraint fk_customer_tenant foreign key (tenant_id) references tenant (id);
create index idx_customer_tenant on customer (tenant_id);
update customer set tenant_id = (select id from tenant where slug = 'demo');

alter table orders add column tenant_id bigint;
alter table orders add constraint fk_orders_tenant foreign key (tenant_id) references tenant (id);
create index idx_orders_tenant on orders (tenant_id);
update orders set tenant_id = (select id from tenant where slug = 'demo');

alter table order_item add column tenant_id bigint;
alter table order_item add constraint fk_order_item_tenant foreign key (tenant_id) references tenant (id);
create index idx_order_item_tenant on order_item (tenant_id);
update order_item set tenant_id = (select id from tenant where slug = 'demo');

alter table address add column tenant_id bigint;
alter table address add constraint fk_address_tenant foreign key (tenant_id) references tenant (id);
create index idx_address_tenant on address (tenant_id);
update address set tenant_id = (select id from tenant where slug = 'demo');
