package com.sportmate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @Test
    @DisplayName("รหัสผ่านที่ถูกต้อง: มี 8 ตัวขึ้นไป และมีทั้งพิมพ์ใหญ่และพิมพ์เล็ก")
    void testValidPasswords() {
        assertDoesNotThrow(() -> UserService.validatePassword("Password123"));
        assertDoesNotThrow(() -> UserService.validatePassword("SportMate2026"));
        assertDoesNotThrow(() -> UserService.validatePassword("aB123456"));
        assertDoesNotThrow(() -> UserService.validatePassword("ValidPass@!"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "Ab1",
            "Short1A",          // 7 chars
            "password123",      // no uppercase
            "alllowercase",     // no uppercase
            "PASSWORD123",      // no lowercase
            "ALLUPPERCASE",     // no lowercase
            "12345678",         // no letters
            "!@#$%^&*()"        // no letters
    })
    @DisplayName("รหัสผ่านที่ไม่ถูกต้อง: สั้นกว่า 8 ตัว หรือขาดพิมพ์ใหญ่/พิมพ์เล็ก")
    void testInvalidPasswords(String invalidPassword) {
        assertThrows(IllegalArgumentException.class, () -> UserService.validatePassword(invalidPassword));
    }

    @Test
    @DisplayName("รหัสผ่าน null ต้องโยน IllegalArgumentException")
    void testNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> UserService.validatePassword(null));
    }
}
