package com.sportmate.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.sportmate.entity.User;
import com.sportmate.service.OAuthAccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthAccountService oAuthAccountService;
    private final RememberMeService rememberMeService;

    public OAuthLoginSuccessHandler(OAuthAccountService oAuthAccountService,
                                    RememberMeService rememberMeService) {
        this.oAuthAccountService = oAuthAccountService;
        this.rememberMeService = rememberMeService;
        setDefaultTargetUrl("/home");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, jakarta.servlet.ServletException {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            String provider = token.getAuthorizedClientRegistrationId();   // google / apple / thaid
            OAuth2User oAuth2User = token.getPrincipal();

            User user = oAuthAccountService.findOrCreate(provider, oAuth2User);
            request.getSession(true).setAttribute("uid", user.getId());
            rememberMeService.remember(request, response, user.getId());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
