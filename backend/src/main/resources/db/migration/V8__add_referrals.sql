-- V8: referral program. A nullable+unique referral_code on customer (safe ALTER — existing rows stay
-- NULL, and MySQL allows multiple NULLs under a unique key) and a referral ledger. Matches the
-- Customer + Referral entities (ddl-auto=validate checks on boot).

alter table customer add column referral_code varchar(255);
alter table customer add constraint UK_customer_referral_code unique (referral_code);

create table referral (
    referee_points integer not null,
    referrer_points integer not null,
    date_created datetime(6) not null,
    id bigint not null auto_increment,
    order_id bigint,
    referee_email varchar(255),
    referrer_code varchar(255),
    status varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_referral_referrer on referral (referrer_code);
create index idx_referral_referee on referral (referee_email);
