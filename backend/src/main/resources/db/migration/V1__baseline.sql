-- V1 baseline: the full schema, generated from the JPA entities (Hibernate, MySQL dialect) so it
-- matches them exactly (ddl-auto=validate verifies this on boot). Existing databases created by the
-- old ddl-auto=update are stamped at this baseline (V1 is NOT re-run on them); fresh databases get
-- the schema from this script. Future schema changes go in V2__*.sql, V3__*.sql, … — never edit V1.

create table address (id bigint not null auto_increment, city varchar(255), country varchar(255), state varchar(255), street varchar(255), zip_code varchar(255), primary key (id)) engine=InnoDB;
create table country (id integer not null auto_increment, code varchar(255), name varchar(255), primary key (id)) engine=InnoDB;
create table coupon (active bit, amount_off decimal(38,2), expires_at date, min_spend decimal(38,2), percent_off integer, id bigint not null auto_increment, code varchar(255) not null, description varchar(255), primary key (id)) engine=InnoDB;
create table customer (newsletter_subscribed bit, id bigint not null auto_increment, email varchar(255), first_name varchar(255), last_name varchar(255), unsubscribe_token varchar(255), primary key (id)) engine=InnoDB;
create table newsletter_subscriber (subscribed bit, date_created datetime(6) not null, id bigint not null auto_increment, email varchar(255) not null, name varchar(255), unsubscribe_token varchar(255), primary key (id)) engine=InnoDB;
create table order_item (quantity integer, unit_price decimal(38,2), id bigint not null auto_increment, order_id bigint, product_id bigint, image_url varchar(255), primary key (id)) engine=InnoDB;
create table orders (discount_amount decimal(38,2), total_price decimal(38,2), total_quantity integer, billing_address_id bigint, customer_id bigint, date_created datetime(6) not null, id bigint not null auto_increment, last_updated datetime(6) not null, shipping_address_id bigint, coupon_code varchar(255), order_tracking_number varchar(255), status varchar(255), primary key (id)) engine=InnoDB;
create table product (active bit, average_rating float(53), original_price decimal(38,2), review_count integer, unit_price decimal(38,2), units_in_stock integer, category_id bigint, date_created datetime(6) not null, id bigint not null auto_increment, last_updated datetime(6) not null, description varchar(255), image_url varchar(255), name varchar(255), sku varchar(255), primary key (id)) engine=InnoDB;
create table product_category (id bigint not null auto_increment, category_name varchar(255), primary key (id)) engine=InnoDB;
create table product_image (sort_order integer not null check ((sort_order>=0)), product_id bigint not null, image_url varchar(255), primary key (sort_order, product_id)) engine=InnoDB;
create table review (rating integer, verified_buyer bit, date_created datetime(6) not null, id bigint not null auto_increment, product_id bigint not null, comment varchar(2000), author_name varchar(255), primary key (id)) engine=InnoDB;
create table state (country_id integer, id integer not null auto_increment, name varchar(255), primary key (id)) engine=InnoDB;
create table wishlist_item (id bigint not null auto_increment, product_id bigint not null, email varchar(255) not null, primary key (id)) engine=InnoDB;
alter table coupon add constraint UKbg4p9ontpj7adq7yr71h93sdn unique (code);
alter table customer add constraint UKdrrqd09kv22wldtyfxv1970ul unique (unsubscribe_token);
alter table newsletter_subscriber add constraint UKjmyiin4onxy5rh5bskafkxrgl unique (email);
alter table newsletter_subscriber add constraint UKas5h0xt1e6ilvlrc3upym8rc1 unique (unsubscribe_token);
alter table orders add constraint UKi4xhef5x6drd02us28r33k430 unique (billing_address_id);
alter table orders add constraint UKsdv8vvdhj9gxm0dfoeh2rqvkh unique (shipping_address_id);
alter table wishlist_item add constraint UK8c07gwseuclbmpu3qglh6fxne unique (email, product_id);
alter table order_item add constraint FKt4dc2r9nbvbujrljv3e23iibt foreign key (order_id) references orders (id);
alter table orders add constraint FKqraecqgbbr1p37ic9dr44e2dr foreign key (billing_address_id) references address (id);
alter table orders add constraint FK624gtjin3po807j3vix093tlf foreign key (customer_id) references customer (id);
alter table orders add constraint FKh0uue95ltjysfmkqb5abgk7tj foreign key (shipping_address_id) references address (id);
alter table product add constraint FK5cypb0k23bovo3rn1a5jqs6j4 foreign key (category_id) references product_category (id);
alter table product_image add constraint FK6oo0cvcdtb6qmwsga468uuukk foreign key (product_id) references product (id);
alter table state add constraint FKghic7mqjt6qb9vq7up7awu0er foreign key (country_id) references country (id);
