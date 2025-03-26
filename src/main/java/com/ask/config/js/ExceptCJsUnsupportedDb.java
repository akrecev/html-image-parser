/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.config.js;

/**
 *
 * @author user
 */
public class ExceptCJsUnsupportedDb extends ExceptCJsUnsupported
{
    private String type;

    public ExceptCJsUnsupportedDb(String type) {
        super("Unsupported DB type " + type);
        this.type = type;
    }
 
    
}
