-- Luv2Shop: create the database, the application user, all tables, and seed data.
-- Run this in MySQL Workbench while connected to YOUR MySQL server (as root).
-- After it runs, the app can connect (user 'ecommerceapp' / pass 'ecommerceapp') and show data.

CREATE DATABASE IF NOT EXISTS `full-stack-ecommerce`;

CREATE USER IF NOT EXISTS 'ecommerceapp'@'%' IDENTIFIED BY 'ecommerceapp';
GRANT ALL PRIVILEGES ON `full-stack-ecommerce`.* TO 'ecommerceapp'@'%';
FLUSH PRIVILEGES;

USE `full-stack-ecommerce`;

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `zip_code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `country` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO `country` VALUES (1,'US','United States'),(2,'CA','Canada'),(3,'BR','Brazil'),(4,'DE','Germany'),(5,'IN','India'),(6,'AU','Australia');
CREATE TABLE `customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(255) DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `unit_price` decimal(38,2) DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKt4dc2r9nbvbujrljv3e23iibt` (`order_id`),
  CONSTRAINT `FKt4dc2r9nbvbujrljv3e23iibt` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime(6) NOT NULL,
  `last_updated` datetime(6) NOT NULL,
  `order_tracking_number` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `total_price` decimal(38,2) DEFAULT NULL,
  `total_quantity` int DEFAULT NULL,
  `billing_address_id` bigint DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `shipping_address_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKi4xhef5x6drd02us28r33k430` (`billing_address_id`),
  UNIQUE KEY `UKsdv8vvdhj9gxm0dfoeh2rqvkh` (`shipping_address_id`),
  KEY `FK624gtjin3po807j3vix093tlf` (`customer_id`),
  CONSTRAINT `FK624gtjin3po807j3vix093tlf` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
  CONSTRAINT `FKh0uue95ltjysfmkqb5abgk7tj` FOREIGN KEY (`shipping_address_id`) REFERENCES `address` (`id`),
  CONSTRAINT `FKqraecqgbbr1p37ic9dr44e2dr` FOREIGN KEY (`billing_address_id`) REFERENCES `address` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) DEFAULT NULL,
  `date_created` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `last_updated` datetime(6) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `sku` varchar(255) DEFAULT NULL,
  `unit_price` decimal(38,2) DEFAULT NULL,
  `units_in_stock` int DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5cypb0k23bovo3rn1a5jqs6j4` (`category_id`),
  CONSTRAINT `FK5cypb0k23bovo3rn1a5jqs6j4` FOREIGN KEY (`category_id`) REFERENCES `product_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO `product` VALUES (1,_binary '','2026-06-14 19:42:52.462000','A hands-on guide to building production-ready Spring Boot applications.','https://placehold.co/600x400?text=The+Art+of+Spring+Boot','2026-06-14 19:42:52.462000','The Art of Spring Boot','BOOK-TECH-1000',21.99,100,1),(2,_binary '','2026-06-14 19:42:52.465000','Master modern standalone Angular with practical, real-world examples.','https://placehold.co/600x400?text=Angular+in+Action','2026-06-14 19:42:52.465000','Angular in Action','BOOK-TECH-1001',27.99,100,1),(3,_binary '','2026-06-14 19:42:52.467000','Connect a Java backend to an Angular frontend the right way.','https://placehold.co/600x400?text=Full+Stack+Foundations','2026-06-14 19:42:52.467000','Full Stack Foundations','BOOK-TECH-1002',24.49,100,1),(4,_binary '','2026-06-14 19:42:52.468000','Write readable, maintainable Java with confidence.','https://placehold.co/600x400?text=Clean+Java','2026-06-14 19:42:52.468000','Clean Java','BOOK-TECH-1003',19.99,100,1),(5,_binary '','2026-06-14 19:42:52.469000','Everything a backend developer needs to know about relational databases.','https://placehold.co/600x400?text=SQL+for+Developers','2026-06-14 19:42:52.469000','SQL for Developers','BOOK-TECH-1004',18.50,100,1),(6,_binary '','2026-06-14 19:42:52.470000','11oz ceramic mug for the caffeine-driven developer.','https://placehold.co/600x400?text=Code+&+Coffee+Mug','2026-06-14 19:42:52.470000','Code & Coffee Mug','MUG-COFFEE-2000',12.99,200,2),(7,_binary '','2026-06-14 19:42:52.471000','A mug that\'s never empty (unless it is).','https://placehold.co/600x400?text=Null+Pointer+Mug','2026-06-14 19:42:52.471000','Null Pointer Mug','MUG-COFFEE-2001',13.49,200,2),(8,_binary '','2026-06-14 19:42:52.472000','Motivation in ceramic form.','https://placehold.co/600x400?text=Ship+It+Mug','2026-06-14 19:42:52.472000','Ship It Mug','MUG-COFFEE-2002',11.99,200,2),(9,_binary '','2026-06-14 19:42:52.473000','Extra-large 15oz mug for long debugging sessions.','https://placehold.co/600x400?text=Stay+Caffeinated+Mug','2026-06-14 19:42:52.473000','Stay Caffeinated Mug','MUG-COFFEE-2003',14.99,200,2),(10,_binary '','2026-06-14 19:42:52.474000','The first mug every programmer should own.','https://placehold.co/600x400?text=Hello+World+Mug','2026-06-14 19:42:52.474000','Hello World Mug','MUG-COFFEE-2004',10.99,200,2),(11,_binary '','2026-06-14 19:42:52.476000','Large low-friction surface for precise tracking.','https://placehold.co/600x400?text=Pro+Gaming+Mouse+Pad','2026-06-14 19:42:52.476000','Pro Gaming Mouse Pad','PAD-MOUSE-3000',16.99,150,3),(12,_binary '','2026-06-14 19:42:52.477000','Memory-foam wrist support for all-day comfort.','https://placehold.co/600x400?text=Ergonomic+Wrist+Pad','2026-06-14 19:42:52.477000','Ergonomic Wrist Pad','PAD-MOUSE-3001',18.99,150,3),(13,_binary '','2026-06-14 19:42:52.478000','Full-desk mat that doubles as a mouse pad.','https://placehold.co/600x400?text=Desk+Mat+XL','2026-06-14 19:42:52.478000','Desk Mat XL','PAD-MOUSE-3002',29.99,150,3),(14,_binary '','2026-06-14 19:42:52.479000','Slim, water-resistant, and durable.','https://placehold.co/600x400?text=Minimal+Mouse+Pad','2026-06-14 19:42:52.479000','Minimal Mouse Pad','PAD-MOUSE-3003',9.99,150,3),(15,_binary '','2026-06-14 19:42:52.480000','Light up your desk with customizable RGB edges.','https://placehold.co/600x400?text=RGB+Mouse+Pad','2026-06-14 19:42:52.480000','RGB Mouse Pad','PAD-MOUSE-3004',24.99,150,3),(16,_binary '','2026-06-14 19:42:52.482000','Lightweight hard-shell carry-on with 360-degree wheels.','https://placehold.co/600x400?text=Carry-On+Spinner','2026-06-14 19:42:52.482000','Carry-On Spinner','LUGG-TRAVEL-4000',89.99,75,4),(17,_binary '','2026-06-14 19:42:52.483000','Spacious checked bag with an expandable zipper.','https://placehold.co/600x400?text=Checked+Suitcase','2026-06-14 19:42:52.483000','Checked Suitcase','LUGG-TRAVEL-4001',129.99,75,4),(18,_binary '','2026-06-14 19:42:52.484000','Durable canvas duffel sized for a long weekend.','https://placehold.co/600x400?text=Weekender+Duffel','2026-06-14 19:42:52.484000','Weekender Duffel','LUGG-TRAVEL-4002',59.99,75,4),(19,_binary '','2026-06-14 19:42:52.485000','Padded travel backpack with a dedicated laptop sleeve.','https://placehold.co/600x400?text=Laptop+Backpack','2026-06-14 19:42:52.485000','Laptop Backpack','LUGG-TRAVEL-4003',64.99,75,4),(20,_binary '','2026-06-14 19:42:52.486000','Stay organized with a set of four packing cubes.','https://placehold.co/600x400?text=Packing+Cube+Set','2026-06-14 19:42:52.486000','Packing Cube Set','LUGG-TRAVEL-4004',24.99,75,4);
CREATE TABLE `product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO `product_category` VALUES (1,'Books'),(2,'Coffee Mugs'),(3,'Mouse Pads'),(4,'Luggage');
CREATE TABLE `state` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `country_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKghic7mqjt6qb9vq7up7awu0er` (`country_id`),
  CONSTRAINT `FKghic7mqjt6qb9vq7up7awu0er` FOREIGN KEY (`country_id`) REFERENCES `country` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO `state` VALUES (1,'California',1),(2,'Texas',1),(3,'New York',1),(4,'Florida',1),(5,'Pennsylvania',1),(6,'Illinois',1),(7,'Ohio',1),(8,'Washington',1),(9,'Ontario',2),(10,'Quebec',2),(11,'British Columbia',2),(12,'Alberta',2),(13,'Manitoba',2),(14,'Nova Scotia',2),(15,'Sao Paulo',3),(16,'Rio de Janeiro',3),(17,'Bahia',3),(18,'Parana',3),(19,'Bavaria',4),(20,'Berlin',4),(21,'Hamburg',4),(22,'Hesse',4),(23,'Saxony',4),(24,'Maharashtra',5),(25,'Karnataka',5),(26,'Tamil Nadu',5),(27,'Delhi',5),(28,'Gujarat',5),(29,'West Bengal',5),(30,'New South Wales',6),(31,'Victoria',6),(32,'Queensland',6),(33,'Western Australia',6),(34,'Tasmania',6);

SET FOREIGN_KEY_CHECKS = 1;
