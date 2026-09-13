-- V24: GDPR — cookie/storage consent + data-subject rights (roadmap #24).
--
-- consent_record is an APPEND-ONLY ledger (same idiom as audit_log_entry and billing_invoice): one
-- row per consent decision, never updated in place. GDPR Art. 7(1) requires being able to
-- demonstrate *that* consent was given and *what* was consented to at the time — a mutable
-- current-state row cannot prove that, because the previous decision is gone.
--
-- Deliberately NOT stored: IP address and user-agent. Those are the conventional "proof of consent"
-- fields, but collecting more personal data to prove a privacy choice is self-defeating. visitor_id
-- is an opaque random id the browser generates for itself; email is filled in only when the visitor
-- is already known to us (i.e. we hold it anyway).
--
-- data_request backs the self-service export/erasure flow. The verification token is the same
-- email-proves-identity idiom customer.unsubscribe_token already established (M6) — the request is
-- worthless until someone proves they can read that mailbox, which is what stops one visitor
-- erasing another's data by typing their address.

create table consent_record (
    id bigint not null auto_increment,
    tenant_id bigint,
    visitor_id varchar(64) not null,
    email varchar(255),
    necessary bit not null,
    functional bit not null,
    analytics bit not null,
    marketing bit not null,
    policy_version varchar(32) not null,
    source varchar(32) not null,
    date_created datetime(6),
    primary key (id)
) engine=InnoDB;

create index idx_consent_record_visitor on consent_record (visitor_id);
create index idx_consent_record_email on consent_record (email);
create index idx_consent_record_tenant on consent_record (tenant_id);

create table data_request (
    id bigint not null auto_increment,
    tenant_id bigint,
    email varchar(255) not null,
    request_type varchar(16) not null,
    status varchar(32) not null,
    token varchar(64) not null,
    result_summary varchar(1000),
    expires_at datetime(6),
    verified_at datetime(6),
    completed_at datetime(6),
    date_created datetime(6),
    primary key (id),
    constraint uk_data_request_token unique (token)
) engine=InnoDB;

create index idx_data_request_email on data_request (email);
create index idx_data_request_tenant on data_request (tenant_id);
