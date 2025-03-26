package com.ask.config.jwt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ask.exception.Except4Support;
import com.ask.parser.conf.js.ConfJs;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.gson.io.GsonDeserializer;
import io.jsonwebtoken.gson.io.GsonSupplierSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtValidationService {

    private static final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.clientid}")
    private String clientId;

    private final String AT = "AT";
    private final String RT = "RT";
    private final String RSA = "RSA";
    private final String REFRESH = "refresh_token";
    private int maxAge;

    public String validateAccessToken(String accessToken, String refreshToken,
                                      HttpServletRequest request, HttpServletResponse response) {
        Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(io.jsonwebtoken.lang.Supplier.class, GsonSupplierSerializer.INSTANCE)
                .disableHtmlEscaping().create();
        String newAccessToken = null;
        try {
            PublicKey publicKey = getPublicKeyFromPEM(ConfJs.getInstance().getApp().getKeycloakPublicKey());

            Claims claims = Jwts.parser()
                    .json(new GsonDeserializer<>(gson))
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            newAccessToken = accessToken;

        } catch (ExpiredJwtException e) {
            newAccessToken = renewAccessToken(refreshToken, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return newAccessToken;
    }

    private PublicKey getPublicKeyFromPEM(String pem) throws Exception {
        byte[] encodedKey = Base64.getDecoder().decode(pem);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        return keyFactory.generatePublic(keySpec);
    }

    public String renewAccessToken(String refreshToken, HttpServletResponse response) {
        Map<String, String> tokens = getNewTokens(refreshToken);
        String newAccessToken = tokens.get(AT);
        String newRefreshToken = tokens.get(RT);

        if (newAccessToken != null && newRefreshToken != null) {
            addCookiesToResponse(AT, newAccessToken, true, response);
            addCookiesToResponse(RT, newRefreshToken, true, response);
            return newAccessToken;
        } else {
            throw new Except4Support("ErrJVS01", "Error to get new tokens",
                    String.format("Error to get new tokens by url: '%s/token' with refresh token: " + refreshToken,
                            ConfJs.getInstance().getApp().getKeycloakOpenidUrl()));
        }
    }

    private void addCookiesToResponse(String tokenType, String token, boolean httpOnly, HttpServletResponse response) {
        ResponseCookie tokenCookie = createCookie(tokenType, token, maxAge, httpOnly);
        response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie.toString());
    }

    private Map<String, String> getNewTokens(String oldRefreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> mapForm = new LinkedMultiValueMap<>();
        mapForm.add("grant_type", REFRESH);
        mapForm.add("client_id", clientId);
        mapForm.add("client_secret", ConfJs.getInstance().getApp().getKeycloakClientSecret());
        mapForm.add(REFRESH, oldRefreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(mapForm, headers);
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            ConfJs.getInstance().getApp().getKeycloakOpenidUrl() + "/token",
                            HttpMethod.POST, request, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Except4Support("ErrJVS02", "Error to get new tokens",
                        String.format("Failed to get new tokens, response status: %s, body: %s",
                                response.getStatusCode(), response.getBody()));
            }

            return processNewTokens(response.getBody());
        } catch (RestClientException ex) {
            throw new Except4Support("ErrJVS03", "Error to get new tokens",
                    String.format("Error to get new tokens by url: '%s/token' with refresh token: " + oldRefreshToken,
                            ConfJs.getInstance().getApp().getKeycloakOpenidUrl()), ex);
        } catch (JsonProcessingException ex) {
            throw new Except4Support("ErrJVS04", "JSON parsing error",
                    String.format("Error parsing response JSON: %s", ex.getMessage()), ex);
        }
    }

    private Map<String, String> processNewTokens(String responseBody) throws Except4Support, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseBody);
        maxAge = root.get("expires_in").asInt();
        String accessToken = root.get("access_token").asText();
        String refreshToken = root.get(REFRESH).asText();

        return Map.of(AT, accessToken, RT, refreshToken);
    }

    private ResponseCookie createCookie(String name, String value, int maxAge, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(false)
                .path("/")
                .maxAge(maxAge)
                .domain(ConfJs.getInstance().getApp().getDomain())
                .build();
    }
}

