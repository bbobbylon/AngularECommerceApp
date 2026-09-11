-- V6: gift cards / store credit. New gift_card table + two nullable columns on orders recording any
-- gift card applied to the order. Adding nullable columns to a populated table is safe under MySQL
-- strict mode. Matches the GiftCard entity + new Order columns (ddl-auto=validate checks on boot).

create table gift_card (
    active bit,
    balance decimal(38,2),
    initial_balance decimal(38,2),
    date_created datetime(6) not null,
    id bigint not null auto_increment,
    code varchar(255),
    recipient_email varchar(255),
    primary key (id)
) engine=InnoDB;

alter table gift_card add constraint UK_gift_card_code unique (code);

alter table orders add column gift_card_code varchar(255);
alter table orders add column gift_card_amount decimal(38,2);
