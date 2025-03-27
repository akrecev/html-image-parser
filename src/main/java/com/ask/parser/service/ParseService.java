package com.ask.parser.service;

import com.ask.exception.ExceptInfoUser;

public interface ParseService {
    void parse(String url, String folderPath) throws ExceptInfoUser;
}
