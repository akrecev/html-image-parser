package com.ask.parser.controller;

import com.ask.exception.ExceptInfoUser;
import com.ask.parser.service.HtmlDownloader;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static com.ask.parser.controller.Contr.ROUTE_BASE;

@Controller
@RequiredArgsConstructor
public class ImageParserController {
    public static final String PARSE_MAPPING = "parse";
    public static final String DEFAULT_URL_PARSE_MAPPING = ROUTE_BASE + PARSE_MAPPING;
    public static final String OK_MESSAGE = "Данные успешно получены!";
    public static final String REDIRECT_ERROR = "redirect:/error?errorMessage=";
    public static final String URL_PARAM = "&url=";


    private final HtmlDownloader htmlDownloader;

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
            Model model,
            HttpServletRequest request) {

        try {
            htmlDownloader.downloadHtml(url);
            model.addAttribute("folderPath", folderPath);
            model.addAttribute("url", url);
            model.addAttribute("message", OK_MESSAGE);
            return "parsing-form";
        } catch (ExceptInfoUser e) {
            String errorMessage = e.getErrors().entrySet().stream()
                    .map(entry -> entry.getKey() + entry.getValue())
                    .collect(Collectors.joining(", "));
            String redirectUrl = REDIRECT_ERROR + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)
                    + URL_PARAM + URLEncoder.encode(url, StandardCharsets.UTF_8);

            return redirectUrl;
        }
    }
}
