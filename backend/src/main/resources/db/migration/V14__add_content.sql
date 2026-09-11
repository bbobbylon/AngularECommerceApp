-- V14: simple CMS (roadmap #17) — an admin-editable site announcement banner (singleton, replaces the
-- old hardcoded "This week's sale" strip) and a FAQ entry list (replaces the old hardcoded FAQ page).
-- New tables only. Matches the SiteBanner/FaqEntry entities (ddl-auto=validate checks on boot).

create table site_banner (
    active bit not null,
    id bigint not null auto_increment,
    link_text varchar(255),
    link_url varchar(255),
    message varchar(500) not null,
    primary key (id)
) engine=InnoDB;

create table faq_entry (
    active bit not null,
    sort_order integer not null,
    id bigint not null auto_increment,
    answer varchar(2000) not null,
    question varchar(500) not null,
    primary key (id)
) engine=InnoDB;
