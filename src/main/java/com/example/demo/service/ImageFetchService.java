package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class ImageFetchService {

    // URL로 GET 요청을 보내서 응답 바이트와 Content-Type을 그대로 반환
    public DownloadedResource fetch(String targetUrl) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        String contentType = conn.getContentType();

        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            is.transferTo(baos); // Java 9+
            byte[] bytes = baos.toByteArray();

            return new DownloadedResource(bytes, contentType);
        }
    }

    // 결과 묶어주는 간단한 record
    public record DownloadedResource(byte[] bytes, String contentType) {}
}
