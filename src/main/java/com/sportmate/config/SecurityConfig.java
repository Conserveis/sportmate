package com.sportmate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ตั้งค่า Spring Security ให้ทำหน้าที่ "เฉพาะ OAuth2 login" เท่านั้น
 *
 * การควบคุมสิทธิ์เข้าหน้าต่าง ๆ ยังเป็นของ AuthInterceptor เหมือนเดิม
 * (เช็ค session "uid") จึงตั้ง permitAll ไว้ทั้งหมด ไม่ให้ Spring Security
 * มาบล็อกซ้ำซ้อนจนระบบเดิมพัง
 */
@Configuration
public class SecurityConfig {

    private final OAuthLoginSuccessHandler successHandler;

    public SecurityConfig(OAuthLoginSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ปิด CSRF เพราะฟอร์มเดิมทั้งหมดในระบบเขียนไว้ตอนที่ยังไม่มี Spring Security
            // จึงยังไม่มี CSRF token ถ้าเปิดไว้ทุกปุ่ม POST (join, comment, review) จะ 403 ทันที
            // หมายเหตุ: การเปิด CSRF เป็นการยกระดับความปลอดภัยที่ควรทำในอนาคต
            // (Thymeleaf จะเติม token ให้ฟอร์ม th:action อัตโนมัติเมื่อเปิดใช้)
            .csrf(AbstractHttpConfigurer::disable)

            // ให้ AuthInterceptor เป็นคนตัดสินว่าหน้าไหนต้องล็อกอิน
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

            // ปิด logout ของ Spring Security เพราะ AuthController มี /logout ของตัวเองอยู่แล้ว
            .logout(AbstractHttpConfigurer::disable)

            .oauth2Login(oauth -> oauth
                .loginPage("/login")                       // ใช้หน้า login เดิมของเรา
                .successHandler(successHandler)            // สำเร็จแล้วไปใส่ uid ลง session
                .failureUrl("/login?error=oauth")
            );

        return http.build();
    }
}
