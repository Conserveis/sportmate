package com.sportmate.util;

/**
 * แปลง Exception ให้เป็นข้อความที่ผู้ใช้อ่านเข้าใจ
 * ไม่เปิดเผยรายละเอียดภายในระบบ (stack trace, SQL, class name)
 */
public final class ErrorMessage {

    private static final String FALLBACK =
            "เกิดข้อผิดพลาดบางอย่าง กรุณาลองใหม่อีกครั้ง";

    private ErrorMessage() {
        // utility class - ห้าม new
    }

    public static String forUser(Exception e) {
        if (e == null) {
            return FALLBACK;
        }

        // IllegalArgumentException/IllegalStateException = validation error
        // ที่เราตั้งใจ throw เอง -> ข้อความปลอดภัยพอที่จะแสดงตรง ๆ
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            String msg = e.getMessage();
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        }

        // exception ประเภทอื่น -> log ไว้ดู แต่ผู้ใช้เห็นข้อความกลาง ๆ
        System.err.println("[ErrorMessage] unhandled: " + e.getClass().getName()
                + " - " + e.getMessage());
        return FALLBACK;
    }
}