-- V2: secondary indexes on hot lookup/sort columns. Kept out of V1 so they apply to BOTH fresh
-- databases (V1 then V2) and existing databases baselined at V1 (which run V2 on next startup) —
-- without a duplicate-index clash. Mirrors the @Index annotations on the entities.

create index idx_customer_email on customer (email);
create index idx_orders_date_created on orders (date_created);
create index idx_product_active_category on product (active, category_id);
create index idx_review_product on review (product_id);
