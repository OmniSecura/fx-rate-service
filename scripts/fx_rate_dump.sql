-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: fx_rate_db
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `currency`
--

DROP TABLE IF EXISTS `currency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `currency` (
  `currency_id` int NOT NULL,
  `iso_code` char(3) NOT NULL,
  `currency_name` varchar(60) NOT NULL,
  `country` varchar(60) NOT NULL,
  `numeric_code` char(3) NOT NULL,
  `minor_units` smallint NOT NULL DEFAULT '2',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `region` varchar(20) NOT NULL,
  PRIMARY KEY (`currency_id`),
  UNIQUE KEY `iso_code` (`iso_code`),
  UNIQUE KEY `numeric_code` (`numeric_code`),
  CONSTRAINT `chk_minor_units` CHECK (((`minor_units` >= 0) and (`minor_units` <= 4))),
  CONSTRAINT `chk_region` CHECK ((`region` in (_utf8mb4'EMEA',_utf8mb4'AMER',_utf8mb4'APAC')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `currency`
--

LOCK TABLES `currency` WRITE;
/*!40000 ALTER TABLE `currency` DISABLE KEYS */;
INSERT INTO `currency` VALUES (1,'USD','US Dollar','United States','840',2,1,'AMER'),(2,'EUR','Euro','Euro Area','978',2,1,'EMEA'),(3,'GBP','Pound Sterling','United Kingdom','826',2,1,'EMEA'),(4,'JPY','Japanese Yen','Japan','392',0,1,'APAC'),(5,'CHF','Swiss Franc','Switzerland','756',2,1,'EMEA'),(6,'AUD','Australian Dollar','Australia','036',2,1,'APAC'),(7,'CAD','Canadian Dollar','Canada','124',2,1,'AMER'),(8,'NZD','New Zealand Dollar','New Zealand','554',2,1,'APAC'),(9,'HKD','Hong Kong Dollar','Hong Kong','344',2,1,'APAC'),(10,'SGD','Singapore Dollar','Singapore','702',2,1,'APAC'),(11,'NOK','Norwegian Krone','Norway','578',2,1,'EMEA'),(12,'SEK','Swedish Krona','Sweden','752',2,1,'EMEA'),(13,'DKK','Danish Krone','Denmark','208',2,1,'EMEA'),(14,'CNH','Chinese Yuan (Offshore)','China','156',2,1,'APAC'),(15,'INR','Indian Rupee','India','356',2,1,'APAC'),(16,'KRW','South Korean Won','South Korea','410',0,1,'APAC'),(17,'MXN','Mexican Peso','Mexico','484',2,1,'AMER'),(18,'BRL','Brazilian Real','Brazil','986',2,1,'AMER'),(19,'ZAR','South African Rand','South Africa','710',2,1,'EMEA'),(20,'PLN','Polish Zloty','Poland','985',2,1,'EMEA');
/*!40000 ALTER TABLE `currency` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `currency_pair`
--

DROP TABLE IF EXISTS `currency_pair`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `currency_pair` (
  `pair_id` int NOT NULL,
  `pair_code` varchar(7) NOT NULL,
  `base_currency` char(3) NOT NULL,
  `quote_currency` char(3) NOT NULL,
  `pair_type` varchar(15) NOT NULL,
  `decimal_places` smallint NOT NULL DEFAULT '4',
  `pip_size` decimal(10,6) NOT NULL DEFAULT '0.000100',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`pair_id`),
  UNIQUE KEY `pair_code` (`pair_code`),
  KEY `base_currency` (`base_currency`),
  KEY `quote_currency` (`quote_currency`),
  CONSTRAINT `currency_pair_ibfk_1` FOREIGN KEY (`base_currency`) REFERENCES `currency` (`iso_code`),
  CONSTRAINT `currency_pair_ibfk_2` FOREIGN KEY (`quote_currency`) REFERENCES `currency` (`iso_code`),
  CONSTRAINT `chk_decimal_places` CHECK (((`decimal_places` >= 0) and (`decimal_places` <= 6))),
  CONSTRAINT `chk_different_currencies` CHECK ((`base_currency` <> `quote_currency`)),
  CONSTRAINT `chk_pair_type` CHECK ((`pair_type` in (_utf8mb4'MAJOR',_utf8mb4'MINOR',_utf8mb4'EXOTIC',_utf8mb4'CROSS'))),
  CONSTRAINT `chk_pip_size` CHECK ((`pip_size` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `currency_pair`
--

LOCK TABLES `currency_pair` WRITE;
/*!40000 ALTER TABLE `currency_pair` DISABLE KEYS */;
INSERT INTO `currency_pair` VALUES (1,'EUR/USD','EUR','USD','MAJOR',4,0.000100,1),(2,'GBP/USD','GBP','USD','MAJOR',4,0.000100,1),(3,'USD/JPY','USD','JPY','MAJOR',2,0.010000,1),(4,'USD/CHF','USD','CHF','MAJOR',4,0.000100,1),(5,'AUD/USD','AUD','USD','MAJOR',4,0.000100,1),(6,'USD/CAD','USD','CAD','MAJOR',4,0.000100,1),(7,'NZD/USD','NZD','USD','MAJOR',4,0.000100,1),(8,'EUR/GBP','EUR','GBP','MINOR',4,0.000100,1),(9,'EUR/JPY','EUR','JPY','MINOR',2,0.010000,1),(10,'GBP/JPY','GBP','JPY','MINOR',2,0.010000,1),(11,'EUR/CHF','EUR','CHF','MINOR',4,0.000100,1),(12,'GBP/CHF','GBP','CHF','MINOR',4,0.000100,1),(13,'AUD/JPY','AUD','JPY','CROSS',2,0.010000,1),(14,'USD/HKD','USD','HKD','MINOR',4,0.000100,1),(15,'USD/SGD','USD','SGD','MINOR',4,0.000100,1),(16,'USD/CNH','USD','CNH','MINOR',4,0.000100,1),(17,'USD/INR','USD','INR','EXOTIC',4,0.000100,1),(18,'EUR/NOK','EUR','NOK','MINOR',4,0.000100,1),(19,'USD/MXN','USD','MXN','EXOTIC',4,0.000100,1),(20,'USD/ZAR','USD','ZAR','EXOTIC',4,0.000100,1);
/*!40000 ALTER TABLE `currency_pair` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eod_fixing`
--

DROP TABLE IF EXISTS `eod_fixing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eod_fixing` (
  `fixing_id` int NOT NULL,
  `pair_id` int NOT NULL,
  `provider_id` int NOT NULL,
  `fixing_date` date NOT NULL,
  `fixing_rate` decimal(18,6) NOT NULL,
  `fixing_time` varchar(10) NOT NULL,
  `fixing_type` varchar(20) NOT NULL,
  `is_official` tinyint(1) NOT NULL DEFAULT '1',
  `published_at` timestamp NOT NULL,
  PRIMARY KEY (`fixing_id`),
  UNIQUE KEY `pair_id` (`pair_id`,`fixing_date`,`fixing_type`),
  KEY `provider_id` (`provider_id`),
  KEY `idx_fixing_date` (`fixing_date`),
  CONSTRAINT `eod_fixing_ibfk_1` FOREIGN KEY (`pair_id`) REFERENCES `currency_pair` (`pair_id`),
  CONSTRAINT `eod_fixing_ibfk_2` FOREIGN KEY (`provider_id`) REFERENCES `rate_provider` (`provider_id`),
  CONSTRAINT `chk_fixing_rate_positive` CHECK ((`fixing_rate` > 0)),
  CONSTRAINT `chk_fixing_type` CHECK ((`fixing_type` in (_utf8mb4'WMR',_utf8mb4'ECB',_utf8mb4'BFIX',_utf8mb4'INTERNAL'))),
  CONSTRAINT `chk_published_after_date` CHECK ((`published_at` >= `fixing_date`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eod_fixing`
--

LOCK TABLES `eod_fixing` WRITE;
/*!40000 ALTER TABLE `eod_fixing` DISABLE KEYS */;
INSERT INTO `eod_fixing` VALUES (1,1,8,'2026-03-25',1.082870,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(2,2,8,'2026-03-25',1.295980,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(3,3,8,'2026-03-25',149.832000,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(4,4,8,'2026-03-25',0.901180,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(5,5,8,'2026-03-25',0.635210,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(6,6,8,'2026-03-25',1.366920,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(7,7,8,'2026-03-25',0.589120,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(8,8,3,'2026-03-25',0.834970,'11:00 ECB','ECB',1,'2026-03-25 11:15:00'),(9,9,3,'2026-03-25',162.211000,'11:00 ECB','ECB',1,'2026-03-25 11:15:00'),(10,10,9,'2026-03-25',194.154000,'16:00 LON','BFIX',1,'2026-03-25 16:05:00'),(11,11,3,'2026-03-25',0.974110,'11:00 ECB','ECB',1,'2026-03-25 11:15:00'),(12,12,9,'2026-03-25',1.165210,'16:00 LON','BFIX',1,'2026-03-25 16:05:00'),(13,13,9,'2026-03-25',95.398700,'16:00 LON','BFIX',1,'2026-03-25 16:05:00'),(14,14,20,'2026-03-25',7.825090,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(15,15,18,'2026-03-25',1.347990,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(16,16,8,'2026-03-25',7.233880,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(17,17,5,'2026-03-25',83.614500,'17:00 NYC','WMR',1,'2026-03-25 17:05:00'),(18,18,3,'2026-03-25',11.781400,'11:00 ECB','ECB',1,'2026-03-25 11:15:00'),(19,19,8,'2026-03-25',20.208800,'16:00 LON','WMR',1,'2026-03-25 16:05:00'),(20,20,8,'2026-03-25',18.876200,'16:00 LON','WMR',1,'2026-03-25 16:05:00');
/*!40000 ALTER TABLE `eod_fixing` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exchange_rate`
--

DROP TABLE IF EXISTS `exchange_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exchange_rate` (
  `rate_id` int NOT NULL,
  `pair_id` int NOT NULL,
  `provider_id` int NOT NULL,
  `bid_rate` decimal(18,6) NOT NULL,
  `ask_rate` decimal(18,6) NOT NULL,
  `mid_rate` decimal(18,6) NOT NULL,
  `rate_timestamp` timestamp NOT NULL,
  `source_system` varchar(30) NOT NULL,
  `is_valid` tinyint(1) NOT NULL DEFAULT '1',
  `is_stale` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`rate_id`),
  KEY `idx_exrate_pair_ts` (`pair_id`,`rate_timestamp`),
  KEY `idx_exrate_provider` (`provider_id`),
  CONSTRAINT `exchange_rate_ibfk_1` FOREIGN KEY (`pair_id`) REFERENCES `currency_pair` (`pair_id`),
  CONSTRAINT `exchange_rate_ibfk_2` FOREIGN KEY (`provider_id`) REFERENCES `rate_provider` (`provider_id`),
  CONSTRAINT `chk_rates_positive` CHECK (((`bid_rate` > 0) and (`ask_rate` > 0) and (`mid_rate` > 0))),
  CONSTRAINT `chk_source_system` CHECK ((`source_system` in (_utf8mb4'REUTERS',_utf8mb4'BLOOMBERG',_utf8mb4'ECB_FEED',_utf8mb4'HSBC_INT',_utf8mb4'WMR',_utf8mb4'ICAP'))),
  CONSTRAINT `chk_spread` CHECK (((`bid_rate` <= `mid_rate`) and (`mid_rate` <= `ask_rate`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exchange_rate`
--

LOCK TABLES `exchange_rate` WRITE;
/*!40000 ALTER TABLE `exchange_rate` DISABLE KEYS */;
INSERT INTO `exchange_rate` VALUES (1,1,1,1.083120,1.083180,1.083150,'2026-03-26 08:00:01','REUTERS',1,0),(2,2,1,1.296210,1.296290,1.296250,'2026-03-26 08:00:02','REUTERS',1,0),(3,3,2,149.872000,149.878000,149.875000,'2026-03-26 08:00:03','BLOOMBERG',1,0),(4,4,2,0.901240,0.901300,0.901270,'2026-03-26 08:00:04','BLOOMBERG',1,0),(5,5,1,0.635420,0.635480,0.635450,'2026-03-26 08:00:05','REUTERS',1,0),(6,6,1,1.367140,1.367200,1.367170,'2026-03-26 08:00:06','REUTERS',1,0),(7,7,2,0.589210,0.589290,0.589250,'2026-03-26 08:00:07','BLOOMBERG',1,0),(8,8,8,0.835120,0.835180,0.835150,'2026-03-26 08:00:08','WMR',1,0),(9,9,8,162.245000,162.255000,162.250000,'2026-03-26 08:00:09','WMR',1,0),(10,10,10,194.181000,194.193000,194.187000,'2026-03-26 08:00:10','ICAP',1,0),(11,11,10,0.974220,0.974280,0.974250,'2026-03-26 08:00:11','ICAP',1,0),(12,12,14,1.165420,1.165540,1.165480,'2026-03-26 08:00:12','HSBC_INT',1,0),(13,13,2,95.421100,95.429300,95.425200,'2026-03-26 08:00:13','BLOOMBERG',1,0),(14,14,14,7.825410,7.825470,7.825440,'2026-03-26 08:00:14','HSBC_INT',1,0),(15,15,14,1.348210,1.348270,1.348240,'2026-03-26 08:00:15','HSBC_INT',1,0),(16,16,2,7.234120,7.234240,7.234180,'2026-03-26 08:00:16','BLOOMBERG',1,0),(17,17,1,83.624100,83.628900,83.626500,'2026-03-26 08:00:17','REUTERS',1,0),(18,18,1,11.782100,11.783300,11.782700,'2026-03-26 08:00:18','REUTERS',1,0),(19,19,2,20.214200,20.216400,20.215300,'2026-03-26 08:00:19','BLOOMBERG',1,0),(20,20,1,18.882100,18.884700,18.883400,'2026-03-26 08:00:20','REUTERS',1,0);
/*!40000 ALTER TABLE `exchange_rate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `forward_rate`
--

DROP TABLE IF EXISTS `forward_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forward_rate` (
  `forward_id` int NOT NULL,
  `pair_id` int NOT NULL,
  `provider_id` int NOT NULL,
  `tenor` varchar(5) NOT NULL,
  `value_date` date NOT NULL,
  `forward_points` decimal(10,4) NOT NULL,
  `forward_rate` decimal(18,6) NOT NULL,
  `rate_timestamp` timestamp NOT NULL,
  PRIMARY KEY (`forward_id`),
  KEY `pair_id` (`pair_id`),
  KEY `provider_id` (`provider_id`),
  CONSTRAINT `forward_rate_ibfk_1` FOREIGN KEY (`pair_id`) REFERENCES `currency_pair` (`pair_id`),
  CONSTRAINT `forward_rate_ibfk_2` FOREIGN KEY (`provider_id`) REFERENCES `rate_provider` (`provider_id`),
  CONSTRAINT `chk_forward_rate_positive` CHECK ((`forward_rate` > 0)),
  CONSTRAINT `chk_tenor` CHECK ((`tenor` in (_utf8mb4'ON',_utf8mb4'TN',_utf8mb4'1W',_utf8mb4'1M',_utf8mb4'2M',_utf8mb4'3M',_utf8mb4'6M',_utf8mb4'1Y'))),
  CONSTRAINT `chk_value_date_future` CHECK ((`value_date` >= `rate_timestamp`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `forward_rate`
--

LOCK TABLES `forward_rate` WRITE;
/*!40000 ALTER TABLE `forward_rate` DISABLE KEYS */;
INSERT INTO `forward_rate` VALUES (1,1,1,'1M','2026-04-28',-8.4200,1.074730,'2026-03-26 08:30:00'),(2,1,1,'3M','2026-06-26',-24.8100,1.080670,'2026-03-26 08:30:00'),(3,1,1,'6M','2026-09-28',-49.2200,1.078230,'2026-03-26 08:30:00'),(4,1,1,'1Y','2027-03-26',-95.4400,1.073610,'2026-03-26 08:30:00'),(5,2,1,'1M','2026-04-28',-12.3100,1.295020,'2026-03-26 08:30:00'),(6,2,1,'3M','2026-06-26',-35.7100,1.292680,'2026-03-26 08:30:00'),(7,2,1,'6M','2026-09-28',-68.4100,1.289410,'2026-03-26 08:30:00'),(8,3,2,'1M','2026-04-28',51.2000,150.387000,'2026-03-26 08:30:00'),(9,3,2,'3M','2026-06-26',148.7000,151.362000,'2026-03-26 08:30:00'),(10,3,2,'6M','2026-09-28',287.4000,152.749000,'2026-03-26 08:30:00'),(11,4,2,'1M','2026-04-28',-5.1200,0.896160,'2026-03-26 08:30:00'),(12,4,2,'3M','2026-06-26',-15.0100,0.886270,'2026-03-26 08:30:00'),(13,5,1,'1M','2026-04-28',-9.2100,0.626230,'2026-03-26 08:30:00'),(14,5,1,'3M','2026-06-26',-25.8200,0.619630,'2026-03-26 08:30:00'),(15,6,1,'1M','2026-04-28',22.4100,1.369410,'2026-03-26 08:30:00'),(16,6,1,'3M','2026-06-26',63.2100,1.373490,'2026-03-26 08:30:00'),(17,8,8,'1M','2026-04-28',3.1400,0.838290,'2026-03-26 08:30:00'),(18,9,8,'1M','2026-04-28',43.2000,162.682000,'2026-03-26 08:30:00'),(19,11,3,'1M','2026-04-28',-4.2100,0.970030,'2026-03-26 08:30:00'),(20,14,14,'1M','2026-04-28',0.1200,7.825560,'2026-03-26 08:30:00');
/*!40000 ALTER TABLE `forward_rate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `latest_rates_vw`
--

DROP TABLE IF EXISTS `latest_rates_vw`;
/*!50001 DROP VIEW IF EXISTS `latest_rates_vw`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `latest_rates_vw` AS SELECT 
 1 AS `pair_code`,
 1 AS `pair_type`,
 1 AS `mid_rate`,
 1 AS `rate_timestamp`,
 1 AS `is_active`,
 1 AS `hours_since_update`,
 1 AS `is_stale`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `rate_alert`
--

DROP TABLE IF EXISTS `rate_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rate_alert` (
  `alert_id` int NOT NULL,
  `pair_id` int NOT NULL,
  `alert_type` varchar(20) NOT NULL,
  `threshold_value` decimal(18,6) DEFAULT NULL,
  `actual_value` decimal(18,6) DEFAULT NULL,
  `alert_message` varchar(255) NOT NULL,
  `severity` varchar(10) NOT NULL,
  `triggered_at` timestamp NOT NULL,
  `acknowledged_at` timestamp NULL DEFAULT NULL,
  `acknowledged_by` varchar(50) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OPEN',
  PRIMARY KEY (`alert_id`),
  KEY `pair_id` (`pair_id`),
  CONSTRAINT `rate_alert_ibfk_1` FOREIGN KEY (`pair_id`) REFERENCES `currency_pair` (`pair_id`),
  CONSTRAINT `chk_ack_after_trigger` CHECK (((`acknowledged_at` is null) or (`acknowledged_at` >= `triggered_at`))),
  CONSTRAINT `chk_alert_type` CHECK ((`alert_type` in (_utf8mb4'THRESHOLD_BREACH',_utf8mb4'STALE_RATE',_utf8mb4'SPREAD_WIDE',_utf8mb4'SPIKE'))),
  CONSTRAINT `chk_severity` CHECK ((`severity` in (_utf8mb4'INFO',_utf8mb4'WARNING',_utf8mb4'CRITICAL'))),
  CONSTRAINT `chk_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'ACKNOWLEDGED',_utf8mb4'RESOLVED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rate_alert`
--

LOCK TABLES `rate_alert` WRITE;
/*!40000 ALTER TABLE `rate_alert` DISABLE KEYS */;
INSERT INTO `rate_alert` VALUES (1,1,'THRESHOLD_BREACH',1.090000,1.083100,'EUR/USD dropped below 1.0900 threshold','WARNING','2026-03-26 07:45:12','2026-03-26 08:02:33','fx.desk@hsbc.com','RESOLVED'),(2,3,'SPIKE',NULL,150.210000,'USD/JPY moved 80 pips in 60 seconds','CRITICAL','2026-03-26 04:12:08','2026-03-26 04:18:44','asia.desk@hsbc.com','RESOLVED'),(3,17,'THRESHOLD_BREACH',84.000000,83.626000,'USD/INR below 84.000 alert level','INFO','2026-03-26 05:30:00',NULL,NULL,'OPEN'),(4,20,'SPREAD_WIDE',0.010000,0.002600,'USD/ZAR spread 26 pips — exceeds 10 pip SLA','WARNING','2026-03-26 06:00:14','2026-03-26 06:15:02','ops@hsbc.com','RESOLVED'),(5,2,'STALE_RATE',NULL,NULL,'GBP/USD rate not updated for 45 seconds','WARNING','2026-03-25 16:59:55','2026-03-25 17:00:10','fx.desk@hsbc.com','RESOLVED'),(6,19,'SPIKE',NULL,20.650000,'USD/MXN spiked to 20.65 — possible illiquid fill','CRITICAL','2026-03-25 14:22:31','2026-03-25 14:25:00','latam.desk@hsbc.com','RESOLVED'),(7,16,'THRESHOLD_BREACH',7.300000,7.234000,'USD/CNH moved above 7.300 watch level','INFO','2026-03-24 09:11:42',NULL,NULL,'OPEN'),(8,4,'SPREAD_WIDE',0.001200,0.000600,'USD/CHF spread within SLA — auto-resolved','INFO','2026-03-24 10:00:00','2026-03-24 10:00:01','system','RESOLVED'),(9,9,'THRESHOLD_BREACH',163.000000,162.250000,'EUR/JPY below 163.00 — options barrier watch','WARNING','2026-03-24 11:30:18','2026-03-24 11:45:00','fx.desk@hsbc.com','ACKNOWLEDGED'),(10,5,'STALE_RATE',NULL,NULL,'AUD/USD rate stale during APAC liquidity gap','WARNING','2026-03-24 03:15:44','2026-03-24 03:20:00','apac.desk@hsbc.com','RESOLVED'),(11,1,'THRESHOLD_BREACH',1.100000,1.083100,'EUR/USD still below 1.1000 long-term level','INFO','2026-03-23 09:00:00',NULL,NULL,'OPEN'),(12,15,'SPIKE',NULL,1.362100,'USD/SGD spike detected in Asian open','WARNING','2026-03-23 01:30:22','2026-03-23 01:38:00','apac.desk@hsbc.com','RESOLVED'),(13,20,'THRESHOLD_BREACH',19.000000,18.883000,'USD/ZAR dropped below 19.000 — EM risk-on','INFO','2026-03-22 09:45:00',NULL,NULL,'OPEN'),(14,3,'THRESHOLD_BREACH',152.000000,149.870000,'USD/JPY below 152 — BOJ intervention watch','CRITICAL','2026-03-21 08:00:00','2026-03-21 08:30:00','rates.desk@hsbc.com','RESOLVED'),(15,6,'SPREAD_WIDE',0.001500,0.000600,'USD/CAD spread normalised after BoC meeting','INFO','2026-03-20 15:00:00','2026-03-20 15:01:00','system','RESOLVED'),(16,2,'THRESHOLD_BREACH',1.300000,1.296000,'GBP/USD below 1.3000 — BoE policy watch','WARNING','2026-03-20 09:30:00','2026-03-20 10:00:00','fx.desk@hsbc.com','RESOLVED'),(17,11,'STALE_RATE',NULL,NULL,'EUR/CHF stale — SNB rate decision pending','INFO','2026-03-19 08:29:50','2026-03-19 08:35:00','emea.desk@hsbc.com','RESOLVED'),(18,18,'THRESHOLD_BREACH',12.000000,11.782000,'EUR/NOK below 12.000 — oil price correlation','INFO','2026-03-19 11:00:00',NULL,NULL,'OPEN'),(19,7,'SPIKE',NULL,0.581200,'NZD/USD spike down — RBNZ surprise cut','CRITICAL','2026-03-18 22:00:05','2026-03-18 22:05:00','apac.desk@hsbc.com','RESOLVED'),(20,8,'THRESHOLD_BREACH',0.850000,0.835200,'EUR/GBP failed to breach 0.8500 resistance','INFO','2026-03-18 10:15:00',NULL,NULL,'OPEN');
/*!40000 ALTER TABLE `rate_alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rate_audit_log`
--

DROP TABLE IF EXISTS `rate_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rate_audit_log` (
  `log_id` int NOT NULL,
  `rate_id` int NOT NULL,
  `pair_id` int NOT NULL,
  `action` varchar(10) NOT NULL,
  `old_mid_rate` decimal(18,6) DEFAULT NULL,
  `new_mid_rate` decimal(18,6) DEFAULT NULL,
  `change_pct` decimal(8,4) DEFAULT NULL,
  `changed_by` varchar(50) NOT NULL,
  `changed_at` timestamp NOT NULL,
  `reason` varchar(120) DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `pair_id` (`pair_id`),
  CONSTRAINT `rate_audit_log_ibfk_1` FOREIGN KEY (`pair_id`) REFERENCES `currency_pair` (`pair_id`),
  CONSTRAINT `chk_action` CHECK ((`action` in (_utf8mb4'INSERT',_utf8mb4'UPDATE',_utf8mb4'INVALIDATE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rate_audit_log`
--

LOCK TABLES `rate_audit_log` WRITE;
/*!40000 ALTER TABLE `rate_audit_log` DISABLE KEYS */;
INSERT INTO `rate_audit_log` VALUES (1,1,1,'INSERT',NULL,1.083150,NULL,'feed.ingest','2026-03-26 08:00:01','Initial load from Reuters'),(2,3,3,'INSERT',NULL,149.875000,NULL,'feed.ingest','2026-03-26 08:00:03','Initial load from Bloomberg'),(3,17,17,'INSERT',NULL,83.626500,NULL,'feed.ingest','2026-03-26 08:00:17','Initial load from Reuters'),(4,1,1,'UPDATE',1.083150,1.083420,0.0249,'feed.ingest','2026-03-26 08:15:00','Tick update'),(5,3,3,'UPDATE',149.875000,150.212000,0.2249,'feed.ingest','2026-03-26 04:12:08','Spike detection — large move'),(6,3,3,'INVALIDATE',150.212000,NULL,NULL,'risk.control','2026-03-26 04:12:15','Rate spike invalidated pending review'),(7,3,3,'UPDATE',NULL,149.921000,NULL,'risk.control','2026-03-26 04:18:44','Corrected rate reloaded after review'),(8,6,6,'INSERT',NULL,1.367170,NULL,'feed.ingest','2026-03-26 08:00:06','Initial load from Reuters'),(9,6,6,'UPDATE',1.367170,1.366980,-0.0139,'feed.ingest','2026-03-26 09:00:00','Tick update'),(10,19,19,'INSERT',NULL,20.215300,NULL,'feed.ingest','2026-03-26 08:00:19','Initial load from Bloomberg'),(11,19,19,'UPDATE',20.215300,20.650000,2.1459,'feed.ingest','2026-03-25 14:22:31','Spike — possible fat-finger'),(12,19,19,'INVALIDATE',20.650000,NULL,NULL,'risk.control','2026-03-25 14:22:35','Fat finger invalidated'),(13,19,19,'UPDATE',NULL,20.208800,NULL,'risk.control','2026-03-25 14:25:00','Corrected rate'),(14,2,2,'INSERT',NULL,1.296250,NULL,'feed.ingest','2026-03-26 08:00:02','Initial load from Reuters'),(15,2,2,'UPDATE',1.296250,1.295980,-0.0209,'feed.ingest','2026-03-25 16:59:55','EOD tick'),(16,2,2,'INVALIDATE',1.295980,NULL,NULL,'feed.ingest','2026-03-25 16:59:55','Stale rate — no provider update'),(17,7,7,'INSERT',NULL,0.589250,NULL,'feed.ingest','2026-03-26 08:00:07','Initial load from Bloomberg'),(18,7,7,'UPDATE',0.589250,0.581200,-1.3666,'feed.ingest','2026-03-18 22:00:05','RBNZ surprise rate cut'),(19,20,20,'INSERT',NULL,18.883400,NULL,'feed.ingest','2026-03-26 08:00:20','Initial load from Reuters'),(20,20,20,'UPDATE',18.883400,18.896200,0.0677,'feed.ingest','2026-03-26 09:30:00','EM session update');
/*!40000 ALTER TABLE `rate_audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `rate_history_vw`
--

DROP TABLE IF EXISTS `rate_history_vw`;
/*!50001 DROP VIEW IF EXISTS `rate_history_vw`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `rate_history_vw` AS SELECT 
 1 AS `rate_id`,
 1 AS `pair_id`,
 1 AS `pair_code`,
 1 AS `base_currency`,
 1 AS `quote_currency`,
 1 AS `pair_type`,
 1 AS `provider_id`,
 1 AS `provider_code`,
 1 AS `provider_name`,
 1 AS `bid_rate`,
 1 AS `ask_rate`,
 1 AS `mid_rate`,
 1 AS `spread`,
 1 AS `rate_timestamp`,
 1 AS `source_system`,
 1 AS `is_valid`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `rate_provider`
--

DROP TABLE IF EXISTS `rate_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rate_provider` (
  `provider_id` int NOT NULL,
  `provider_code` varchar(15) NOT NULL,
  `provider_name` varchar(80) NOT NULL,
  `provider_type` varchar(20) NOT NULL,
  `country` char(2) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `priority` smallint NOT NULL DEFAULT '1',
  PRIMARY KEY (`provider_id`),
  UNIQUE KEY `provider_code` (`provider_code`),
  CONSTRAINT `chk_priority` CHECK ((`priority` >= 1)),
  CONSTRAINT `chk_provider_type` CHECK ((`provider_type` in (_utf8mb4'MARKET_DATA',_utf8mb4'CENTRAL_BANK',_utf8mb4'INTERNAL',_utf8mb4'BROKER')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rate_provider`
--

LOCK TABLES `rate_provider` WRITE;
/*!40000 ALTER TABLE `rate_provider` DISABLE KEYS */;
INSERT INTO `rate_provider` VALUES (1,'REUTERS','Refinitiv (Reuters) Eikon','MARKET_DATA','GB',1,1),(2,'BLOOMBERG','Bloomberg Terminal FX','MARKET_DATA','US',1,1),(3,'ECB','European Central Bank','CENTRAL_BANK','DE',1,2),(4,'BOE','Bank of England','CENTRAL_BANK','GB',1,2),(5,'FED','US Federal Reserve','CENTRAL_BANK','US',1,2),(6,'BOFJ','Bank of Japan','CENTRAL_BANK','JP',1,2),(7,'SNB','Swiss National Bank','CENTRAL_BANK','CH',1,2),(8,'WMR','WM/Reuters 4pm London Fix','MARKET_DATA','GB',1,1),(9,'BFIX','Bloomberg BFIX Benchmark','MARKET_DATA','US',1,1),(10,'ICAP','ICAP EBS FX Platform','BROKER','GB',1,1),(11,'CURRENEX','State Street Currenex','BROKER','US',1,2),(12,'FXC','FXConnect (State Street)','BROKER','US',1,2),(13,'RTFX','Refinitiv FXall','BROKER','GB',1,2),(14,'HSBC_INT','HSBC Internal Rate Engine','INTERNAL','GB',1,1),(15,'FXCM','FXCM Institutional','BROKER','US',1,3),(16,'OANDA','OANDA Rate API','MARKET_DATA','US',1,3),(17,'XE','XE.com Corporate FX','MARKET_DATA','CA',1,3),(18,'MAS','Monetary Authority of Singapore','CENTRAL_BANK','SG',1,2),(19,'RBA','Reserve Bank of Australia','CENTRAL_BANK','AU',1,2),(20,'HKMA','Hong Kong Monetary Authority','CENTRAL_BANK','HK',1,2);
/*!40000 ALTER TABLE `rate_provider` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'fx_rate_db'
--
/*!50003 DROP PROCEDURE IF EXISTS `get_cross_rate` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `get_cross_rate`(
    IN p_pair1 VARCHAR(7),
    IN p_pair2 VARCHAR(7),
    IN p_stale_minutes INT
)
main_block: BEGIN
    DECLARE v_stale_minutes INT DEFAULT 60;
    DECLARE v_base1 VARCHAR(3);
    DECLARE v_quote1 VARCHAR(3);
    DECLARE v_base2 VARCHAR(3);
    DECLARE v_quote2 VARCHAR(3);
    DECLARE v_common VARCHAR(3);
    DECLARE v_cross_base VARCHAR(3);
    DECLARE v_cross_quote VARCHAR(3);
    DECLARE v_mid1 DECIMAL(18,6);
    DECLARE v_mid2 DECIMAL(18,6);
    DECLARE v_rate1_time TIMESTAMP;
    DECLARE v_rate2_time TIMESTAMP;
    DECLARE v_is_stale1 BOOLEAN;
    DECLARE v_is_stale2 BOOLEAN;
    DECLARE v_cross_rate DECIMAL(18,6);

    IF p_stale_minutes IS NOT NULL AND p_stale_minutes > 0 THEN
        SET v_stale_minutes = p_stale_minutes;
    END IF;

    SET v_base1 = SUBSTRING_INDEX(p_pair1, '/', 1);
    SET v_quote1 = SUBSTRING_INDEX(p_pair1, '/', -1);
    SET v_base2 = SUBSTRING_INDEX(p_pair2, '/', 1);
    SET v_quote2 = SUBSTRING_INDEX(p_pair2, '/', -1);

    IF v_base1 = v_base2 THEN
        SET v_common = v_base1;
        SET v_cross_base = v_quote1;
        SET v_cross_quote = v_quote2;
    ELSEIF v_base1 = v_quote2 THEN
        SET v_common = v_base1;
        SET v_cross_base = v_quote1;
        SET v_cross_quote = v_base2;
    ELSEIF v_quote1 = v_base2 THEN
        SET v_common = v_quote1;
        SET v_cross_base = v_base1;
        SET v_cross_quote = v_quote2;
    ELSEIF v_quote1 = v_quote2 THEN
        SET v_common = v_quote1;
        SET v_cross_base = v_base1;
        SET v_cross_quote = v_base2;
    ELSE
        SELECT NULL AS cross_pair, NULL AS cross_rate, NULL AS mid1, NULL AS mid2, NULL AS is_stale1, NULL AS is_stale2, NULL AS rate1_time, NULL AS rate2_time;
        LEAVE main_block;
    END IF;

    SELECT er.mid_rate, er.rate_timestamp,
           (er.rate_timestamp < (UTC_TIMESTAMP() - INTERVAL v_stale_minutes MINUTE)) AS is_stale
      INTO v_mid1, v_rate1_time, v_is_stale1
      FROM currency_pair cp
      LEFT JOIN exchange_rate er ON er.rate_id = (
        SELECT er2.rate_id FROM exchange_rate er2
         WHERE er2.pair_id = cp.pair_id AND er2.is_valid = TRUE
         ORDER BY er2.rate_timestamp DESC, er2.rate_id DESC LIMIT 1)
     WHERE cp.pair_code = p_pair1 AND cp.is_active = TRUE;

    SELECT er.mid_rate, er.rate_timestamp,
           (er.rate_timestamp < (UTC_TIMESTAMP() - INTERVAL v_stale_minutes MINUTE)) AS is_stale
      INTO v_mid2, v_rate2_time, v_is_stale2
      FROM currency_pair cp
      LEFT JOIN exchange_rate er ON er.rate_id = (
        SELECT er2.rate_id FROM exchange_rate er2
         WHERE er2.pair_id = cp.pair_id AND er2.is_valid = TRUE
         ORDER BY er2.rate_timestamp DESC, er2.rate_id DESC LIMIT 1)
     WHERE cp.pair_code = p_pair2 AND cp.is_active = TRUE;

    IF v_mid1 IS NULL OR v_mid2 IS NULL THEN
        SET v_cross_rate = NULL;
    ELSEIF v_common = v_quote1 AND v_common = v_base2 THEN
        SET v_cross_rate = v_mid1 * v_mid2;
    ELSEIF v_common = v_base1 AND v_common = v_quote2 THEN
        SET v_cross_rate = v_mid2 * v_mid1;
    ELSEIF v_common = v_quote1 AND v_common = v_quote2 THEN
        SET v_cross_rate = v_mid1 / v_mid2;
    ELSEIF v_common = v_base1 AND v_common = v_base2 THEN
        SET v_cross_rate = v_mid2 / v_mid1;
    ELSE
        SET v_cross_rate = NULL;
    END IF;

    SELECT CONCAT(v_cross_base, '/', v_cross_quote) AS cross_pair,
           v_cross_rate AS cross_rate,
           v_mid1 AS mid1,
           v_mid2 AS mid2,
           v_is_stale1 AS is_stale1,
           v_is_stale2 AS is_stale2,
           v_rate1_time AS rate1_time,
           v_rate2_time AS rate2_time;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `get_rate` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `get_rate`(
    IN p_pair_code VARCHAR(7),
    IN p_stale_minutes INT
)
BEGIN
    DECLARE v_stale_minutes INT DEFAULT 60;

    IF p_stale_minutes IS NOT NULL AND p_stale_minutes > 0 THEN
        SET v_stale_minutes = p_stale_minutes;
    END IF;

    SELECT
        cp.pair_code,
        er.mid_rate,
        er.rate_timestamp,
        CASE
            WHEN er.rate_timestamp IS NULL THEN NULL
            WHEN er.rate_timestamp < (UTC_TIMESTAMP() - INTERVAL v_stale_minutes MINUTE) THEN TRUE
            ELSE FALSE
        END AS is_stale,
        TIMESTAMPDIFF(MINUTE, er.rate_timestamp, UTC_TIMESTAMP()) AS age_minutes
    FROM currency_pair cp
    LEFT JOIN exchange_rate er
        ON er.rate_id = (
            SELECT er2.rate_id
            FROM exchange_rate er2
            WHERE er2.pair_id = cp.pair_id
              AND er2.is_valid = TRUE
            ORDER BY er2.rate_timestamp DESC, er2.rate_id DESC
            LIMIT 1
        )
    WHERE cp.pair_code = p_pair_code
      AND cp.is_active = TRUE;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `store_fixing` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `store_fixing`(
    IN p_fixing_id    INT,
    IN p_pair_id      INT,
    IN p_provider_id  INT,
    IN p_fixing_date  DATE,
    IN p_fixing_rate  DECIMAL(18,6),
    IN p_fixing_time  VARCHAR(10),
    IN p_fixing_type  VARCHAR(20),
    IN p_is_official  BOOLEAN,
    IN p_published_at TIMESTAMP,
    IN p_threshold    DECIMAL(5,4)
)
BEGIN
    DECLARE v_last_mid_rate  DECIMAL(18,6);
    DECLARE v_deviation_pct  DECIMAL(8,4);
    DECLARE v_threshold      DECIMAL(5,4);
 
    SET v_threshold = IFNULL(p_threshold, 0.01);
 
    SELECT mid_rate INTO v_last_mid_rate
    FROM exchange_rate
    WHERE pair_id = p_pair_id
      AND is_valid = 1
    ORDER BY rate_timestamp DESC
    LIMIT 1;
 
    IF v_last_mid_rate IS NOT NULL AND v_last_mid_rate != 0 THEN
        SET v_deviation_pct = ABS((p_fixing_rate - v_last_mid_rate) / v_last_mid_rate);
 
        IF v_deviation_pct > v_threshold THEN
            INSERT INTO rate_alert (
                alert_id,
                pair_id,
                alert_type,
                threshold_value,
                actual_value,
                alert_message,
                severity,
                triggered_at,
                status
            )
            VALUES (
                (SELECT IFNULL(MAX(alert_id), 0) + 1 FROM rate_alert ra),
                p_pair_id,
                'THRESHOLD_BREACH',
                v_threshold,
                v_deviation_pct,
                CONCAT('Fixing deviation alert: fixing=', p_fixing_rate,
                       ' lastTraded=', v_last_mid_rate,
                       ' deviation=', ROUND(v_deviation_pct * 100, 2), '%'),
                'WARNING',
                NOW(),
                'OPEN'
            );
        END IF;
    END IF;
 
    INSERT INTO eod_fixing (
        fixing_id,
        pair_id,
        provider_id,
        fixing_date,
        fixing_rate,
        fixing_time,
        fixing_type,
        is_official,
        published_at
    )
    VALUES (
        p_fixing_id,
        p_pair_id,
        p_provider_id,
        p_fixing_date,
        p_fixing_rate,
        p_fixing_time,
        p_fixing_type,
        p_is_official,
        p_published_at
    );
 
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `store_rate` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `store_rate`(
    IN p_rate_id INT,
    IN p_pair_id INT,
    IN p_provider_id INT,
    IN p_bid_rate DECIMAL(18,6),
    IN p_ask_rate DECIMAL(18,6),
    IN p_rate_timestamp TIMESTAMP,
    IN p_source_system VARCHAR(30)
)
BEGIN
    DECLARE v_mid_rate DECIMAL(18,6);

    IF p_bid_rate > p_ask_rate THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid rate: bid_rate cannot be greater than ask_rate';
    END IF;

    SET v_mid_rate = ROUND((p_bid_rate + p_ask_rate) / 2, 6);

    UPDATE exchange_rate
    SET is_stale = TRUE
    WHERE pair_id = p_pair_id
      AND is_valid = TRUE
      AND is_stale = FALSE;

    INSERT INTO exchange_rate (
        rate_id,
        pair_id,
        provider_id,
        bid_rate,
        ask_rate,
        mid_rate,
        rate_timestamp,
        source_system,
        is_valid,
        is_stale
    )
    VALUES (
        p_rate_id,
        p_pair_id,
        p_provider_id,
        p_bid_rate,
        p_ask_rate,
        v_mid_rate,
        p_rate_timestamp,
        p_source_system,
        TRUE,
        FALSE
    );
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Final view structure for view `latest_rates_vw`
--

/*!50001 DROP VIEW IF EXISTS `latest_rates_vw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `latest_rates_vw` AS select `cp`.`pair_code` AS `pair_code`,`cp`.`pair_type` AS `pair_type`,`er`.`mid_rate` AS `mid_rate`,`er`.`rate_timestamp` AS `rate_timestamp`,`cp`.`is_active` AS `is_active`,timestampdiff(HOUR,`er`.`rate_timestamp`,now()) AS `hours_since_update`,(case when (timestampdiff(HOUR,`er`.`rate_timestamp`,now()) > 4) then 1 else 0 end) AS `is_stale` from (`currency_pair` `cp` join `exchange_rate` `er` on((`er`.`rate_id` = (select `exchange_rate`.`rate_id` from `exchange_rate` where ((`exchange_rate`.`pair_id` = `cp`.`pair_id`) and (`exchange_rate`.`is_valid` = 1)) order by `exchange_rate`.`rate_timestamp` desc limit 1)))) where (`cp`.`is_active` = 1) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `rate_history_vw`
--

/*!50001 DROP VIEW IF EXISTS `rate_history_vw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `rate_history_vw` AS select `er`.`rate_id` AS `rate_id`,`er`.`pair_id` AS `pair_id`,`cp`.`pair_code` AS `pair_code`,`cp`.`base_currency` AS `base_currency`,`cp`.`quote_currency` AS `quote_currency`,`cp`.`pair_type` AS `pair_type`,`er`.`provider_id` AS `provider_id`,`rp`.`provider_code` AS `provider_code`,`rp`.`provider_name` AS `provider_name`,`er`.`bid_rate` AS `bid_rate`,`er`.`ask_rate` AS `ask_rate`,`er`.`mid_rate` AS `mid_rate`,round((`er`.`ask_rate` - `er`.`bid_rate`),6) AS `spread`,`er`.`rate_timestamp` AS `rate_timestamp`,`er`.`source_system` AS `source_system`,`er`.`is_valid` AS `is_valid` from ((`exchange_rate` `er` join `currency_pair` `cp` on((`er`.`pair_id` = `cp`.`pair_id`))) join `rate_provider` `rp` on((`er`.`provider_id` = `rp`.`provider_id`))) order by `er`.`rate_timestamp` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-17 10:00:57
