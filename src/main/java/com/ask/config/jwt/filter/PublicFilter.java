package com.ask.config.jwt.filter;


import com.ask.config.jwt.TokenHolder;
import com.ask.config.jwt.config.MutableHttpServletRequest;
import com.ask.config.jwt.service.JwtValidationService;
import com.ask.parser.conf.js.ConfJs;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;

@RequiredArgsConstructor
@Component
public class PublicFilter extends OncePerRequestFilter {
    @Value("${keycloak.clientid}")
    private String clientId;

    private final String RESPONSE_TYPE_CODE = "code";
    private final String SCOPE = "openid";

    public static final String BEARER = "Bearer ";
    public static final String RT = "RT";
    public static final String AT = "AT";
    private static final String ALPHA_NUMERIC_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final JwtValidationService jwtValidationService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final TokenHolder tokenHolder;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = null;
        String refreshToken = null;

        if (authHeader != null && authHeader.startsWith(BEARER)) {
            accessToken = authHeader.substring(BEARER.length()); // Убираем "Bearer " перед токеном
        }

        if (request.getCookies() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        for (Cookie cookie : request.getCookies()) {
            if (RT.equals(cookie.getName())) {
                refreshToken = cookie.getValue();
            }
            if (AT.equals(cookie.getName())) {
                accessToken = cookie.getValue();
            }
        }


        if (accessToken != null && refreshToken != null) {
            String newAccessToken = "";
            try {
                newAccessToken = jwtValidationService.validateAccessToken(accessToken, refreshToken, request, response);

                tokenHolder.setToken(newAccessToken);
            } catch (Exception e) {
                response.sendRedirect(getAuthUri());
                return;
            }

            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);

            mutableRequest.addHeader(HttpHeaders.AUTHORIZATION, BEARER + newAccessToken);

            filterChain.doFilter(mutableRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }

    }

    private String getAuthUri() {
        String authUrl = ConfJs.getInstance().getApp().getKeycloakOpenidUrl() + "/auth";
        authUrl += "?response_type=" + RESPONSE_TYPE_CODE;
        authUrl += "&client_id=" + clientId;
        authUrl += "&state=" + generateState(30);
        authUrl += "&scope=" + SCOPE;
        authUrl += "&redirect_uri=" + ConfJs.getInstance().getApp().getKeycloakClientUrl();

        return authUrl;
    }

    public static String generateState(int length) {
        StringBuilder state = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHA_NUMERIC_CHARACTERS.length());
            state.append(ALPHA_NUMERIC_CHARACTERS.charAt(index));
        }

        return state.toString();
    }

}