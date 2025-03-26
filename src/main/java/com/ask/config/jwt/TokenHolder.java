package com.ask.config.jwt;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.text.ParseException;
import java.util.Optional;

@RequestScope
@Component
public class TokenHolder {
    ThreadLocal<JWTClaimsSet> threadLocal;

    public TokenHolder() {
        threadLocal = new ThreadLocal<>();
    }

    public void setToken(String accessToken) throws ParseException {
        JWTClaimsSet claims;
        JWSObject jwsObj = JWSObject.parse(accessToken);
        claims = JWTClaimsSet.parse(jwsObj.getPayload().toJSONObject());

        threadLocal.set(claims);
    }

    public Optional<JWTClaimsSet> getToken() {
        return threadLocal.get() == null ?
                Optional.empty() : Optional.of(threadLocal.get());
    }

    //prevent memory leak
    @PreDestroy
    public void removeToken() {
        threadLocal.remove();
    }
}
