package com.ask.config.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.ask.config.jwt.filter.PublicFilter;
import com.ask.config.jwt.service.JwtValidationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtUtils {
    private final String SUB = "sub";
    private final String CUSTOMER_ID = "customer_id";
    private final String SID = "sid";
    private final String EMAIL = "email";
    private final String REALM_ACCESS = "realm_access";
    private final String ROLES = "roles";
    private final String CUSTOMER_ROLE = "customer_role";

    private final TokenHolder tokenHolder;
    private final JwtValidationService jwtService;

    public Optional<String> getUserId() {
        return getClaimAsString(SUB);
    }

    public Optional<Long> getCustomerId() {
        try {
            return Optional.ofNullable(tokenHolder.getToken()
                    .orElseThrow(NullPointerException::new)
                    .getLongClaim(CUSTOMER_ID));
        } catch (ParseException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getSessionId() {
        return getClaimAsString(SID);
    }

    public Optional<String> getEmail() {
        return getClaimAsString(EMAIL);
    }

    public Optional<JWTClaimsSet> getAllClaims() {
        return tokenHolder.getToken();
    }

    public Optional<String> getClaimAsString(String claim) {
        try {
            return Optional.ofNullable(tokenHolder.getToken()
                    .orElseThrow(NullPointerException::new)
                    .getStringClaim(claim));
        } catch (ParseException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public List<String> getRoles() {
        try {
            Optional<JWTClaimsSet> token = tokenHolder.getToken();
            return token.isPresent() ? (List<String>) token.get()
                    .getJSONObjectClaim(REALM_ACCESS).get(ROLES) :
                    List.of();
        } catch (ClassCastException | ParseException | NullPointerException e) {
            return List.of(); // Возможно, надо заменить на роль "Неавторизованный"
        }
    }
    public void getNewTokens(HttpServletRequest req, HttpServletResponse resp) throws ParseException, NoSuchElementException {
        String refreshToken = null;

        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if (PublicFilter.RT.equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }
        String accessToken;
        if (refreshToken != null)
            accessToken = jwtService.renewAccessToken(refreshToken, resp);
        else
            throw new NoSuchElementException();

        tokenHolder.setToken(accessToken);
    }

    public Optional<String> getCustomerRole() {
        return getClaimAsString(CUSTOMER_ROLE);
    }

    /**
     * Used for get "Bearer " + auth token, for further inter-service
     * communications
     *
     * @param request - http request with auth header or cookie
     * @return - Optional of auth header or cookie. if token in
     * cookie, appends "Bearer " in start
     */
    public static Optional<String> getRawBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = null;

        if (authHeader != null && authHeader.startsWith(PublicFilter.BEARER)) {
            accessToken = authHeader;
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (PublicFilter.AT.equals(cookie.getName())) {
                    accessToken = PublicFilter.BEARER + cookie.getValue();
                }
            }
        }

        return Optional.ofNullable(accessToken);
    }
}
