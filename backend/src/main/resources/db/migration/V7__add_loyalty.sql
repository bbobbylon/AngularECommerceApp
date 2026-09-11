-- V7: loyalty / rewards points. Nullable point columns on customer (safe ALTER on a populated table),
-- a loyalty_transaction ledger, and two nullable columns on orders recording points redeemed as store
-- credit. Matches the Customer/LoyaltyTransaction/Order changes (ddl-auto=validate checks on boot).

alter table customer add column loyalty_points integer;
alter table customer add column lifetime_points integer;

create table loyalty_transaction (
    points integer,
    date_created datetime(6) not null,
    id bigint not null auto_increment,
    order_id bigint,
    customer_email varchar(255),
    description varchar(255),
    type varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_loyalty_tx_email on loyalty_transaction (customer_email);

alter table orders add column loyalty_points_redeemed integer;
alter table orders add column loyalty_discount decimal(38,2);
