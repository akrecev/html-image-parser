/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.parser.controller;

import com.ask.exception.Except;

/**
 *
 * @author VL
 */
public class ExceptAccess extends Except {
    private static final String MEG_DENIED = "Доступ закрыт";
    private Long userId;

    public ExceptAccess(String errorCode, Long userId, String extendedMessage) {
        super(errorCode, MEG_DENIED, extendedMessage, null);
        this.userId = userId;
    }
    @Override
    public String getMessage4Support() 
    {
        return super.getMessage4Support() + String.format(" Userid: %d", userId);
    }
    
}
