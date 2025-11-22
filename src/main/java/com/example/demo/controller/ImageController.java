package com.example.demo.controller;

import com.example.demo.service.ImgproxyService;
import com.example.demo.service.ImageFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ImageController {

    private final S3Client s3Client;
    private final ImgproxyService imgproxyService;

    // SSRF 데모용: 서버가 임의 URL로 직접 요청 보내는 서비스
    private final ImageFetchService imageFetchService;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.base-url}")
    private String s3BaseUrl;

    // ---------------------------
    // 공통: S3 이미지 리스트 조회
    // ---------------------------
    private void populateImageList(Model model) {
        List<String> imageUrls = new ArrayList<>();

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucket)
                .build();
        ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);

        for (S3Object obj : listRes.contents()) {

            String originUrl = String.format("%s/%s/%s", s3BaseUrl, bucket, obj.key());
            log.info("ORIGIN S3 URL = {}", originUrl);

            // Imgproxy 옵션 (예시: 300x200 리사이즈)
            String options = "/rs:fit:300:200/q:85";

            // 🔒 Imgproxy 서명 URL 생성
            String proxyUrl = imgproxyService.generateSignedUrl(originUrl, options);

            imageUrls.add(proxyUrl);
        }

        model.addAttribute("images", imageUrls);
    }

    // 메인 페이지
    @GetMapping("/")
    public String index(Model model) {
        populateImageList(model);
        return "index";
    }

    // URL로 이미지를 받아서 S3(게시판 첨부파일처럼) 에 저장
    @PostMapping("/upload-url")
    public String uploadFromUrl(@RequestParam("url") String url,
                                RedirectAttributes redirectAttributes) {

        if (url == null || url.isBlank()) {
            redirectAttributes.addFlashAttribute("message", "URL을 입력하세요.");
            return "redirect:/";
        }

        try {
            ImageFetchService.DownloadedResource res = imageFetchService.fetch(url);

            String key = "url_" + UUID.randomUUID();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(
                            res.contentType() != null ?
                                    res.contentType() :
                                    "application/octet-stream"
                    )
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(res.bytes()));

            log.info("URL image saved to S3. url={}, key={}", url, key);

            // 🔽 방금 업로드된 이미지의 Imgproxy URL을 계산해서 FlashAttribute 로 전달
            String originUrl = String.format("%s/%s/%s", s3BaseUrl, bucket, key);
            String options = "/rs:fit:300:200/q:85";
            String proxyUrl = imgproxyService.generateSignedUrl(originUrl, options);

            redirectAttributes.addFlashAttribute("lastImageUrl", proxyUrl);
            redirectAttributes.addFlashAttribute("message", "URL 이미지 업로드 완료!");
        } catch (Exception e) {
            log.error("Error while fetching URL", e);
            redirectAttributes.addFlashAttribute("message", "URL 업로드 실패: " + e.getMessage());
        }

        return "redirect:/";
    }


    // 이미지 업로드 → S3
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         RedirectAttributes redirectAttributes) throws IOException {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "파일을 선택하세요.");
            return "redirect:/";
        }

        String originalName = file.getOriginalFilename();
        String key = UUID.randomUUID() + "_" + originalName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(file.getBytes())
        );
        log.info("File uploaded to S3: {}", key);

        redirectAttributes.addFlashAttribute("message", "업로드 완료!");

        return "redirect:/";
    }
}
