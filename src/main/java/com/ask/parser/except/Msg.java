/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.parser.except;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import vl.utils.MapList;
import vl.utils.MapMap;

/**
 *
 * @author vlitenko
 */
public class Msg {

    public static final String US_en = "US_en";
    public static final String RU_ru = "RU_ru";

    public static final String CODE_LOGIN_ERR_SESSION_EXIPED = "CODE_LOGIN_ERR_SESSION_EXIPED";
    private static final String RU_MSG_LOGIN_ERR_SESSION_EXIPED = "Ваша сессия истекла, зайдите заново";
    public static final String CODE_TG_ERR_NO_TRIES = "CODE_TG_ERR_NO_TRIES";
    private static final String RU_MSG_TG_ERR_NO_TRIES = "У вас исчерпан лимит доступных попыток";
    public static final String CODE_HTTP_METHOD_NOT_SUPPORTED= "CODE_HTTP_METHOD_NOT_SUPPORTED";
    private static final String RU_MSG_HTTP_METHOD_NOT_SUPPORTED = "HTTP метод %s не поддерживается";

    private static final HashMap<String, Msg> map = new HashMap<>();
    private static final MapMap<String, String, String> msg = new MapMap<>();   // locale, code, translation
    private static final MapList<String, String> months = new MapList<>();   // locale, List of month

    static {
        map.put(RU_ru, new Msg(RU_ru));

        HashMap<String, String> mMess;
        mMess = msg.getOrCreate(RU_ru);
        mMess.put(CODE_LOGIN_ERR_SESSION_EXIPED, RU_MSG_LOGIN_ERR_SESSION_EXIPED);
        mMess.put(CODE_TG_ERR_NO_TRIES, RU_MSG_TG_ERR_NO_TRIES);
        mMess.put(CODE_HTTP_METHOD_NOT_SUPPORTED, RU_MSG_HTTP_METHOD_NOT_SUPPORTED);

        ArrayList<String> aMonth = months.getOrCreateList(RU_ru);
        aMonth.addAll(Arrays.asList("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"));
    }

    private static Msg instance = map.get(RU_ru);
    private String locale;

    public Msg(String locale) {
        this.locale = locale;
    }

    public static Msg i() {
        return instance;
    }

    public static void changeLocale(String locale) {
        instance = map.get(locale);
        instance.locale = locale;
    }

    public Message4User getMessage(String messageCode) {
        String sRes = msg.get(locale, messageCode);
        return (sRes != null) ? new Message4User(sRes) : new Message4User(messageCode);
    }
    public Message4User getMessageHttpMethodNotSupported(String method) {
        return new Message4User(String
                .format(getMessage(CODE_HTTP_METHOD_NOT_SUPPORTED).toString(),
                        method));
    }


}
