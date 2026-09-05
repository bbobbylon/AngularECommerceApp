-- V13: promotions engine — automatic, no-code discounts scheduled over a date window. New table only,
-- plus two nullable columns on orders recording one when it was applied (safe ALTER on a populated
-- table). Matches the Promotion entity + Order.promotionName/promotionDiscount (ddl-auto=validate
-- checks on boot).

create table promotion (
    active bit not null,
    amount_off decimal(38,2),
    min_spend decimal(38,2),
    percent_off integer,
    ends_at date,
    starts_at date,
    id bigint not null auto_increment,
    description varchar(255),
    name varchar(255) not null,
    primary key (id)
) engine=InnoDB;

alter table orders add column promotion_name varchar(255);
alter table orders add column promotion_discount decimal(38,2);
