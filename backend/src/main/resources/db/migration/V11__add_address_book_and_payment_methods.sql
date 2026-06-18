-- V11: account address book + saved payment methods. New tables only (no risk to existing tables).
-- saved_payment_method stores only Stripe references + display metadata, never raw card data.
-- Matches the SavedAddress / SavedPaymentMethod entities (ddl-auto=validate checks on boot).

create table saved_address (
    default_address bit,
    id bigint not null auto_increment,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    label varchar(255),
    recipient_name varchar(255),
    state varchar(255),
    street varchar(255),
    zip_code varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_saved_address_email on saved_address (email);

create table saved_payment_method (
    default_method bit,
    exp_month integer,
    exp_year integer,
    id bigint not null auto_increment,
    brand varchar(255),
    email varchar(255),
    last4 varchar(255),
    stripe_payment_method_id varchar(255),
    primary key (id)
) engine=InnoDB;

create index idx_saved_pm_email on saved_payment_method (email);
