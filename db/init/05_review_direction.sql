-- เพิ่มทิศทางของรีวิว
ALTER TABLE Review
    ADD COLUMN Direction ENUM('to_owner','to_participant') NOT NULL DEFAULT 'to_owner';

-- 1 Event รีวิวได้ 2 ทิศทาง (คนละ 1 ครั้ง) แทนที่จะได้แค่ 1
ALTER TABLE Review ADD UNIQUE KEY uq_review_event_direction (EventID, Direction);
ALTER TABLE Review DROP INDEX EventID;

-- trigger เดิมจะเอารีวิวฝั่งผู้เข้าร่วมไปปนกับคะแนนผู้จัด ต้องเขียนใหม่
DROP TRIGGER IF EXISTS trg_review_after_insert;
DELIMITER $$

CREATE TRIGGER trg_review_after_insert
AFTER INSERT ON Review
FOR EACH ROW
BEGIN
    DECLARE v_owner INT;

    -- คิด AvgScore เฉพาะรีวิวที่ให้ "ผู้จัด" เท่านั้น
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
END$$

DELIMITER ;