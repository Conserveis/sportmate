DROP DATABASE IF EXISTS sportmate;
CREATE DATABASE sportmate
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE sportmate;

DROP TABLE IF EXISTS Review;
DROP TABLE IF EXISTS Chat;
DROP TABLE IF EXISTS Event;
DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS Receipt;
DROP TABLE IF EXISTS UserSport;
DROP TABLE IF EXISTS `User`;
DROP TABLE IF EXISTS PostType;
DROP TABLE IF EXISTS Location;
DROP TABLE IF EXISTS Sport;
DROP TABLE IF EXISTS UserType;

-- 1) UserType : ประเภทผู้ใช้ (สมาชิก / ไม่ใช่สมาชิก)
CREATE TABLE UserType (
    UserTypeID   INT AUTO_INCREMENT PRIMARY KEY,
    UTypeName    VARCHAR(50) NOT NULL UNIQUE   -- 'Normal', 'Member'
) ENGINE=InnoDB;

-- 2) Sport : ประเภทกีฬา
CREATE TABLE Sport (
    SportID      INT AUTO_INCREMENT PRIMARY KEY,
    SportName    VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- 3) Location : สถานที่เล่น/แข่งกีฬา
CREATE TABLE Location (
    LocationID    INT AUTO_INCREMENT PRIMARY KEY,
    LocationName  VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- 4) PostType : ประเภทของโพสต์ (Post / Tournament)
CREATE TABLE PostType (
    PostTypeID   INT AUTO_INCREMENT PRIMARY KEY,
    PtypeName    VARCHAR(50) NOT NULL UNIQUE   -- 'Post', 'Tournament'
) ENGINE=InnoDB;

-- 5) User : ข้อมูลผู้ใช้งาน
CREATE TABLE `User` (
    UserID            INT AUTO_INCREMENT PRIMARY KEY,
    UserName          VARCHAR(50)  NOT NULL UNIQUE,
    Password          VARCHAR(255),                   -- NULL สำหรับบัญชี OAuth
    AuthProvider      VARCHAR(20)   NOT NULL DEFAULT 'local',
    ProviderId        VARCHAR(255),
    PhoneNumber       VARCHAR(20),
    Gmail             VARCHAR(255) NOT NULL UNIQUE,
    UserTypeID        INT NOT NULL,
    AvgScore          DECIMAL(3,2) NOT NULL DEFAULT 0.00,   -- ดูหมายเหตุด้านบน

    IsEmailVerified   BOOLEAN NOT NULL DEFAULT FALSE,
    OtpCode           VARCHAR(10),
    OtpExpireAt       DATETIME,

    FailedLoginCount  INT NOT NULL DEFAULT 0,
    LockUntil         DATETIME NULL,
    LastActivityAt    DATETIME NULL,
    MembershipExpireAt DATETIME NULL,

    CreatedAt         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (UserTypeID) REFERENCES UserType(UserTypeID),
    UNIQUE KEY uq_provider (AuthProvider, ProviderId)
) ENGINE=InnoDB;

-- 6) UserSport (กีฬาที่ผู้ใช้กดถูกใจ)
CREATE TABLE UserSport (
    UserID   INT NOT NULL,
    SportID  INT NOT NULL,
    PRIMARY KEY (UserID, SportID),
    FOREIGN KEY (UserID)  REFERENCES `User`(UserID)  ON DELETE CASCADE,
    FOREIGN KEY (SportID) REFERENCES Sport(SportID)  ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7) Receipt
CREATE TABLE Receipt (
    PlaymentID      INT AUTO_INCREMENT PRIMARY KEY,
    UserID          INT NOT NULL,
    DatePlayment    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PlaymentAmount  DECIMAL(10,2) NOT NULL,
    QR              VARCHAR(255),
    FOREIGN KEY (UserID) REFERENCES `User`(UserID)
) ENGINE=InnoDB;

-- 8) Post : โพสต์หากิจกรรมกีฬาหรือทัวร์นาเมนต์
CREATE TABLE Post (
    PostID        INT AUTO_INCREMENT PRIMARY KEY,
    OwnerUserID   INT NOT NULL,
    PostTypeID    INT NOT NULL,
    SportID       INT NOT NULL,
    LocationID    INT NOT NULL,
    PostName      VARCHAR(255) NOT NULL,
    Description   TEXT,
    DatePlay      DATETIME NOT NULL,
    DateCreate    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PublishAt     DATETIME NULL,        -- ถ้า NULL = เผยแพร่ทันที, ถ้ามีค่า = ตั้งเวลาเผยแพร่
    MaxPlayer     INT NOT NULL,
    MinPlayer     INT NOT NULL,
    IsPublic      BOOLEAN NOT NULL DEFAULT TRUE,   -- true=สาธารณะ, false=ต้องขออนุมัติ (UC-7)
    Status        ENUM('open','closed','cancelled','finished') NOT NULL DEFAULT 'open',

    CHECK (MaxPlayer >= MinPlayer),

    FOREIGN KEY (OwnerUserID) REFERENCES `User`(UserID),
    FOREIGN KEY (PostTypeID)  REFERENCES PostType(PostTypeID),
    FOREIGN KEY (SportID)     REFERENCES Sport(SportID),
    FOREIGN KEY (LocationID)  REFERENCES Location(LocationID)
) ENGINE=InnoDB;

-- 9) Event : บันทึกว่า "User คนไหนเข้าร่วม Post/กิจกรรมไหนบ้าง"
CREATE TABLE Event (
    EventID     INT AUTO_INCREMENT PRIMARY KEY,
    UserID      INT NOT NULL,      -- ผู้เข้าร่วม (ไม่ใช่เจ้าของโพสต์)
    PostID      INT NOT NULL,
    EventName   VARCHAR(255),      -- ตาม ER Diagram (ชื่อกิจกรรม ณ เวลาที่เข้าร่วม)
    Status      ENUM('pending','approved','rejected','cancelled') NOT NULL DEFAULT 'pending',
    JoinDate    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CancelDate  DATETIME NULL,

    UNIQUE KEY uq_user_post (UserID, PostID),   -- เข้าร่วมโพสต์เดิมซ้ำไม่ได้

    FOREIGN KEY (UserID) REFERENCES `User`(UserID),
    FOREIGN KEY (PostID) REFERENCES Post(PostID)
) ENGINE=InnoDB;

-- 10) Chat : ข้อความพูดคุยในโพสต์ (เห็นเฉพาะผู้เข้าร่วม)
CREATE TABLE Chat (
    ChatID   INT AUTO_INCREMENT PRIMARY KEY,
    PostID   INT NOT NULL,
    UserID   INT NOT NULL,
    State    ENUM('active','ended') NOT NULL DEFAULT 'active',
    Text     TEXT NOT NULL,
    Time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (PostID) REFERENCES Post(PostID),
    FOREIGN KEY (UserID) REFERENCES `User`(UserID)
) ENGINE=InnoDB;

-- 11) Review : คะแนน/คอมเมนต์ที่ผู้เข้าร่วมให้กับเจ้าของกิจกรรม (UC-6)
CREATE TABLE Review (
    ReviewID     INT AUTO_INCREMENT PRIMARY KEY,
    EventID      INT NOT NULL UNIQUE,
    ReviewScore  TINYINT NOT NULL,
    Comment      TEXT,
    ReviewDate   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CHECK (ReviewScore BETWEEN 1 AND 5),

    FOREIGN KEY (EventID) REFERENCES Event(EventID)
) ENGINE=InnoDB;

-- อัปเดต AvgScore ของเจ้าของกิจกรรมอัตโนมัติ ทุกครั้งที่มี Review ใหม่
DELIMITER $$

CREATE TRIGGER trg_review_after_insert
AFTER INSERT ON Review
FOR EACH ROW
BEGIN
    DECLARE v_owner INT;

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
    )
    WHERE u.UserID = v_owner;
END$$

DELIMITER ;

--  SEED DATA พื้นฐาน
INSERT INTO UserType (UTypeName) VALUES ('Normal'), ('Member');

INSERT INTO PostType (PtypeName) VALUES ('Post'), ('Tournament');

INSERT INTO Sport (SportName) VALUES
    ('Football'), ('Basketball'), ('Badminton'), ('Running'), ('Swimming');

--  ตัวอย่าง Index เพิ่มเติมเพื่อ performance กับ query ที่ใช้บ่อย
CREATE INDEX idx_post_status_publish ON Post (Status, PublishAt);
CREATE INDEX idx_event_user ON Event (UserID);
CREATE INDEX idx_event_post ON Event (PostID);
CREATE INDEX idx_chat_post_time ON Chat (PostID, Time);

--  Seed เพิ่มเติม: สถานที่ (Location) 
USE sportmate;

INSERT INTO Location (LocationName) VALUES
    ('สนามกีฬา ม.เทคโนโลยีพระจอมเกล้าฯ'),
    ('สนามฟุตบอลหญ้าเทียม ลาดกระบัง'),
    ('สนามแบดมินตัน Sport Complex'),
    ('สระว่ายน้ำกลาง'),
    ('ลานอเนกประสงค์ อาคาร A'),
    ('สวนสาธารณะใกล้มหาวิทยาลัย');

--  ตาราง Notification 
USE sportmate;

CREATE TABLE IF NOT EXISTS Notification (
    NotificationID INT AUTO_INCREMENT PRIMARY KEY,
    UserID         INT NOT NULL,                     -- ผู้รับการแจ้งเตือน
    Message        VARCHAR(500) NOT NULL,            -- ข้อความแจ้งเตือน
    Link           VARCHAR(255),                     -- ลิงก์ไปยังโพสต์ที่เกี่ยวข้อง
    Ntype          VARCHAR(30)  NOT NULL,            -- join / cancel_join / new_post / post_cancelled / reminder
    IsRead         BOOLEAN NOT NULL DEFAULT FALSE,   -- อ่านแล้วหรือยัง
    CreatedAt      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (UserID) REFERENCES `User`(UserID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_notif_user_read ON Notification (UserID, IsRead, CreatedAt);
