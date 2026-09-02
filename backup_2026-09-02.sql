-- MySQL dump 10.13  Distrib 8.0.46, for Linux (aarch64)
--
-- Host: localhost    Database: sportmate
-- ------------------------------------------------------
-- Server version	8.0.46

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
-- Table structure for table `Chat`
--

DROP TABLE IF EXISTS `Chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Chat` (
  `ChatID` int NOT NULL AUTO_INCREMENT,
  `PostID` int NOT NULL,
  `UserID` int NOT NULL,
  `State` enum('active','ended') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `Text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `Time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ChatID`),
  KEY `UserID` (`UserID`),
  KEY `idx_chat_post_time` (`PostID`,`Time`),
  CONSTRAINT `Chat_ibfk_1` FOREIGN KEY (`PostID`) REFERENCES `Post` (`PostID`),
  CONSTRAINT `Chat_ibfk_2` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Chat`
--

LOCK TABLES `Chat` WRITE;
/*!40000 ALTER TABLE `Chat` DISABLE KEYS */;
INSERT INTO `Chat` VALUES (1,5,5,'active','ไปเลท 5 นาทีได้ไหมคะ','2026-08-25 11:02:00'),(2,5,3,'active','ได้คราฟ','2026-08-25 15:35:04'),(4,7,3,'active','test','2026-08-30 04:41:41'),(5,17,3,'active','เทสๆๆๆ','2026-09-02 07:16:27'),(6,17,3,'active','ต้องไม่เห็นน้า','2026-09-02 07:16:35');
/*!40000 ALTER TABLE `Chat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Event`
--

DROP TABLE IF EXISTS `Event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Event` (
  `EventID` int NOT NULL AUTO_INCREMENT,
  `UserID` int NOT NULL,
  `PostID` int NOT NULL,
  `EventName` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Status` enum('pending','approved','rejected','cancelled') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `JoinDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `CancelDate` datetime DEFAULT NULL,
  PRIMARY KEY (`EventID`),
  UNIQUE KEY `uq_user_post` (`UserID`,`PostID`),
  KEY `idx_event_user` (`UserID`),
  KEY `idx_event_post` (`PostID`),
  CONSTRAINT `Event_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`),
  CONSTRAINT `Event_ibfk_2` FOREIGN KEY (`PostID`) REFERENCES `Post` (`PostID`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Event`
--

LOCK TABLES `Event` WRITE;
/*!40000 ALTER TABLE `Event` DISABLE KEYS */;
INSERT INTO `Event` VALUES (1,5,5,'หาเพื่อนซ้อมระบำใต้น้ำ','approved','2026-08-25 11:01:19',NULL),(2,1,8,'test review','approved','2026-08-30 05:20:46',NULL),(3,1,9,'ะำหะนะ','approved','2026-08-30 05:26:56',NULL),(4,7,11,'test review','approved','2026-08-30 05:42:22',NULL),(5,1,12,'test cancle','approved','2026-08-31 05:18:09',NULL),(6,1,14,'test cancle','approved','2026-08-31 05:33:30',NULL),(7,1,17,'demo','pending','2026-09-02 07:17:29',NULL),(8,1,16,'ะำหะนะ','cancelled','2026-09-02 07:46:38','2026-09-02 07:54:02');
/*!40000 ALTER TABLE `Event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Location`
--

DROP TABLE IF EXISTS `Location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Location` (
  `LocationID` int NOT NULL AUTO_INCREMENT,
  `LocationName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`LocationID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Location`
--

LOCK TABLES `Location` WRITE;
/*!40000 ALTER TABLE `Location` DISABLE KEYS */;
INSERT INTO `Location` VALUES (1,'KMITL Stadium'),(2,'Ladkrabang Artificial Turf Football Field'),(3,'Sport Complex Badminton Court'),(4,'Central Swimming Pool'),(5,'Building A Multipurpose Area'),(6,'Public Park Near the University');
/*!40000 ALTER TABLE `Location` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Notification`
--

DROP TABLE IF EXISTS `Notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Notification` (
  `NotificationID` int NOT NULL AUTO_INCREMENT,
  `UserID` int NOT NULL,
  `Message` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Link` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Ntype` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `IsRead` tinyint(1) NOT NULL DEFAULT '0',
  `CreatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`NotificationID`),
  KEY `idx_notif_user_read` (`UserID`,`IsRead`,`CreatedAt`),
  CONSTRAINT `Notification_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Notification`
--

LOCK TABLES `Notification` WRITE;
/*!40000 ALTER TABLE `Notification` DISABLE KEYS */;
INSERT INTO `Notification` VALUES (1,3,'prae เข้าร่วม \"หาเพื่อนซ้อมระบำใต้น้ำ\" (ตอนนี้ 1/10 ยังขาดอีก 2 คน)','/posts/5','join',1,'2026-08-25 11:01:19'),(2,7,'demo เข้าร่วม \"test review\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/8','join',1,'2026-08-30 05:20:46'),(3,7,'demo เข้าร่วม \"ะำหะนะ\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/9','join',1,'2026-08-30 05:26:56'),(4,2,'0432pattarapornprathumsuwan เข้าร่วม \"test review\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/11','join',1,'2026-08-30 05:42:22'),(5,7,'demo เข้าร่วม \"test cancle\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/12','join',1,'2026-08-31 05:18:09'),(6,7,'demo เข้าร่วม \"test cancle\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/14','join',1,'2026-08-31 05:33:30'),(7,3,'demo ขอเข้าร่วม \"demo\" (รอคุณอนุมัติ)','/posts/17','join_request',1,'2026-09-02 07:17:29'),(8,3,'demo เข้าร่วม \"ะำหะนะ\" (ตอนนี้ 1/10 ยังขาดอีก 3 คน)','/posts/16','join',0,'2026-09-02 07:46:39'),(9,3,'demo ยกเลิกการเข้าร่วม \"ะำหะนะ\" (ตอนนี้ 0/10 ยังขาดอีก 4 คน)','/posts/16','cancel_join',0,'2026-09-02 07:54:02');
/*!40000 ALTER TABLE `Notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Post`
--

DROP TABLE IF EXISTS `Post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Post` (
  `PostID` int NOT NULL AUTO_INCREMENT,
  `OwnerUserID` int NOT NULL,
  `PostTypeID` int NOT NULL,
  `SportID` int NOT NULL,
  `LocationID` int NOT NULL,
  `PostName` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Description` text COLLATE utf8mb4_unicode_ci,
  `DatePlay` datetime NOT NULL,
  `DateCreate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PublishAt` datetime DEFAULT NULL,
  `MaxPlayer` int NOT NULL,
  `MinPlayer` int NOT NULL,
  `IsPublic` tinyint(1) NOT NULL DEFAULT '1',
  `Status` enum('open','closed','cancelled','finished') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'open',
  PRIMARY KEY (`PostID`),
  KEY `OwnerUserID` (`OwnerUserID`),
  KEY `PostTypeID` (`PostTypeID`),
  KEY `SportID` (`SportID`),
  KEY `LocationID` (`LocationID`),
  KEY `idx_post_status_publish` (`Status`,`PublishAt`),
  CONSTRAINT `Post_ibfk_1` FOREIGN KEY (`OwnerUserID`) REFERENCES `User` (`UserID`),
  CONSTRAINT `Post_ibfk_2` FOREIGN KEY (`PostTypeID`) REFERENCES `PostType` (`PostTypeID`),
  CONSTRAINT `Post_ibfk_3` FOREIGN KEY (`SportID`) REFERENCES `Sport` (`SportID`),
  CONSTRAINT `Post_ibfk_4` FOREIGN KEY (`LocationID`) REFERENCES `Location` (`LocationID`),
  CONSTRAINT `Post_chk_1` CHECK ((`MaxPlayer` >= `MinPlayer`))
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Post`
--

LOCK TABLES `Post` WRITE;
/*!40000 ALTER TABLE `Post` DISABLE KEYS */;
INSERT INTO `Post` VALUES (1,2,1,3,1,'หาเพื่อนเตะบอล 5v5 เย็นนี้','มาสนุกกันครับ ขาดอีก 4 คน','2026-07-23 09:48:52','2026-07-22 09:48:52',NULL,10,6,1,'open'),(2,1,1,1,3,'แบดมินตันคู่ผสม','ระดับกลาง สนุก ๆ','2026-07-24 09:48:52','2026-07-22 09:48:52',NULL,6,4,1,'open'),(3,1,1,4,6,'วิ่งเช้าเมื่อวาน (หมดเวลาแล้ว)','โพสต์นี้หมดเวลา ควรหายจากหน้า Post','2026-07-21 09:48:52','2026-07-22 09:48:52',NULL,8,3,1,'open'),(4,2,2,2,1,'บาสเกตบอลทัวร์นาเมนต์ประจำเดือน','ทีมละ 5 คน ชิงถ้วยรางวัล','2026-07-29 09:48:52','2026-07-22 09:48:52',NULL,40,10,1,'open'),(5,3,1,5,4,'หาเพื่อนซ้อมระบำใต้น้ำ','','2026-08-25 20:00:00','2026-08-25 10:59:37',NULL,10,3,1,'open'),(6,3,1,4,1,'test post','private','2026-08-25 22:59:00','2026-08-25 22:50:57',NULL,5,3,0,'open'),(7,3,1,3,1,'test comment','','2026-08-30 04:50:00','2026-08-30 04:41:15',NULL,10,4,1,'open'),(8,7,1,3,1,'test review','','2026-08-30 05:21:00','2026-08-30 05:20:41',NULL,10,4,1,'open'),(9,7,1,3,1,'ะำหะนะ','','2026-08-30 05:27:00','2026-08-30 05:26:49',NULL,10,4,1,'open'),(10,2,1,3,1,'test review','','2026-08-30 05:42:00','2026-08-30 05:41:55',NULL,10,4,1,'open'),(11,2,1,3,1,'test review','','2026-08-30 05:43:00','2026-08-30 05:42:17',NULL,10,4,1,'open'),(12,7,1,3,1,'test cancle','','2026-08-31 05:20:00','2026-08-31 05:17:34',NULL,10,4,1,'open'),(13,1,1,3,1,'test cancle','','2026-09-01 07:18:00','2026-08-31 05:18:42',NULL,10,4,1,'cancelled'),(14,7,2,3,1,'test cancle','','2026-08-31 05:39:00','2026-08-31 05:33:09',NULL,10,4,1,'open'),(15,7,1,3,1,'test edit post','','2026-08-31 05:43:00','2026-08-31 05:41:28',NULL,10,4,1,'open'),(16,3,1,3,1,'ะำหะนะ','','2026-09-04 06:45:00','2026-09-02 06:45:59',NULL,10,4,1,'open'),(17,3,1,3,1,'demo','','2026-09-04 07:16:00','2026-09-02 07:16:16',NULL,10,4,0,'open');
/*!40000 ALTER TABLE `Post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PostType`
--

DROP TABLE IF EXISTS `PostType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PostType` (
  `PostTypeID` int NOT NULL AUTO_INCREMENT,
  `PtypeName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`PostTypeID`),
  UNIQUE KEY `PtypeName` (`PtypeName`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PostType`
--

LOCK TABLES `PostType` WRITE;
/*!40000 ALTER TABLE `PostType` DISABLE KEYS */;
INSERT INTO `PostType` VALUES (1,'Post'),(2,'Tournament');
/*!40000 ALTER TABLE `PostType` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Receipt`
--

DROP TABLE IF EXISTS `Receipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Receipt` (
  `PlaymentID` int NOT NULL AUTO_INCREMENT,
  `UserID` int NOT NULL,
  `DatePlayment` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PlaymentAmount` decimal(10,2) NOT NULL,
  `QR` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`PlaymentID`),
  KEY `UserID` (`UserID`),
  CONSTRAINT `Receipt_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `User` (`UserID`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Receipt`
--

LOCK TABLES `Receipt` WRITE;
/*!40000 ALTER TABLE `Receipt` DISABLE KEYS */;
INSERT INTO `Receipt` VALUES (1,7,'2026-08-31 05:32:52',99.00,'**** **** **** 1111');
/*!40000 ALTER TABLE `Receipt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Review`
--

DROP TABLE IF EXISTS `Review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Review` (
  `ReviewID` int NOT NULL AUTO_INCREMENT,
  `EventID` int NOT NULL,
  `ReviewScore` tinyint NOT NULL,
  `Comment` text COLLATE utf8mb4_unicode_ci,
  `ReviewDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Direction` enum('to_owner','to_participant') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'to_owner',
  PRIMARY KEY (`ReviewID`),
  UNIQUE KEY `uq_review_event_direction` (`EventID`,`Direction`),
  CONSTRAINT `Review_ibfk_1` FOREIGN KEY (`EventID`) REFERENCES `Event` (`EventID`),
  CONSTRAINT `Review_chk_1` CHECK ((`ReviewScore` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Review`
--

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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_review_after_insert` AFTER INSERT ON `Review` FOR EACH ROW BEGIN
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

--
-- Table structure for table `Sport`
--

DROP TABLE IF EXISTS `Sport`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Sport` (
  `SportID` int NOT NULL AUTO_INCREMENT,
  `SportName` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`SportID`),
  UNIQUE KEY `SportName` (`SportName`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Sport`
--

LOCK TABLES `Sport` WRITE;
/*!40000 ALTER TABLE `Sport` DISABLE KEYS */;
INSERT INTO `Sport` VALUES (3,'Badminton'),(2,'Basketball'),(1,'Football'),(4,'Running'),(5,'Swimming');
/*!40000 ALTER TABLE `Sport` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `User`
--

DROP TABLE IF EXISTS `User`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `User` (
  `UserID` int NOT NULL AUTO_INCREMENT,
  `UserName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PhoneNumber` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Gmail` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `UserTypeID` int NOT NULL,
  `AvgScore` decimal(3,2) NOT NULL DEFAULT '0.00',
  `IsEmailVerified` tinyint(1) NOT NULL DEFAULT '0',
  `OtpCode` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `OtpExpireAt` datetime DEFAULT NULL,
  `FailedLoginCount` int NOT NULL DEFAULT '0',
  `LockUntil` datetime DEFAULT NULL,
  `LastActivityAt` datetime DEFAULT NULL,
  `MembershipExpireAt` datetime DEFAULT NULL,
  `CreatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `AuthProvider` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'local',
  `ProviderId` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`UserID`),
  UNIQUE KEY `UserName` (`UserName`),
  UNIQUE KEY `Gmail` (`Gmail`),
  UNIQUE KEY `uq_provider` (`AuthProvider`,`ProviderId`),
  KEY `UserTypeID` (`UserTypeID`),
  CONSTRAINT `User_ibfk_1` FOREIGN KEY (`UserTypeID`) REFERENCES `UserType` (`UserTypeID`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `User`
--

LOCK TABLES `User` WRITE;
/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO `User` VALUES (1,'demo','$2a$10$l0B9iIIMYqRVbko1DyT5/eCxc1Ld6aTCUagFPYB/bssAtOluUa18e',NULL,'demo@example.com',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-07-22 09:48:52','local',NULL),(2,'member','$2a$10$d.lRDPzg6Md6x/cXKleTa.E/QvXgIhVFDWDMWG/oI1S/PknmW7PfC',NULL,'member@example.com',2,0.00,1,NULL,NULL,0,NULL,NULL,'2026-08-22 09:48:52','2026-07-22 09:48:52','local',NULL),(3,'pttrpp',NULL,NULL,'pattarapornas2119@gmail.com',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-25 07:38:06','google','111365608985921890151'),(4,'สมหญิงเทส',NULL,NULL,'thaid_1111111111111@no-email.sportmate.local',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-25 07:38:39','thaid','1111111111111'),(5,'prae',NULL,NULL,'thunrada12092548@gmail.com',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-25 11:01:14','google','110626182276584414169'),(6,'testkun','$2a$10$llEzKXDC0VVfsm0KS/4Gfed9ZzzPFs0vCDgMNZ6fah3gaJG1O9eaO',NULL,'pattarapornas777@gmail.com',1,0.00,0,'313840','2026-08-25 22:04:34',0,NULL,NULL,NULL,'2026-08-25 21:54:11','local',NULL),(7,'0432pattarapornprathumsuwan',NULL,NULL,'67050432@kmitl.ac.th',2,0.00,1,NULL,NULL,0,NULL,NULL,'2026-09-30 05:32:52','2026-08-30 05:19:46','google','105116109342553146245'),(8,'twst','$2a$10$RUIAIbDj4/01Rvwx/pcXvO2BY/yQbI2XstQ5VJV66lLNRA8RHDpQG',NULL,'test@gmail.com',1,0.00,1,NULL,NULL,0,NULL,NULL,NULL,'2026-08-31 06:13:17','local',NULL);
/*!40000 ALTER TABLE `User` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `UserSport`
--

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

--
-- Dumping data for table `UserSport`
--

LOCK TABLES `UserSport` WRITE;
/*!40000 ALTER TABLE `UserSport` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserSport` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `UserType`
--

DROP TABLE IF EXISTS `UserType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `UserType` (
  `UserTypeID` int NOT NULL AUTO_INCREMENT,
  `UTypeName` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`UserTypeID`),
  UNIQUE KEY `UTypeName` (`UTypeName`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `UserType`
--

LOCK TABLES `UserType` WRITE;
/*!40000 ALTER TABLE `UserType` DISABLE KEYS */;
INSERT INTO `UserType` VALUES (2,'Member'),(1,'Normal');
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

-- Dump completed on 2026-09-02 16:00:16
