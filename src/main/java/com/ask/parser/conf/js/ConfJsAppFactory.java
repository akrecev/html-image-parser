/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.parser.conf.js;

import com.ask.config.js.ConfJsAppFactory_I;
import com.ask.config.js.ConfJsDbFactory_I;
import java.util.HashMap;

/**
 *
 * @author vlitenko
 */
public class ConfJsAppFactory implements ConfJsAppFactory_I
{
    private static final ConfJsAppFactory instance = new ConfJsAppFactory();

    public static ConfJsAppFactory getInstance() {
        return instance;
    }
    
    @Override
    public com.ask.config.js.ConfJsApp newObj(HashMap<String, ConfJsDbFactory_I> factoriesDb) {
        return new ConfJsApp();
    }
    
}
