-- V9: back-in-stock notifications. New table only (no risk to existing tables). Matches the
-- StockNotification entity (ddl-auto=validate checks on boot).

create table stock_notification (
    notified bit,
    date_created datetime(6) not null,
    id bigint not null auto_increment,
    product_id bigint,
    email varchar(255),
    variant_sku varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_stock_notification_product on stock_notification (product_id);
