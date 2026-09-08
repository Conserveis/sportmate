-- เพิ่มประเภทผู้ใช้ "Admin" (ไม่แตะบัญชีผู้ใช้เดิม)
-- บัญชี admin จะถูกสร้าง/อัปเดตโดย AdminAccountInitializer ตอนแอปสตาร์ท
USE sportmate;

INSERT IGNORE INTO UserType (UTypeName) VALUES ('Admin');