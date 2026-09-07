-- V19: multi-tenancy, Milestone D (roadmap #21) — extends tenant_id to the remaining customer-keyed and
-- ops entities that Milestones A-C left global: review, wishlist_item, newsletter_subscriber,
-- loyalty_transaction, referral, return_request, stock_notification, abandoned_cart, saved_address,
-- saved_payment_method, inventory_adjustment, audit_log_entry, warehouse, shipment. Same shape as V17/V18
-- (nullable tenant_id + FK + index, backfilled to the demo tenant).
--
-- Deliberately NOT scoped:
--   * country / state — global reference geo data shared by every storefront.
--   * warehouse_stock — reached only through its (now tenant-scoped) warehouse FK; uk_warehouse_stock
--     (warehouse_id, sku) already implies the tenant, so a second column would be redundant.
--
-- Unique constraints reworked so two tenants can share a value without colliding:
--   * newsletter_subscriber.email  -> (tenant_id, email). unsubscribe_token stays globally unique (random UUID).
--   * warehouse.code               -> (tenant_id, code).
--   * wishlist_item (email, product_id) is left alone — product_id already belongs to exactly one tenant.
--
-- audit_log_entry.tenant_id is intentionally allowed to stay NULL for rows written by the platform tier
-- (/api/platform/** runs outside tenant resolution); tenant back offices only ever list their own rows.

alter table review add column tenant_id bigint;
alter table review add constraint fk_review_tenant foreign key (tenant_id) references tenant (id);
create index idx_review_tenant on review (tenant_id);
update review set tenant_id = (select id from tenant where slug = 'demo');

alter table wishlist_item add column tenant_id bigint;
alter table wishlist_item add constraint fk_wishlist_item_tenant foreign key (tenant_id) references tenant (id);
create index idx_wishlist_item_tenant on wishlist_item (tenant_id);
update wishlist_item set tenant_id = (select id from tenant where slug = 'demo');

alter table newsletter_subscriber add column tenant_id bigint;
alter table newsletter_subscriber add constraint fk_newsletter_subscriber_tenant foreign key (tenant_id) references tenant (id);
create index idx_newsletter_subscriber_tenant on newsletter_subscriber (tenant_id);
update newsletter_subscriber set tenant_id = (select id from tenant where slug = 'demo');
alter table newsletter_subscriber drop index UKjmyiin4onxy5rh5bskafkxrgl;
alter table newsletter_subscriber add constraint uk_newsletter_subscriber_tenant_email unique (tenant_id, email);

alter table loyalty_transaction add column tenant_id bigint;
alter table loyalty_transaction add constraint fk_loyalty_transaction_tenant foreign key (tenant_id) references tenant (id);
create index idx_loyalty_transaction_tenant on loyalty_transaction (tenant_id);
update loyalty_transaction set tenant_id = (select id from tenant where slug = 'demo');

alter table referral add column tenant_id bigint;
alter table referral add constraint fk_referral_tenant foreign key (tenant_id) references tenant (id);
create index idx_referral_tenant on referral (tenant_id);
update referral set tenant_id = (select id from tenant where slug = 'demo');

alter table return_request add column tenant_id bigint;
alter table return_request add constraint fk_return_request_tenant foreign key (tenant_id) references tenant (id);
create index idx_return_request_tenant on return_request (tenant_id);
update return_request set tenant_id = (select id from tenant where slug = 'demo');

alter table stock_notification add column tenant_id bigint;
alter table stock_notification add constraint fk_stock_notification_tenant foreign key (tenant_id) references tenant (id);
create index idx_stock_notification_tenant on stock_notification (tenant_id);
update stock_notification set tenant_id = (select id from tenant where slug = 'demo');

alter table abandoned_cart add column tenant_id bigint;
alter table abandoned_cart add constraint fk_abandoned_cart_tenant foreign key (tenant_id) references tenant (id);
create index idx_abandoned_cart_tenant on abandoned_cart (tenant_id);
update abandoned_cart set tenant_id = (select id from tenant where slug = 'demo');

alter table saved_address add column tenant_id bigint;
alter table saved_address add constraint fk_saved_address_tenant foreign key (tenant_id) references tenant (id);
create index idx_saved_address_tenant on saved_address (tenant_id);
update saved_address set tenant_id = (select id from tenant where slug = 'demo');

alter table saved_payment_method add column tenant_id bigint;
alter table saved_payment_method add constraint fk_saved_payment_method_tenant foreign key (tenant_id) references tenant (id);
create index idx_saved_payment_method_tenant on saved_payment_method (tenant_id);
update saved_payment_method set tenant_id = (select id from tenant where slug = 'demo');

alter table inventory_adjustment add column tenant_id bigint;
alter table inventory_adjustment add constraint fk_inventory_adjustment_tenant foreign key (tenant_id) references tenant (id);
create index idx_inventory_adjustment_tenant on inventory_adjustment (tenant_id);
update inventory_adjustment set tenant_id = (select id from tenant where slug = 'demo');

alter table audit_log_entry add column tenant_id bigint;
alter table audit_log_entry add constraint fk_audit_log_entry_tenant foreign key (tenant_id) references tenant (id);
create index idx_audit_log_entry_tenant on audit_log_entry (tenant_id);
update audit_log_entry set tenant_id = (select id from tenant where slug = 'demo');

alter table warehouse add column tenant_id bigint;
alter table warehouse add constraint fk_warehouse_tenant foreign key (tenant_id) references tenant (id);
create index idx_warehouse_tenant on warehouse (tenant_id);
update warehouse set tenant_id = (select id from tenant where slug = 'demo');
alter table warehouse drop index uk_warehouse_code;
alter table warehouse add constraint uk_warehouse_tenant_code unique (tenant_id, code);

alter table shipment add column tenant_id bigint;
alter table shipment add constraint fk_shipment_tenant foreign key (tenant_id) references tenant (id);
create index idx_shipment_tenant on shipment (tenant_id);
update shipment set tenant_id = (select id from tenant where slug = 'demo');
