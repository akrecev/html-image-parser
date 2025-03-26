package com.ask.config.jwt.filter;

import com.ask.config.jwt.TokenHolder;
import com.ask.config.jwt.config.KCRoleConverter;
import com.ask.config.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;

/**
 * For inter-service communication,
 * filter does not validate the access token,
 * but checks for its presence.
 * It is assumed that internal communications occur
 * using already validated access token
 */
@Component
@RequiredArgsConstructor
public class InnerFilter extends OncePerRequestFilter {
    private final String BEARER = "Bearer ";

    private final TokenHolder tokenHolder;
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = null;

        SecurityContext secContext = SecurityContextHolder.getContext();

        if (authHeader != null && authHeader.startsWith(BEARER)) {
            accessToken = authHeader.substring(BEARER.length()); // Убираем "Bearer " перед токеном
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (PublicFilter.AT.equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                }
            }
        }

        if (accessToken == null){
            //proceed filter, without authentication
            filterChain.doFilter(request, response);
            return;
        }

        try {
            tokenHolder.setToken(accessToken);
        } catch (ParseException | NullPointerException e) {
            //token invalid => unauthenticated
            filterChain.doFilter(request, response);
            return;
        }

        secContext
                .setAuthentication(UsernamePasswordAuthenticationToken
                        .authenticated(tokenHolder.getToken(), accessToken, KCRoleConverter.convert(jwtUtils.getRoles())));

        filterChain.doFilter(request, response);
    }
}
