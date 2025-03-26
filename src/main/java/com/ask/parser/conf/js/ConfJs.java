/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.parser.conf.js;

import com.ask.config.js.ExceptConf;
import com.ask.config.js.ExceptCJsNoObject;
import com.ask.config.js.ExceptCJsUnsupported;
import java.io.FileNotFoundException;

/**
 *
 * @author vlitenko
 */
public class ConfJs extends com.ask.config.js.ConfJs {

    public static final String APP_NAME = "img_parser_server";
    private static final ConfJs instance = new ConfJs();
    private static final String CONF_FILE_NAME = "conf_img_parser_server.json";

    private ConfJs() {
        super(APP_NAME, ConfJsAppFactory.getInstance());
        try {
            load(CONF_FILE_NAME, "../" + CONF_FILE_NAME);
        } catch (FileNotFoundException ex) {
            throw new ExceptConf("ErrConf1", "Can not load project configuration", "Can not find configuration file " + CONF_FILE_NAME, ex);
        } catch (ExceptCJsUnsupported ex) {
            throw new ExceptConf("ErrConf2", "Can not process project configuration", "Can not parse configuration file " + CONF_FILE_NAME, ex);
        }
    }

    public void updateConf() {
        try {
            load(CONF_FILE_NAME, "../" + CONF_FILE_NAME);
        } catch (FileNotFoundException ex) {
            throw new ExceptConf("ErrConf1", "Can not load project configuration", "Can not find configuration file " + CONF_FILE_NAME, ex);
        } catch (ExceptCJsUnsupported ex) {
            throw new ExceptConf("ErrConf2", "Can not process project configuration", "Can not parse configuration file " + CONF_FILE_NAME, ex);
        }
    }

    public static ConfJs getInstance() {
        return instance;
    }

    public ConfJsApp getApp() {
        try {
            return (ConfJsApp) super.getApp(APP_NAME);
        } catch (ExceptCJsNoObject ex) {
            throw new ExceptConf("ErrConf3", "Can not process project configuration",
                     String.format("Can not get app %s in file %s", APP_NAME, CONF_FILE_NAME), ex);
        }
    }

}
