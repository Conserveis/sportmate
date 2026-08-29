package com.sportmate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final OAuthLoginSuccessHandler successHandler;

    public SecurityConfig(OAuthLoginSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            // ให้ AuthInterceptor เป็นคนตัดสิน
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

            .logout(AbstractHttpConfigurer::disable)

            .oauth2Login(oauth -> oauth
                .loginPage("/login")                       // ใช้หน้า login เดิมของเรา
                .successHandler(successHandler)            // สำเร็จแล้วไปใส่ uid ลง session
                .failureUrl("/login?error=oauth")
            );

        return http.build();
    }
}
