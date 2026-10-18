package com.sportmate.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * กติกาจำนวนผู้เข้าแข่งของ Tournament แยกตามกีฬา
 *
 *  แบดมินตัน เดี่ยว  ขั้นต่ำ 2   เพิ่มทีละ 1
 *  แบดมินตัน คู่     ขั้นต่ำ 4   คู่ละ 2     เพิ่มทีละ 2
 *  ว่ายน้ำ เดี่ยว     ขั้นต่ำ 2   เพิ่มทีละ 1
 *  วิ่ง เดี่ยว        ขั้นต่ำ 2   เพิ่มทีละ 1
 *  บาสเกตบอล       ขั้นต่ำ 10  ทีมละ 5    เพิ่มทีละ 5
 *  ฟุตบอล          ขั้นต่ำ 22  ทีมละ 11   เพิ่มทีละ 11
 *  ทุกกีฬาไม่เกิน 500 คน
 */
public final class SportRule {

    public static final int MAX_PLAYERS = 500;

    /** min = ขั้นต่ำ (ล็อก), step = เพิ่มได้ทีละ, teamSize = 1 คือแข่งเดี่ยว */
    public record Rule(int min, int step, int teamSize, String label) {
        /** จำนวนสูงสุดที่ตั้งได้จริง (ปัดลงให้หารด้วย step ลงตัว และไม่เกิน 500) */
        public int cap() { return (MAX_PLAYERS / step) * step; }
        public boolean isTeam() { return teamSize > 1; }
    }

    private SportRule() {}

    /**
     * @param sportName ชื่อกีฬาในตาราง Sport (Badminton, Basketball, Football, Running, Swimming)
     * @param requestedTeamSize ใช้เฉพาะแบดมินตัน: 2 = คู่, อื่น ๆ = เดี่ยว
     */
    public static Rule of(String sportName, Integer requestedTeamSize) {
        String s = sportName == null ? "" : sportName.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "badminton":
                return (requestedTeamSize != null && requestedTeamSize == 2)
                        ? new Rule(4, 2, 2, "แบดมินตัน คู่")
                        : new Rule(2, 1, 1, "แบดมินตัน เดี่ยว");
            case "basketball":
                return new Rule(10, 5, 5, "บาสเกตบอล");
            case "football":
                return new Rule(22, 11, 11, "ฟุตบอล");
            case "swimming":
                return new Rule(2, 1, 1, "ว่ายน้ำ เดี่ยว");
            case "running":
                return new Rule(2, 1, 1, "วิ่ง เดี่ยว");
            default:
                return new Rule(2, 1, 1, sportName == null ? "กีฬา" : sportName);
        }
    }

    /** ตรวจจำนวนสูงสุดตามกติกา — ไม่ผ่านจะโยน IllegalArgumentException พร้อมข้อความภาษาไทย */
    public static void validateMax(Rule r, Integer maxPlayer) {
        if (maxPlayer == null) {
            throw new IllegalArgumentException("กรุณาระบุจำนวนผู้เข้าแข่งสูงสุด");
        }
        if (maxPlayer < r.min()) {
            throw new IllegalArgumentException(r.label() + " ต้องมีผู้เข้าแข่งอย่างน้อย " + r.min() + " คน");
        }
        if (maxPlayer > MAX_PLAYERS) {
            throw new IllegalArgumentException("ทัวร์นาเมนต์รับผู้เข้าแข่งได้ไม่เกิน " + MAX_PLAYERS + " คน");
        }
        if (maxPlayer % r.step() != 0) {
            throw new IllegalArgumentException(r.label() + " ต้องตั้งจำนวนสูงสุดเพิ่มทีละ " + r.step()
                    + " คน (เช่น " + r.min() + ", " + (r.min() + r.step()) + ", " + (r.min() + 2 * r.step()) + ")");
        }
    }

    /** กติกาทั้งหมด ใช้ส่งให้ JavaScript ในฟอร์มสร้าง/แก้ไขทัวร์นาเมนต์ (key = ชื่อกีฬาตัวเล็ก) */
    public static Map<String, Map<String, Object>> clientRules() {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        put(m, "badminton", of("Badminton", 1));
        put(m, "badminton-2", of("Badminton", 2));
        put(m, "basketball", of("Basketball", null));
        put(m, "football", of("Football", null));
        put(m, "swimming", of("Swimming", null));
        put(m, "running", of("Running", null));
        return m;
    }

    private static void put(Map<String, Map<String, Object>> m, String key, Rule r) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("min", r.min());
        v.put("step", r.step());
        v.put("teamSize", r.teamSize());
        v.put("cap", r.cap());
        v.put("label", r.label());
        m.put(key, v);
    }
}
