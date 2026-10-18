-- =====================================================================
-- SportMate: ระบบทีม + สายการแข่งขันของ Tournament (2026-10-19)
-- รันครั้งเดียวกับ DB ที่มีอยู่แล้ว (ddl-auto=none)
--   docker exec -i <mysql-container> mysql -uroot -p<รหัส> sportmate < db/migrations/2026-10-19_tournament_teams.sql
-- =====================================================================

-- 1) ขนาดทีมของทัวร์นาเมนต์: 1 = แข่งเดี่ยว, 2 = แบดคู่, 5 = บาส, 11 = ฟุตบอล
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
