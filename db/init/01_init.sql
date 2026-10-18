
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

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sportmate` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `sportmate`;
DROP TABLE IF EXISTS `Chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Chat` (
  `ChatID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `UserID` int NOT NULL,
  `State` enum('active','ended') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `Text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `EditedAt` datetime DEFAULT NULL,
  PRIMARY KEY (`ChatID`),
  KEY `UserID` (`UserID`),
  KEY `idx_chat_post_time` (`PostID`,`Time`),
  CONSTRAINT `Chat_ibfk_1` FOREIGN KEY (`PostID`) REFERENCES `Post` (`PostID`),
  CONSTRAINT `Chat_ibfk_2` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `Chat` WRITE;
/*!40000 ALTER TABLE `Chat` DISABLE KEYS */;
INSERT INTO `Chat` VALUES (1,5,5,'active','ไปเลท 5 นาทีได้ไหมคะ','2026-08-25 11:02:00'),(2,5,3,'active','ได้คราฟ','2026-08-25 15:35:04'),(4,7,3,'active','test','2026-08-30 04:41:41'),(5,17,3,'active','เทสๆๆๆ','2026-09-02 07:16:27'),(6,17,3,'active','ต้องไม่เห็นน้า','2026-09-02 07:16:35');
/*!40000 ALTER TABLE `Chat` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `Event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Event` (
  `EventID` int NOT NULL AUTO_INCREMENT,
  `UserID` int NOT NULL,
  `PostID` int NOT NULL,
  `EventName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Status` enum('pending','approved','rejected','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `JoinDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `CancelDate` datetime DEFAULT NULL,
  PRIMARY KEY (`EventID`),
  UNIQUE KEY `uq_user_post` (`UserID`,`PostID`),
  KEY `idx_event_user` (`UserID`),
  KEY `idx_event_post` (`PostID`),
  CONSTRAINT `Event_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`),
  CONSTRAINT `Event_ibfk_2` FOREIGN KEY (`PostID`) REFERENCES `Post` (`PostID`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `Event` WRITE;
/*!40000 ALTER TABLE `Event` DISABLE KEYS */;
INSERT INTO `Event` VALUES (1,5,5,'หาเพื่อนซ้อมระบำใต้น้ำ','approved','2026-08-25 11:01:19',NULL),(2,1,8,'test review','approved','2026-08-30 05:20:46',NULL),(3,1,9,'ะำหะนะ','approved','2026-08-30 05:26:56',NULL),(4,7,11,'test review','approved','2026-08-30 05:42:22',NULL),(5,1,12,'test cancle','approved','2026-08-31 05:18:09',NULL),(6,1,14,'test cancle','approved','2026-08-31 05:33:30',NULL),(7,1,17,'demo','pending','2026-09-02 07:17:29',NULL),(8,1,16,'ะำหะนะ','cancelled','2026-09-02 07:46:38','2026-09-02 07:54:02'),(9,12,18,'iejmfkr','approved','2026-09-08 16:51:19',NULL),(10,10,22,'test posttt','approved','2026-09-10 10:16:44',NULL),(11,10,20,'test sort2','approved','2026-09-10 11:05:12',NULL);
/*!40000 ALTER TABLE `Event` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `Location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Location` (
  `LocationID` int NOT NULL AUTO_INCREMENT,
  `LocationName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`LocationID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `Location` WRITE;
/*!40000 ALTER TABLE `Location` DISABLE KEYS */;
INSERT INTO `Location` VALUES (1,'KMITL Stadium'),(2,'Ladkrabang Artificial Turf Football Field'),(3,'Sport Complex Badminton Court'),(4,'Central Swimming Pool'),(5,'Building A Multipurpose Area'),(6,'Public Park Near the University');
/*!40000 ALTER TABLE `Location` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `Notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Notification` (
  `NotificationID` int NOT NULL AUTO_INCREMENT,
  `UserID` int NOT NULL,
  `Message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Ntype` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `IsRead` tinyint(1) NOT NULL DEFAULT '0',
  `CreatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`NotificationID`),
  KEY `idx_notif_user_read` (`UserID`,`IsRead`,`CreatedAt`),
  CONSTRAINT `Notification_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `Notification` WRITE;
/*!40000 ALTER TABLE `Notification` DISABLE KEYS */;
INSERT INTO `Notification` VALUES (1,3,'prae เข้าร่วม \"หาเพื่อนซ้อมระบำใต้น้ำ\" (ตอนนี้ 1/10 ยังขาดอีก 2 คน)','/posts/5','join',1,'2026-08-25 11:01:19'),(2,7,'demo เข้าร่วม \"test review\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/8','join',1,'2026-08-30 05:20:46'),(3,7,'demo เข้าร่วม \"ะำหะนะ\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/9','join',1,'2026-08-30 05:26:56'),(4,2,'0432pattarapornprathumsuwan เข้าร่วม \"test review\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/11','join',1,'2026-08-30 05:42:22'),(5,7,'demo เข้าร่วม \"test cancle\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/12','join',1,'2026-08-31 05:18:09'),(6,7,'demo เข้าร่วม \"test cancle\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/14','join',1,'2026-08-31 05:33:30'),(7,3,'demo ขอเข้าร่วม \"demo\" (รอคุณอนุมัติ)','/posts/17','join_request',1,'2026-09-02 07:17:29'),(8,3,'demo เข้าร่วม \"ะำหะนะ\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/16','join',1,'2026-09-02 07:46:39'),(9,3,'demo ยกเลิกการเข้าร่วม \"ะำหะนะ\" (ตอนนี้ 0/10 ยังขาดอีก 4 คน)','/posts/16','cancel_join',1,'2026-09-02 07:54:02'),(10,10,'douyarawatashi เข้าร่วม \"iejmfkr\" (ตอนนี้ 1/10 ยังขาดอีก 2 คน)','/posts/18','join',1,'2026-09-08 16:51:19'),(11,10,'อีก 12 ชั่วโมง จะถึงเวลา \"iejmfkr\" ที่ KMITL Stadium','/posts/18','reminder',1,'2026-09-08 16:51:34'),(12,12,'อีก 12 ชั่วโมง จะถึงเวลา \"iejmfkr\" ที่ KMITL Stadium','/posts/18','reminder',1,'2026-09-08 16:51:34'),(13,10,'อีก 1 ชั่วโมง จะถึงเวลา \"iejmfkr\" ที่ KMITL Stadium','/posts/18','reminder',1,'2026-09-08 16:52:34'),(14,12,'อีก 1 ชั่วโมง จะถึงเวลา \"iejmfkr\" ที่ KMITL Stadium','/posts/18','reminder',1,'2026-09-08 16:52:34'),(15,2,'อีก 1 ชั่วโมง จะถึงเวลา \"test 3\" ที่ Central Swimming Pool','/posts/21','reminder',0,'2026-09-10 09:52:12'),(16,12,'อีก 24 ชั่วโมง จะถึงเวลา \"test posttt\" ที่ KMITL Stadium','/posts/22','reminder',1,'2026-09-10 10:16:42'),(17,12,'pttrpp2 เข้าร่วม \"test posttt\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/22','join',1,'2026-09-10 10:16:44'),(18,10,'ผู้จัดแก้ไขรายละเอียดกิจกรรม \"test posttt\" (วัน/เวลา หรือสถานที่เปลี่ยน)','/posts/22','post_updated',1,'2026-09-10 10:17:07'),(19,10,'กิจกรรม \"test posttt\" ถูกยกเลิกโดยผู้จัด','/posts/22','post_cancelled',1,'2026-09-10 10:17:34'),(20,2,'pttrpp2 เข้าร่วม \"test sort2\" (ตอนนี้ 1/134 ยังขาดอีก 3 คน)','/posts/20','join',0,'2026-09-10 11:05:12'),(21,2,'อีก 24 ชั่วโมง จะถึงเวลา \"test sort\" ที่ KMITL Stadium','/posts/19','reminder',0,'2026-09-11 14:37:44'),(22,2,'อีก 12 ชั่วโมง จะถึงเวลา \"test sort\" ที่ KMITL Stadium','/posts/19','reminder',0,'2026-09-12 01:57:33'),(23,2,'อีก 1 ชั่วโมง จะถึงเวลา \"test sort\" ที่ KMITL Stadium','/posts/19','reminder',0,'2026-09-12 12:48:26'),(24,10,'อีก 24 ชั่วโมง จะถึงเวลา \"test edit post\" ที่ Central Swimming Pool','/posts/23','reminder',1,'2026-09-13 18:59:09'),(25,10,'อีก 12 ชั่วโมง จะถึงเวลา \"test edit post\" ที่ Central Swimming Pool','/posts/23','reminder',1,'2026-09-14 06:58:34'),(26,2,'อีก 24 ชั่วโมง จะถึงเวลา \"test sort2\" ที่ Sport Complex Badminton Court','/posts/20','reminder',0,'2026-09-14 10:00:45'),(27,10,'อีก 24 ชั่วโมง จะถึงเวลา \"test sort2\" ที่ Sport Complex Badminton Court','/posts/20','reminder',1,'2026-09-14 10:00:45'),(28,10,'อีก 1 ชั่วโมง จะถึงเวลา \"test edit post\" ที่ Central Swimming Pool','/posts/23','reminder',1,'2026-09-14 17:58:53'),(29,2,'อีก 12 ชั่วโมง จะถึงเวลา \"test sort2\" ที่ Sport Complex Badminton Court','/posts/20','reminder',0,'2026-09-14 22:00:56'),(30,10,'อีก 12 ชั่วโมง จะถึงเวลา \"test sort2\" ที่ Sport Complex Badminton Court','/posts/20','reminder',1,'2026-09-14 22:00:56'),(31,2,'อีก 1 ชั่วโมง จะถึงเวลา \"test sort2\" ที่ Sport Complex Badminton Court','/posts/20','reminder',0,'2026-09-15 09:37:16'),(32,10,'อีก 1 ชั่วโมง จะถึงเวลา \"test sort2\" ที่ Sport Complex Badminton Court','/posts/20','reminder',1,'2026-09-15 09:37:16'),(33,10,'อีก 24 ชั่วโมง จะถึงเวลา \"ui testtt\" ที่ Central Swimming Pool','/posts/24','reminder',0,'2026-09-29 02:18:39'),(34,10,'อีก 12 ชั่วโมง จะถึงเวลา \"ui testtt\" ที่ Central Swimming Pool','/posts/24','reminder',0,'2026-09-29 13:32:43');
/*!40000 ALTER TABLE `Notification` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `Post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Post` (
  `PostID` int NOT NULL AUTO_INCREMENT,
  `OwnerUserID` int NOT NULL,
  `PostTypeID` int NOT NULL,
  `SportID` int NOT NULL,
  `LocationID` int NOT NULL,
  `PostName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `DatePlay` datetime NOT NULL,
  `OriginalDatePlay` datetime DEFAULT NULL,
  `DateCreate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PublishAt` datetime DEFAULT NULL,
  `NotifiedAt` datetime DEFAULT NULL,
  `EditCount` int NOT NULL DEFAULT '0',
  `MaxPlayer` int NOT NULL,
  `MinPlayer` int NOT NULL,
  `IsPublic` tinyint(1) NOT NULL DEFAULT '1',
  `Status` enum('open','closed','cancelled','finished') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'open',
  PRIMARY KEY (`PostID`),
  KEY `OwnerUserID` (`OwnerUserID`),
  KEY `PostTypeID` (`PostTypeID`),
  KEY `SportID` (`SportID`),
  KEY `LocationID` (`LocationID`),
  KEY `idx_post_status_publish` (`Status`,`PublishAt`),
  KEY `idx_post_notify` (`NotifiedAt`,`PublishAt`),
  CONSTRAINT `Post_ibfk_1` FOREIGN KEY (`OwnerUserID`) REFERENCES `User` (`UserID`),
  CONSTRAINT `Post_ibfk_2` FOREIGN KEY (`PostTypeID`) REFERENCES `PostType` (`PostTypeID`),
  CONSTRAINT `Post_ibfk_3` FOREIGN KEY (`SportID`) REFERENCES `Sport` (`SportID`),
  CONSTRAINT `Post_ibfk_4` FOREIGN KEY (`LocationID`) REFERENCES `Location` (`LocationID`),
  CONSTRAINT `Post_chk_1` CHECK ((`MaxPlayer` >= `MinPlayer`))
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `Post` WRITE;
/*!40000 ALTER TABLE `Post` DISABLE KEYS */;
INSERT INTO `Post` VALUES (1,2,1,3,1,'หาเพื่อนเตะบอล... 5v5 เย็นนี้','มาสนุกกันครับ ขาดอีก 4 คน','2026-07-23 09:48:52','2026-07-23 09:48:52','2026-07-22 09:48:52',NULL,'2026-07-22 09:48:52',0,10,6,1,'open'),(2,1,1,1,3,'แบดมินตันคู่ผสม','ระดับกลาง สนุก ๆ','2026-07-24 09:48:52','2026-07-24 09:48:52','2026-07-22 09:48:52',NULL,'2026-07-22 09:48:52',0,6,4,1,'open'),(3,1,1,4,6,'วิ่งเช้าเมื่อวาน (หมดเวลาแล้ว)','โพสต์นี้หมดเวลา ควรหายจากหน้า Post','2026-07-21 09:48:52','2026-07-21 09:48:52','2026-07-22 09:48:52',NULL,'2026-07-22 09:48:52',0,8,3,1,'open'),(4,2,2,2,1,'บาสเกตบอลทัวร์นาเมนต์ประจำเดือน','ทีมละ 5 คน ชิงถ้วยรางวัล','2026-07-29 09:48:52','2026-07-29 09:48:52','2026-07-22 09:48:52',NULL,'2026-07-22 09:48:52',0,40,10,1,'open'),(5,3,1,5,4,'หาเพื่อนซ้อมระบำใต้น้ำ','','2026-08-25 20:00:00','2026-08-25 20:00:00','2026-08-25 10:59:37',NULL,'2026-08-25 10:59:37',0,10,3,1,'open'),(6,3,1,4,1,'test post','private','2026-08-25 22:59:00','2026-08-25 22:59:00','2026-08-25 22:50:57',NULL,'2026-08-25 22:50:57',0,5,3,0,'open'),(7,3,1,3,1,'test comment','','2026-08-30 04:50:00','2026-08-30 04:50:00','2026-08-30 04:41:15',NULL,'2026-08-30 04:41:15',0,10,4,1,'open'),(8,7,1,3,1,'test review','','2026-08-30 05:21:00','2026-08-30 05:21:00','2026-08-30 05:20:41',NULL,'2026-08-30 05:20:41',0,10,4,1,'open'),(9,7,1,3,1,'ะำหะนะ','','2026-08-30 05:27:00','2026-08-30 05:27:00','2026-08-30 05:26:49',NULL,'2026-08-30 05:26:49',0,10,4,1,'open'),(10,2,1,3,1,'test review','','2026-08-30 05:42:00','2026-08-30 05:42:00','2026-08-30 05:41:55',NULL,'2026-08-30 05:41:55',0,10,4,1,'open'),(11,2,1,3,1,'test review','','2026-08-30 05:43:00','2026-08-30 05:43:00','2026-08-30 05:42:17',NULL,'2026-08-30 05:42:17',0,10,4,1,'open'),(12,7,1,3,1,'test cancle','','2026-08-31 05:20:00','2026-08-31 05:20:00','2026-08-31 05:17:34',NULL,'2026-08-31 05:17:34',0,10,4,1,'open'),(13,1,1,3,1,'test cancle','','2026-09-01 07:18:00','2026-09-01 07:18:00','2026-08-31 05:18:42',NULL,'2026-08-31 05:18:42',0,10,4,1,'cancelled'),(14,7,2,3,1,'test cancle','','2026-08-31 05:39:00','2026-08-31 05:39:00','2026-08-31 05:33:09',NULL,'2026-08-31 05:33:09',0,10,4,1,'open'),(15,7,1,3,1,'test edit post','','2026-08-31 05:43:00','2026-08-31 05:43:00','2026-08-31 05:41:28',NULL,'2026-08-31 05:41:28',0,10,4,1,'open'),(16,3,1,3,1,'ะำหะนะ','','2026-09-04 06:45:00','2026-09-04 06:45:00','2026-09-02 06:45:59',NULL,'2026-09-02 06:45:59',0,10,4,1,'open'),(17,3,1,3,1,'demo','','2026-09-04 07:16:00','2026-09-04 07:16:00','2026-09-02 07:16:16',NULL,'2026-09-02 07:16:16',0,10,4,0,'open'),(18,10,1,3,1,'iejmfkr','','2026-09-08 17:53:00','2026-09-08 17:53:00','2026-09-08 16:50:35',NULL,'2026-09-08 16:50:35',0,10,3,1,'open'),(19,2,2,3,1,'test sort','','2026-09-12 13:49:00','2026-09-12 13:49:00','2026-09-10 09:49:51',NULL,'2026-09-10 09:49:51',0,10,4,1,'open'),(20,2,2,1,3,'test sort2','','2026-09-15 09:50:00','2026-09-15 09:50:00','2026-09-10 09:50:40',NULL,'2026-09-10 09:50:40',0,134,4,1,'open'),(21,2,2,5,4,'test 3','','2026-09-10 10:51:00','2026-09-10 10:51:00','2026-09-10 09:51:14',NULL,'2026-09-10 09:51:14',0,10,4,1,'open'),(22,12,1,2,1,'test posttt','','2026-09-12 10:16:00','2026-09-12 10:16:00','2026-09-10 10:16:29',NULL,'2026-09-10 10:16:29',0,10,4,1,'cancelled'),(23,10,1,2,4,'test edit post','','2026-09-14 18:59:00','2026-09-11 18:59:00','2026-09-10 18:59:50',NULL,'2026-09-10 18:59:50',1,10,4,1,'open'),(24,10,1,4,4,'ui testtt','','2026-09-30 01:33:00','2026-09-30 01:33:00','2026-09-28 01:33:29',NULL,'2026-09-28 01:33:29',0,10,4,1,'open');
/*!40000 ALTER TABLE `Post` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `PostReminder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PostReminder` (
  `ReminderID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `HoursBefore` int NOT NULL,
  `SentAt` datetime NOT NULL,
  PRIMARY KEY (`ReminderID`),
  UNIQUE KEY `uq_post_stage` (`PostID`,`HoursBefore`),
  CONSTRAINT `PostReminder_ibfk_1` FOREIGN KEY (`PostID`) REFERENCES `Post` (`PostID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `PostReminder` WRITE;
/*!40000 ALTER TABLE `PostReminder` DISABLE KEYS */;
INSERT INTO `PostReminder` VALUES (1,18,24,'2026-09-08 16:51:34'),(2,18,12,'2026-09-08 16:51:34'),(3,18,1,'2026-09-08 16:52:34'),(4,21,24,'2026-09-10 09:52:12'),(5,21,12,'2026-09-10 09:52:12'),(6,21,1,'2026-09-10 09:52:12'),(7,22,24,'2026-09-10 10:16:42'),(8,19,24,'2026-09-11 14:37:44'),(9,19,12,'2026-09-12 01:57:33'),(10,19,1,'2026-09-12 12:48:26'),(11,23,24,'2026-09-13 18:59:09'),(12,23,12,'2026-09-14 06:58:34'),(13,20,24,'2026-09-14 10:00:45'),(14,23,1,'2026-09-14 17:58:53'),(15,20,12,'2026-09-14 22:00:56'),(16,20,1,'2026-09-15 09:37:16'),(17,24,24,'2026-09-29 02:18:39'),(18,24,12,'2026-09-29 13:32:43');
/*!40000 ALTER TABLE `PostReminder` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `PostType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PostType` (
  `PostTypeID` int NOT NULL AUTO_INCREMENT,
  `PtypeName` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`PostTypeID`),
  UNIQUE KEY `PtypeName` (`PtypeName`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `PostType` WRITE;
/*!40000 ALTER TABLE `PostType` DISABLE KEYS */;
INSERT INTO `PostType` VALUES (1,'Post'),(2,'Tournament');
/*!40000 ALTER TABLE `PostType` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `Receipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Receipt` (
  `PlaymentID` int NOT NULL AUTO_INCREMENT,
  `UserID` int NOT NULL,
  `DatePlayment` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PlaymentAmount` decimal(10,2) NOT NULL,
  `QR` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`PlaymentID`),
  KEY `UserID` (`UserID`),
  CONSTRAINT `Receipt_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `Receipt` WRITE;
/*!40000 ALTER TABLE `Receipt` DISABLE KEYS */;
INSERT INTO `Receipt` VALUES (1,7,'2026-08-31 05:32:52',99.00,'**** **** **** 1111'),(2,2,'2026-09-10 09:48:06',99.00,'**** **** **** 1111');
/*!40000 ALTER TABLE `Receipt` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `Review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Review` (
  `ReviewID` int NOT NULL AUTO_INCREMENT,
  `EventID` int NOT NULL,
  `ReviewScore` tinyint NOT NULL,
  `Comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `ReviewDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Direction` enum('to_owner','to_participant') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'to_owner',
  PRIMARY KEY (`ReviewID`),
  UNIQUE KEY `uq_review_event_direction` (`EventID`,`Direction`),
  CONSTRAINT `Review_ibfk_1` FOREIGN KEY (`EventID`) REFERENCES `Event` (`EventID`),
  CONSTRAINT `Review_chk_1` CHECK ((`ReviewScore` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `Review` WRITE;
/*!40000 ALTER TABLE `Review` DISABLE KEYS */;
INSERT INTO `Review` VALUES (1,2,5,NULL,'2026-08-30 05:21:50','to_participant'),(2,3,1,'ไม่มานัดนะน้อง','2026-08-30 05:27:39','to_participant'),(3,4,5,'เจ๋งแจ๋วสุดจ๊าบนะ','2026-08-30 05:43:23','to_participant');
/*!40000 ALTER TABLE `Review` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = latin1 */ ;
/*!50003 SET character_set_results = latin1 */ ;
/*!50003 SET collation_connection  = latin1_swedish_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 */ /*!50003 TRIGGER `trg_review_after_insert` AFTER INSERT ON `Review` FOR EACH ROW BEGIN
    DECLARE v_owner INT;

    
    IF NEW.Direction = 'to_owner' THEN
        SELECT p.OwnerUserID INTO v_owner
        FROM Event e
        JOIN Post p ON e.PostID = p.PostID
        WHERE e.EventID = NEW.EventID;

        UPDATE `User` u
        SET u.AvgScore = (
            SELECT AVG(r.ReviewScore)
            FROM Review r
            JOIN Event e2 ON r.EventID = e2.EventID
            JOIN Post p2 ON e2.PostID = p2.PostID
            WHERE p2.OwnerUserID = v_owner
              AND r.Direction = 'to_owner'
        )
        WHERE u.UserID = v_owner;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
DROP TABLE IF EXISTS `Sport`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Sport` (
  `SportID` int NOT NULL AUTO_INCREMENT,
  `SportName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`SportID`),
  UNIQUE KEY `SportName` (`SportName`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `Sport` WRITE;
/*!40000 ALTER TABLE `Sport` DISABLE KEYS */;
INSERT INTO `Sport` VALUES (3,'Badminton'),(2,'Basketball'),(1,'Football'),(4,'Running'),(5,'Swimming');
/*!40000 ALTER TABLE `Sport` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `User`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `User` (
  `UserID` int NOT NULL AUTO_INCREMENT,
  `UserName` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PhoneNumber` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Gmail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `UserTypeID` int NOT NULL,
  `AvgScore` decimal(3,2) NOT NULL DEFAULT '0.00',
  `IsEmailVerified` tinyint(1) NOT NULL DEFAULT '0',
  `OtpCode` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `OtpExpireAt` datetime DEFAULT NULL,
  `FailedLoginCount` int NOT NULL DEFAULT '0',
  `LockUntil` datetime DEFAULT NULL,
  `LastActivityAt` datetime DEFAULT NULL,
  `MembershipExpireAt` datetime DEFAULT NULL,
  `CreatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `AuthProvider` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'local',
  `ProviderId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `AvatarPath` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`UserID`),
  UNIQUE KEY `UserName` (`UserName`),
  UNIQUE KEY `Gmail` (`Gmail`),
  UNIQUE KEY `uq_provider` (`AuthProvider`,`ProviderId`),
  KEY `UserTypeID` (`UserTypeID`),
  CONSTRAINT `User_ibfk_1` FOREIGN KEY (`UserTypeID`) REFERENCES `UserType` (`UserTypeID`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `User` WRITE;
/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO `User` VALUES (1,'demo','$2a$10$.Pb8p2kJlOh02rfBZY1jSeQJv2Q1J/OmB5p5bJ23sQEXi0aj25ufm','0800000000','user1@sportmate.test',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-07-22 09:48:52','local',NULL,NULL),(2,'member','$2a$10$.Pb8p2kJlOh02rfBZY1jSeQJv2Q1J/OmB5p5bJ23sQEXi0aj25ufm','0800000000','user2@sportmate.test',2,0.00,1,NULL,NULL,0,NULL,NULL,'2026-10-10 09:48:06','2026-07-22 09:48:52','local',NULL,NULL),(3,'pttrpp',NULL,'0800000000','user3@sportmate.test',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-25 07:38:06','google',NULL,NULL),(4,'สมหญิงเทส',NULL,'0800000000','user4@sportmate.test',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-25 07:38:39','thaid',NULL,NULL),(5,'prae',NULL,'0800000000','user5@sportmate.test',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-25 11:01:14','google',NULL,NULL),(6,'testkun','$2a$10$.Pb8p2kJlOh02rfBZY1jSeQJv2Q1J/OmB5p5bJ23sQEXi0aj25ufm','0800000000','user6@sportmate.test',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-25 21:54:11','local',NULL,NULL),(7,'0432pattarapornprathumsuwan',NULL,'0800000000','user7@sportmate.test',2,0.00,1,NULL,NULL,0,NULL,NULL,'2026-09-30 05:32:52','2026-08-30 05:19:46','google',NULL,NULL),(8,'twst','$2a$10$.Pb8p2kJlOh02rfBZY1jSeQJv2Q1J/OmB5p5bJ23sQEXi0aj25ufm','0800000000','user8@sportmate.test',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-31 06:13:17','local',NULL,NULL),(9,'admin','$2a$10$QdWWTX2rqn1.IBspBpyeIOoPCKiB5n9JRHBJu08Lk6xiGEf8zuR9y',NULL,'admin@sportmate.local',3,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-09-02 16:19:23','local',NULL,NULL),(10,'pttrpp2',NULL,NULL,'pattarapornas2119@gmail.com',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-09-08 08:23:43','google','111365608985921890151',NULL),(11,'testpass','$2a$10$.Pb8p2kJlOh02rfBZY1jSeQJv2Q1J/OmB5p5bJ23sQEXi0aj25ufm',NULL,'testpass@sportmate.test',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-09-08 08:31:23','local',NULL,NULL),(12,'douyarawatashi',NULL,NULL,'numary.ragalia@gmail.com',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-09-08 16:51:15','google','110586939501125641739',NULL);
/*!40000 ALTER TABLE `User` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `UserSport`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `UserSport` (
  `UserID` int NOT NULL,
  `SportID` int NOT NULL,
  PRIMARY KEY (`UserID`,`SportID`),
  KEY `SportID` (`SportID`),
  CONSTRAINT `UserSport_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`) ON DELETE CASCADE,
  CONSTRAINT `UserSport_ibfk_2` FOREIGN KEY (`SportID`) REFERENCES `Sport` (`SportID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `UserSport` WRITE;
/*!40000 ALTER TABLE `UserSport` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserSport` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `UserType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `UserType` (
  `UserTypeID` int NOT NULL AUTO_INCREMENT,
  `UTypeName` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`UserTypeID`),
  UNIQUE KEY `UTypeName` (`UTypeName`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `UserType` WRITE;
/*!40000 ALTER TABLE `UserType` DISABLE KEYS */;
INSERT INTO `UserType` VALUES (3,'Admin'),(2,'Member'),(1,'Normal');
/*!40000 ALTER TABLE `UserType` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


-- ===== ต่อท้าย: ระบบทีม + สายการแข่ง (ดู db/migrations/2026-10-19_tournament_teams.sql) =====
ALTER TABLE `Post`
  ADD COLUMN `TeamSize` int NOT NULL DEFAULT 1 AFTER `MinPlayer`;

-- 2) ทีมในทัวร์นาเมนต์ (หัวหน้าทีม = คนขอสร้างทีม, เจ้าของทัวร์นาเมนต์เป็นคนอนุมัติ)
CREATE TABLE `Team` (
  `TeamID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `CaptainUserID` int NOT NULL,
  `TeamName` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `Note` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Status` enum('pending','approved','rejected','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `CreatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`TeamID`),
  KEY `idx_team_post` (`PostID`,`Status`),
  KEY `idx_team_captain` (`CaptainUserID`),
  CONSTRAINT `Team_ibfk_1` FOREIGN KEY (`PostID`) REFERENCES `Post` (`PostID`),
  CONSTRAINT `Team_ibfk_2` FOREIGN KEY (`CaptainUserID`) REFERENCES `User` (`UserID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) สมาชิกทีม (หัวหน้าทีมอนุมัติ) — หัวหน้าทีมก็มีแถวของตัวเองด้วย จึงนับรวมได้ตรง ๆ
CREATE TABLE `TeamMember` (
  `TeamMemberID` int NOT NULL AUTO_INCREMENT,
  `TeamID` int NOT NULL,
  `UserID` int NOT NULL,
  `Message` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Status` enum('pending','approved','rejected','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `JoinDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `CancelDate` datetime DEFAULT NULL,
  PRIMARY KEY (`TeamMemberID`),
  UNIQUE KEY `uq_team_user` (`TeamID`,`UserID`),
  KEY `idx_tm_user` (`UserID`),
  CONSTRAINT `TeamMember_ibfk_1` FOREIGN KEY (`TeamID`) REFERENCES `Team` (`TeamID`),
  CONSTRAINT `TeamMember_ibfk_2` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4) คู่การแข่งขันในสาย (Round 1 = รอบแรก, Slot = ลำดับคู่ในรอบนั้น เริ่มที่ 0)
--    TeamBID ว่าง + Status 'bye' = ไม่มีคู่ เข้ารอบต่อไปอัตโนมัติ
CREATE TABLE `TournamentMatch` (
  `MatchID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `Round` int NOT NULL,
  `Slot` int NOT NULL,
  `TeamAID` int DEFAULT NULL,
  `TeamBID` int DEFAULT NULL,
  `ScoreA` int DEFAULT NULL,
  `ScoreB` int DEFAULT NULL,
  `WinnerTeamID` int DEFAULT NULL,
  `Status` enum('waiting','ready','done','bye') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'waiting',
  `PlayedAt` datetime DEFAULT NULL,
  PRIMARY KEY (`MatchID`),
  UNIQUE KEY `uq_match_slot` (`PostID`,`Round`,`Slot`),
  CONSTRAINT `TournamentMatch_ibfk_1` FOREIGN KEY (`PostID`) REFERENCES `Post` (`PostID`),
  CONSTRAINT `TournamentMatch_ibfk_2` FOREIGN KEY (`TeamAID`) REFERENCES `Team` (`TeamID`),
  CONSTRAINT `TournamentMatch_ibfk_3` FOREIGN KEY (`TeamBID`) REFERENCES `Team` (`TeamID`),
  CONSTRAINT `TournamentMatch_ibfk_4` FOREIGN KEY (`WinnerTeamID`) REFERENCES `Team` (`TeamID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5) แชททีม: TeamID ว่าง = แชทรวมของโพสต์ (เหมือนเดิม), มีค่า = แชทของทีมนั้น
ALTER TABLE `Chat`
  ADD COLUMN `TeamID` int DEFAULT NULL AFTER `UserID`,
  ADD KEY `idx_chat_team_time` (`TeamID`,`Time`),
  ADD CONSTRAINT `Chat_ibfk_3` FOREIGN KEY (`TeamID`) REFERENCES `Team` (`TeamID`);
