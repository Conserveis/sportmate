package com.sportmate.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 *  Google เป็น OAuth2 จริง
 */
@Configuration
public class OAuthClientConfig {

    @Value("${google.client-id:}")     private String googleClientId;
    @Value("${google.client-secret:}") private String googleClientSecret;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (!googleClientId.isBlank()) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleClientId)
                    .clientSecret(googleClientSecret)
                    .build());
        }

        // ต้องมีอย่างน้อย 1 ตัวเสมอ ไม่งั้น Spring Security start ไม่ขึ้น
        if (registrations.isEmpty()) {
            registrations.add(placeholderRegistration());
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    /** ตัวหลอกกันแอปล้มตอนยังไม่ได้ตั้งค่า Google — กดใช้งานจริงไม่ได้ */
    private ClientRegistration placeholderRegistration() {
        return ClientRegistration.withRegistrationId("disabled")
                .clientId("not-configured")
                .clientSecret("not-configured")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.invalid/authorize")
                .tokenUri("https://example.invalid/token")
                .clientName("Disabled")
                .build();
    }
}