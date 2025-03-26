package com.ask.config.jwt.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KCRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // получаем доступ к разделу JSON
        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        // если раздел JSON не будет найден - значит нет ролей
        if (realmAccess == null || realmAccess.isEmpty()) {
            return new ArrayList<>();
        }

        return convert((List<String>) realmAccess.get("roles"));
    }

    public static Collection<GrantedAuthority> convert(List<String> roles) {
        // для того, чтобы spring контейнер понимал роли из jwt -
        // нужно их преобразовать в коллекцию GrantedAuthority
        Collection<GrantedAuthority> returnValue = new ArrayList<>();

        for (String roleName : roles) {
            // создаем объект SimpleGrantedAuthority - это дефолтная реализация GrantedAuthority
            returnValue.add(new SimpleGrantedAuthority("ROLE_" + roleName)); // префикс ROLE обязатален
        }

        return returnValue;
    }
}

