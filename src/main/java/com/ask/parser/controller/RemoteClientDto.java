/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.parser.controller;

import jakarta.servlet.http.HttpServletRequest;

/**
 *
 * @author vlitenko
 */
public class RemoteClientDto
{
    private String clientIp;
    private String userAgent;
    private String requestURI;
    private String host;

    public RemoteClientDto(HttpServletRequest req) {
        clientIp = req.getHeader("X-FORWARDED-FOR");
        if (clientIp == null) {
            clientIp = req.getRemoteAddr();
        }
        userAgent = req.getHeader("User-Agent");
        requestURI = req.getRequestURI();
        host = req.getHeader("Host");
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getHost() {
        return host;
    }
    
    
    
}
