-- V10: abandoned-cart recovery. New table only. Matches the AbandonedCart entity
-- (ddl-auto=validate checks on boot).

create table abandoned_cart (
    recovered bit,
    reminded bit,
    item_count integer,
    total decimal(38,2),
    date_created datetime(6) not null,
    last_updated datetime(6) not null,
    id bigint not null auto_increment,
    email varchar(255),
    summary varchar(1000),
    primary key (id)
) engine=InnoDB;

create index idx_abandoned_cart_email on abandoned_cart (email);
