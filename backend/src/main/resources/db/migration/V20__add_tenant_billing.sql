-- V20: tenant billing (roadmap #22) — fulfills the reservation Tenant.plan's javadoc made back in V17.
-- New billing_plan (platform-level catalog, like tenant itself), tenant_billing_account (one row per
-- tenant once assigned a plan; card-on-file stores only Stripe refs + display metadata, never raw card
-- data, same PCI shape as saved_payment_method), and billing_invoice (append-only ledger, one row per
-- billing attempt, snapshotting plan name/price at charge time so a later price change never rewrites
-- history).
--
-- No Stripe Product/Price/Subscription objects and no webhook are introduced here (deliberate — see
-- BillingService javadoc): this app's own tables are the source of truth for plan/status/renewal date,
-- and MonthlyBillingScheduler drives the actual charge via a plain off-session PaymentIntent.

create table billing_plan (
    id bigint not null auto_increment,
    name varchar(255) not null,
    monthly_price decimal(38,2) not null,
    currency varchar(3) not null,
    features varchar(2000),
    active bit not null,
    sort_order integer not null,
    date_created datetime(6),
    primary key (id),
    constraint uk_billing_plan_name unique (name)
) engine=InnoDB;

create table tenant_billing_account (
    id bigint not null auto_increment,
    tenant_id bigint not null,
    plan_id bigint,
    status varchar(32) not null,
    stripe_payment_method_id varchar(255),
    card_brand varchar(255),
    card_last4 varchar(255),
    card_exp_month integer,
    card_exp_year integer,
    current_period_end datetime(6),
    last_billed_at datetime(6),
    date_created datetime(6),
    primary key (id),
    constraint uk_billing_account_tenant unique (tenant_id),
    constraint fk_billing_account_tenant foreign key (tenant_id) references tenant (id),
    constraint fk_billing_account_plan foreign key (plan_id) references billing_plan (id)
) engine=InnoDB;

create table billing_invoice (
    id bigint not null auto_increment,
    tenant_id bigint not null,
    billing_account_id bigint not null,
    plan_name_snapshot varchar(255),
    amount decimal(38,2),
    currency varchar(3),
    status varchar(32) not null,
    failure_reason varchar(500),
    stripe_payment_intent_id varchar(255),
    attempted_at datetime(6),
    primary key (id),
    constraint fk_billing_invoice_tenant foreign key (tenant_id) references tenant (id),
    constraint fk_billing_invoice_account foreign key (billing_account_id) references tenant_billing_account (id)
) engine=InnoDB;

create index idx_billing_invoice_tenant on billing_invoice (tenant_id);
create index idx_billing_invoice_account on billing_invoice (billing_account_id);

-- Retire the old free-text tenant.plan column, first migrating any real value into the new catalog.
-- On this repo's own dev DB this backfill is a no-op (the seeded demo tenant has never had plan set),
-- but it must still be correct for a real deployment where a superadmin already typed something into
-- the old field. The migrated monthly_price is a 0.00 placeholder (free text carried no price) — a
-- superadmin should revisit it via /platform/billing-plans after upgrading.
insert into billing_plan (name, monthly_price, currency, active, sort_order, date_created)
select distinct t.plan, 0.00, 'USD', true, 0, now()
from tenant t
where t.plan is not null and t.plan not in (select name from billing_plan);

insert into tenant_billing_account (tenant_id, plan_id, status, current_period_end, date_created)
select t.id, bp.id, 'ACTIVE', date_add(now(), interval 1 month), now()
from tenant t join billing_plan bp on bp.name = t.plan
where t.plan is not null;

alter table tenant drop column plan;
