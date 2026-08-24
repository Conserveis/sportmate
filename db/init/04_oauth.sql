-- ============================================================================
--  รองรับการเข้าสู่ระบบผ่านผู้ให้บริการภายนอก (Google / ThaiD)
--  รันต่อจากไฟล์ 01-03 โดย docker-entrypoint-initdb.d
-- ============================================================================
USE sportmate;

-- ผู้ใช้ที่ล็อกอินผ่าน Google/ThaiD จะไม่มีรหัสผ่านในระบบเรา
-- จึงต้องยอมให้ Password เป็น NULL ได้ (เดิมเป็น NOT NULL)
ALTER TABLE `User` MODIFY COLUMN Password VARCHAR(255) NULL;

-- AuthProvider : 'local' = สมัครด้วย username/password เอง
--                'google' | 'thaid' = ล็อกอินผ่านผู้ให้บริการภายนอก
ALTER TABLE `User` ADD COLUMN AuthProvider VARCHAR(20) NOT NULL DEFAULT 'local';

-- ProviderId : รหัสผู้ใช้ฝั่งผู้ให้บริการ (เช่น Google "sub", ThaiD "pid")
--              ใช้จับคู่บัญชีเดิมเวลาล็อกอินซ้ำ แม้ผู้ใช้จะเปลี่ยนอีเมล
ALTER TABLE `User` ADD COLUMN ProviderId VARCHAR(255) NULL;

-- ผู้ให้บริการเดียวกันต้องไม่มี ProviderId ซ้ำกัน
ALTER TABLE `User` ADD CONSTRAINT uq_provider UNIQUE (AuthProvider, ProviderId);
