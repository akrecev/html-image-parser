/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.config;

import com.ask.config.jwt.config.KCRoleConverter;
import com.ask.config.jwt.config.OAuth2ExceptionHandler;
import com.ask.config.jwt.filter.InnerFilter;
import com.ask.config.jwt.filter.PublicFilter;
import com.ask.parser.controller.Contr;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * @author vlitenko
 */
@Configuration
public class ConfWebSecurity {
    private final PublicFilter publicFilter;
    private final InnerFilter innerFilter;

    public ConfWebSecurity(PublicFilter publicFilter, InnerFilter innerFilter) {
        this.publicFilter = publicFilter;
        this.innerFilter = innerFilter;
    }

    //disable automatic adding to the  filter chain
    @Bean
    public FilterRegistrationBean<PublicFilter> jwtFilterRegistration(PublicFilter filter) {
        FilterRegistrationBean<PublicFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    //disable automatic adding to the  filter chain
    @Bean
    public FilterRegistrationBean<InnerFilter> innerFilterRegistration(InnerFilter filter) {
        FilterRegistrationBean<InnerFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(1)
    // use it for inter-service endpoints, when token required
    public SecurityFilterChain innerFilterChain(HttpSecurity http) throws Exception {
        return http
                //set matcher for all urls with internal access
                .securityMatcher("/") //указать url, к которому будет применяться эта цепочка
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                //add filter for inner requests
                .addFilterBefore(innerFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }


    @Bean
    @Order(2)
    //use it for app who talks with front. authorized, token required
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {

        // конвертер для настройки spring security
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        // подключаем конвертер ролей
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KCRoleConverter());

        // все сетевые настройки
        return http
                .securityMatcher(Contr.ROUTE_INTERNAL_ADMIN + "**") //указать url, к которому будет применяться эта цепочка
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().hasRole("T-SUPPORT")
                )
                .csrf(csrf -> csrf.disable()) // отключаем встроенную защиту от CSRF атак, т.к. используем свою, из OAUTH2
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // разрешает выполнение OPTIONS запросов от клиента (preflight запросы) без авторизации
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new OAuth2ExceptionHandler())// обработка ошибок при аутентификации
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // настройка создания сессий
                //add token filter with validation
                .addFilterBefore(publicFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(3)
    // use it for open access inter-service and frontend communication
    public SecurityFilterChain unauthorizedFilterChain(HttpSecurity http) throws Exception {
        return http
                //set matcher for public ulrs, without token validation
                .securityMatcher("your-url" + "**") //указать url, к которому должна быть применена цепочка
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    // все эти настройки обязательны для корректного сохранения куков в браузере
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
//        configuration.setAllowedOrigins(Arrays.asList(clientURL));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
