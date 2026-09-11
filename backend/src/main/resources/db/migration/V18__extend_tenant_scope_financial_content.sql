-- V18: multi-tenancy, Milestone C (roadmap #21) — extends tenant_id to the financial/storefront-config
-- entities that were fully global despite Milestone A/B's per-tenant checkout: coupon, promotion,
-- gift_card, tax_rate, shipping_method, site_banner, faq_entry. Same shape as V17 (nullable tenant_id +
-- FK + index, backfilled to the demo tenant). coupon/gift_card/shipping_method also had a single-column
-- unique constraint on code — replaced with a composite (tenant_id, code) constraint so two tenants can
-- both use the same code (e.g. "WELCOME10") without colliding.

alter table coupon add column tenant_id bigint;
alter table coupon add constraint fk_coupon_tenant foreign key (tenant_id) references tenant (id);
create index idx_coupon_tenant on coupon (tenant_id);
update coupon set tenant_id = (select id from tenant where slug = 'demo');
alter table coupon drop index UKbg4p9ontpj7adq7yr71h93sdn;
alter table coupon add constraint uk_coupon_tenant_code unique (tenant_id, code);

alter table promotion add column tenant_id bigint;
alter table promotion add constraint fk_promotion_tenant foreign key (tenant_id) references tenant (id);
create index idx_promotion_tenant on promotion (tenant_id);
update promotion set tenant_id = (select id from tenant where slug = 'demo');

alter table gift_card add column tenant_id bigint;
alter table gift_card add constraint fk_gift_card_tenant foreign key (tenant_id) references tenant (id);
create index idx_gift_card_tenant on gift_card (tenant_id);
update gift_card set tenant_id = (select id from tenant where slug = 'demo');
alter table gift_card drop index UK_gift_card_code;
alter table gift_card add constraint uk_gift_card_tenant_code unique (tenant_id, code);

alter table tax_rate add column tenant_id bigint;
alter table tax_rate add constraint fk_tax_rate_tenant foreign key (tenant_id) references tenant (id);
create index idx_tax_rate_tenant on tax_rate (tenant_id);
update tax_rate set tenant_id = (select id from tenant where slug = 'demo');

alter table shipping_method add column tenant_id bigint;
alter table shipping_method add constraint fk_shipping_method_tenant foreign key (tenant_id) references tenant (id);
create index idx_shipping_method_tenant on shipping_method (tenant_id);
update shipping_method set tenant_id = (select id from tenant where slug = 'demo');
alter table shipping_method drop index UK_shipping_method_code;
alter table shipping_method add constraint uk_shipping_method_tenant_code unique (tenant_id, code);

alter table site_banner add column tenant_id bigint;
alter table site_banner add constraint fk_site_banner_tenant foreign key (tenant_id) references tenant (id);
create index idx_site_banner_tenant on site_banner (tenant_id);
update site_banner set tenant_id = (select id from tenant where slug = 'demo');

alter table faq_entry add column tenant_id bigint;
alter table faq_entry add constraint fk_faq_entry_tenant foreign key (tenant_id) references tenant (id);
create index idx_faq_entry_tenant on faq_entry (tenant_id);
update faq_entry set tenant_id = (select id from tenant where slug = 'demo');
