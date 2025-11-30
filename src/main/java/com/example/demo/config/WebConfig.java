package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드 디렉토리를 절대경로로 변환
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadPathUri = uploadPath.toUri().toString();
        // 예: file:/C:/Users/Admin/Desktop/WebSecurity/WebSecurity-main/uploads/

        // 🔹 /images/** → 실제 파일 폴더(uploads)로 연결
        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPathUri);
    }
}
