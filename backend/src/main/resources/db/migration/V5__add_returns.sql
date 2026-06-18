-- V5: returns / RMA + refunds. New return_request table + a nullable payment_intent_id on orders
-- (so an approved return can issue a Stripe refund against the original charge). Adding a nullable
-- column to a populated table is safe under MySQL strict mode. Matches the ReturnRequest entity +
-- the new Order column (ddl-auto=validate checks this on boot).

create table return_request (
    id bigint not null auto_increment,
    order_id bigint,
    refund_amount decimal(38,2),
    date_created datetime(6) not null,
    date_updated datetime(6) not null,
    admin_note varchar(2000),
    customer_email varchar(255),
    order_tracking_number varchar(255),
    reason varchar(2000),
    status varchar(255),
    stripe_refund_id varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_return_request_order on return_request (order_id);

alter table orders add column payment_intent_id varchar(255);
