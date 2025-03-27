package com.ask.parser.service.htmlservice.impl;

import com.ask.exception.ExceptInfoUser;
import com.ask.parser.conf.js.ConfJs;
import com.ask.parser.service.htmlservice.HtmlParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HtmlParseServiceImpl implements HtmlParseService {
    private final HtmlDownloader htmlDownloader;
    private final ImageParser imageParser;
    private final ImageDownloader imageDownloader;


    @Override
    public void parse(String url, String folderPath) throws ExceptInfoUser {
        String html = htmlDownloader.downloadHtml(url);
        List<String> parseImageUrls = imageParser.parseImageUrls(html, url);
        imageDownloader.downloadImages(
                parseImageUrls, ConfJs.getInstance().getApp().getOutputDir() + File.separator + folderPath);
    }
}
