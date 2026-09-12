-- V23: webhook event outbox + delivery ledger (roadmap #23, Milestones C + D). webhook_event is a
-- mutable per-event-x-subscription unit (shaped like tenant_billing_account) that the delivery
-- scheduler advances through PENDING -> DELIVERED/FAILED -> (eventually) ABANDONED;
-- webhook_delivery_attempt is an append-only ledger (shaped like billing_invoice) — one row per HTTP
-- attempt, never updated. Kept deliberately separate from audit_log_entry and billing_invoice, same
-- rationale as billing's own ledger being separate from the audit log.

create table webhook_event (
    id bigint not null auto_increment,
    tenant_id bigint not null,
    webhook_subscription_id bigint not null,
    event_type varchar(255) not null,
    payload longtext not null,
    status varchar(32) not null,
    attempt_count integer not null,
    next_attempt_at datetime(6),
    last_attempted_at datetime(6),
    date_created datetime(6),
    primary key (id),
    constraint fk_webhook_event_tenant foreign key (tenant_id) references tenant (id),
    constraint fk_webhook_event_subscription foreign key (webhook_subscription_id) references webhook_subscription (id)
) engine=InnoDB;

create index idx_webhook_event_tenant on webhook_event (tenant_id);
create index idx_webhook_event_subscription on webhook_event (webhook_subscription_id);
create index idx_webhook_event_due on webhook_event (status, next_attempt_at);

create table webhook_delivery_attempt (
    id bigint not null auto_increment,
    tenant_id bigint not null,
    webhook_event_id bigint not null,
    webhook_subscription_id bigint not null,
    event_type varchar(255) not null,
    attempt_number integer not null,
    outcome varchar(32) not null,
    http_status_code integer,
    error_message varchar(500),
    attempted_at datetime(6),
    primary key (id),
    constraint fk_webhook_delivery_attempt_tenant foreign key (tenant_id) references tenant (id),
    constraint fk_webhook_delivery_attempt_event foreign key (webhook_event_id) references webhook_event (id),
    constraint fk_webhook_delivery_attempt_subscription foreign key (webhook_subscription_id) references webhook_subscription (id)
) engine=InnoDB;

create index idx_webhook_delivery_attempt_tenant on webhook_delivery_attempt (tenant_id);
create index idx_webhook_delivery_attempt_event on webhook_delivery_attempt (webhook_event_id);
create index idx_webhook_delivery_attempt_subscription on webhook_delivery_attempt (webhook_subscription_id);
