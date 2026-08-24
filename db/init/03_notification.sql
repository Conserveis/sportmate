-- ============================================================================
--  ตาราง Notification : เก็บการแจ้งเตือนของผู้ใช้ (UC-4)
--  รันต่อจาก 01_schema.sql / 02_seed_location.sql โดย docker-entrypoint-initdb.d
-- ============================================================================
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
