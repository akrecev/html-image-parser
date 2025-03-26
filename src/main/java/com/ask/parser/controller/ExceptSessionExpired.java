/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.parser.controller;

import com.ask.exception.Except;

/**
 *
 * @author marina
 */
public class ExceptSessionExpired extends Except {
    
    public final static String MESSAGE = "Сессия истекла";
    
    public ExceptSessionExpired(String errorCode) {
        super(errorCode, MESSAGE, MESSAGE);
    }
}
