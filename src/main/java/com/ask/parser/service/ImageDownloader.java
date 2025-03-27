package com.ask.parser.service;


import com.ask.exception.Except4Support;
import com.ask.exception.ExceptInfoUser;
import com.ask.parser.conf.js.ConfJs;
import org.springframework.stereotype.Service;
import vl.thread.ThreadInPool;
import vl.thread.ThreadPool;
import vl.thread.ThreadPool_I;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ImageDownloader {
    public static final String ERROR_CODE_01 = "ErrImageDownloader01";

    private final ThreadPool threadPool;
    private final List<String> downloadErrors;

    public ImageDownloader() {
        int maxThreads = ConfJs.getInstance().getApp().getHikariPoolMaxSize();
        this.threadPool = new ThreadPool(maxThreads);
        this.downloadErrors = Collections.synchronizedList(new ArrayList<>());
    }

    public void downloadImages(List<String> imageUrls, String outputDir) throws ExceptInfoUser {
        File directory = new File(outputDir);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new Except4Support(ERROR_CODE_01, "Ошибка создания папки для загрузки: " + outputDir);
            }
        }

        downloadErrors.clear();

        int count = 0;
        for (String imageUrl : imageUrls) {
            String fileName = getFileNameFromUrl(imageUrl);
            String filePath = outputDir + File.separator + count + "_" + fileName;
            DownloadImageTask task = new DownloadImageTask(threadPool, imageUrl, filePath, downloadErrors);
            threadPool.submit(task);
            count++;
        }


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!downloadErrors.isEmpty()) {
            Map<String, String> errorsMap = new HashMap<>();
            errorsMap.put("Ошибки при загрузке изображений: ", String.join("; ", downloadErrors));
            throw new ExceptInfoUser(errorsMap);
        }
    }

    private String getFileNameFromUrl(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        int paramIndex = fileName.indexOf('?');
        if (paramIndex != -1) {
            fileName = fileName.substring(0, paramIndex);
        }
        if (!fileName.matches(".*\\.(jpg|jpeg|png|gif|bmp)$")) {
            fileName += ".jpg";
        }
        return fileName;
    }

    private static class DownloadImageTask extends ThreadInPool {
        private final String imageUrl;
        private final String filePath;
        private final List<String> errors;

        public DownloadImageTask(ThreadPool_I pool, String imageUrl, String filePath, List<String> errors) {
            super(pool);
            this.imageUrl = imageUrl;
            this.filePath = filePath;
            this.errors = errors;
        }

        @Override
        protected void runVl2() {
            try {
                URI uri = new URI(imageUrl);
                URL url = uri.toURL();
                try (InputStream in = url.openStream()) {
                    Files.copy(in, Paths.get(filePath));
                }
            } catch (URISyntaxException e) {
                errors.add("Недопустимый синтаксис URL изображения: " + imageUrl);
            } catch (IllegalArgumentException e) {
                errors.add("Относительный URL изображения недопустим: " + imageUrl);
            } catch (IOException e) {
                errors.add("Не удалось скачать изображение: " + imageUrl + " (" + e.getMessage() + ")");
            }
        }
    }
}