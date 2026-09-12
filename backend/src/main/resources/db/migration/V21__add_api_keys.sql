-- V21: API keys for headless/programmatic access to a tenant's own back office (roadmap #23,
-- Milestone A). key_hash is the SHA-256 hash of the raw secret — the raw value is shown to the
-- caller exactly once at issue time and never persisted; key_prefix is a short, unhashed slice
-- shown in list views so an admin can tell keys apart without ever seeing the full secret again.
-- authorities reuses the existing Admin/OrderManager/Viewer role strings verbatim (roadmap #19) —
-- no new authority vocabulary.

create table api_key (
    id bigint not null auto_increment,
    tenant_id bigint not null,
    name varchar(255) not null,
    key_prefix varchar(16) not null,
    key_hash varchar(64) not null,
    authorities varchar(255) not null,
    active bit not null,
    expires_at datetime(6),
    last_used_at datetime(6),
    revoked_at datetime(6),
    date_created datetime(6),
    primary key (id),
    constraint uk_api_key_key_hash unique (key_hash),
    constraint fk_api_key_tenant foreign key (tenant_id) references tenant (id)
) engine=InnoDB;

create index idx_api_key_tenant on api_key (tenant_id);
