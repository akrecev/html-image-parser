package com.ask.parser.service;

import com.ask.exception.ExceptInfoUser;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

@Service
public class HtmlDownloader {
    public String downloadHtml(String urlString) throws ExceptInfoUser {
        try {
            URI uri = new URI(urlString);
            URL url = uri.toURL();

            if (!url.getProtocol().matches("https?|ftp")) {
                throw new ExceptInfoUser(Map.of("Недопустимый протокол: ", url.getProtocol()));
            }

            StringBuilder html = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    html.append(line).append("\n");
                }
            }
            return html.toString();

        } catch (URISyntaxException e) {
            throw new ExceptInfoUser(Map.of("Некорректный URL: ", urlString));
        } catch (MalformedURLException e) {
            throw new ExceptInfoUser(Map.of("Неправильный формат URL: ", urlString));
        } catch (IllegalArgumentException e) {
            throw new ExceptInfoUser(Map.of("Ошибка: ", "Требуется абсолютный URL (например, https://example.com): " + urlString));
        } catch (IOException e) {
            throw new ExceptInfoUser(Map.of("Ошибка загрузки: ", e.getMessage()));
        }
    }
}