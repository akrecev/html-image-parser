package com.ask.parser.controller;

import com.ask.exception.ExceptInfoUser;
import com.ask.parser.conf.js.ConfJs;
import com.ask.parser.service.HtmlDownloader;
import com.ask.parser.service.ImageDownloader;
import com.ask.parser.service.ImageParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static com.ask.parser.controller.Contr.ROUTE_BASE;

@Controller
@RequiredArgsConstructor
public class ImageParserController {
    public static final String PARSE_MAPPING = "parse";
    public static final String DEFAULT_URL_PARSE_MAPPING = ROUTE_BASE + PARSE_MAPPING;
    public static final String OK_MESSAGE = "Данные успешно получены!";


    private final HtmlDownloader htmlDownloader;
    private final ImageParser imageParser;
    private final ImageDownloader imageDownloader;

    @GetMapping(DEFAULT_URL_PARSE_MAPPING)
    public String showForm(Model model) {
        model.addAttribute("folderPath", "");
        model.addAttribute("url", "");
        return "parsing-form";
    }

    @PostMapping(DEFAULT_URL_PARSE_MAPPING)
    public String processForm(
            @RequestParam String folderPath,
            @RequestParam String url,
            Model model) {

        try {
            String html = htmlDownloader.downloadHtml(url);
            List<String> parseImageUrls = imageParser.parseImageUrls(html, url);
            imageDownloader.downloadImages(parseImageUrls, ConfJs.getInstance().getApp().getOutputDir() + File.separator + folderPath);

            model.addAttribute("folderPath", folderPath);
            model.addAttribute("url", url);
            model.addAttribute("message", OK_MESSAGE);
        } catch (ExceptInfoUser e) {
            String errorMessage = e.getErrors().entrySet().stream()
                    .map(entry -> entry.getKey() + entry.getValue())
                    .collect(Collectors.joining(", "));
            model.addAttribute("folderPath", folderPath);
            model.addAttribute("url", url);
            model.addAttribute("errors", errorMessage);
        }
        return "parsing-form";
    }
}
