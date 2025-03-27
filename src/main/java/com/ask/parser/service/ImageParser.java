package com.ask.parser.service;

import com.ask.exception.Except4Support;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImageParser {
    public static final String ERROR_CODE_01 = "ErrImageParser01";

    public List<String> parseImageUrls(String html) {
        List<String> imageUrls = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(html);
            Elements imgTags = doc.select("img");

            for (Element img : imgTags) {
                String src = img.attr("src");
                if (!src.isEmpty()) {
                    imageUrls.add(src);
                }
            }
        } catch (Exception e) {
            throw new Except4Support(ERROR_CODE_01, "Error to parse html");
        }

        return imageUrls;
    }
}

