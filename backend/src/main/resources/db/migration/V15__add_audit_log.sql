-- V15: global admin audit log (roadmap #19) — one row per admin mutation (who/what/when), separate
-- from the domain-specific inventory_adjustment ledger (V12), which only covers stock changes. New
-- table only. Matches the AuditLogEntry entity (ddl-auto=validate checks on boot).

create table audit_log_entry (
    id bigint not null auto_increment,
    actor varchar(255),
    action varchar(255),
    entity_type varchar(255),
    entity_id varchar(255),
    details varchar(500),
    created_at datetime(6),
    primary key (id)
) engine=InnoDB;

create index idx_audit_log_entity_type on audit_log_entry (entity_type);
create index idx_audit_log_created_at on audit_log_entry (created_at);
