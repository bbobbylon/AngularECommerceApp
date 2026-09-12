-- V22: webhook subscriptions (roadmap #23, Milestone B) — a tenant registers a target URL + the
-- event types it wants; secret is stored raw (not hashed), since this app must re-read it on every
-- delivery to sign the outbound request (symmetric to how Stripe's own dashboard stores/re-displays
-- webhook secrets — a hash would make delivery impossible).

create table webhook_subscription (
    id bigint not null auto_increment,
    tenant_id bigint not null,
    url varchar(2048) not null,
    secret varchar(255) not null,
    event_types varchar(500) not null,
    active bit not null,
    date_created datetime(6),
    primary key (id),
    constraint fk_webhook_subscription_tenant foreign key (tenant_id) references tenant (id)
) engine=InnoDB;

create index idx_webhook_subscription_tenant on webhook_subscription (tenant_id);
