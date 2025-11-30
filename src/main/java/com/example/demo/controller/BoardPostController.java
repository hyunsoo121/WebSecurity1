package com.example.demo.controller;

import com.example.demo.board.BoardPost;
import com.example.demo.board.BoardPostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/board")
public class BoardPostController {

    private final BoardPostService boardPostService;

    // application.yml 의 file.upload-dir 사용
    @Value("${file.upload-dir}")
    private String uploadDir;

    public BoardPostController(BoardPostService boardPostService) {
        this.boardPostService = boardPostService;
    }

    // 1) 게시글 목록
    @GetMapping
    public String list(Model model) {
        model.addAttribute("posts", boardPostService.findAll());
        return "board/list";
    }

    // 2) 게시글 상세
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
                         @ModelAttribute("errorMessage") String errorMessage) {
        boardPostService.increaseViewCount(id);
        model.addAttribute("post", boardPostService.findById(id));
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "board/detail";
    }

    // 3) 글쓰기 폼
    @GetMapping("/write")
    public String writeForm(Model model) {
        model.addAttribute("post", new BoardPost());
        model.addAttribute("mode", "create");
        return "board/form";
    }

    // 4) 글 등록 처리 (이미지 업로드 포함)
    @PostMapping
    public String write(@ModelAttribute BoardPost post,
                        @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                        RedirectAttributes redirectAttributes) {

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = saveImageFile(imageFile, redirectAttributes);
            if (imageUrl == null) {
                // 저장 실패 시 목록으로 돌려보내면서 에러 메시지
                return "redirect:/board";
            }
            post.setImageUrl(imageUrl);
        }

        boardPostService.create(post);
        return "redirect:/board";
    }

    // 5) 수정 폼
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           @ModelAttribute("errorMessage") String errorMessage) {
        model.addAttribute("post", boardPostService.findById(id));
        model.addAttribute("mode", "edit");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "board/form";
    }

    // 6) 수정 처리 (이미지 교체 가능)
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute BoardPost form,
                       @RequestParam String password,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       RedirectAttributes redirectAttributes) {

        try {
            BoardPost existing = boardPostService.findById(id);

            // 기존 이미지 유지
            String imageUrl = existing.getImageUrl();

            // 새 이미지 업로드가 있으면 교체
            if (imageFile != null && !imageFile.isEmpty()) {
                String newImageUrl = saveImageFile(imageFile, redirectAttributes);
                if (newImageUrl == null) {
                    return "redirect:/board/" + id + "/edit";
                }
                imageUrl = newImageUrl;
            }

            form.setImageUrl(imageUrl);
            boardPostService.update(id, form, password);

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/board/" + id + "/edit";
        }

        return "redirect:/board/" + id;
    }

    // 7) 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam String password,
                         RedirectAttributes redirectAttributes) {
        try {
            boardPostService.delete(id, password);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/board/" + id;
        }
        return "redirect:/board";
    }

    private String saveImageFile(MultipartFile imageFile, RedirectAttributes redirectAttributes) {
        try {
            // uploads 같은 상대경로를 절대경로로 변환
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = imageFile.getOriginalFilename();
            String ext = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String newFileName = UUID.randomUUID().toString() + ext;
            Path target = uploadPath.resolve(newFileName);

            Files.copy(imageFile.getInputStream(), target);

            // 🔹 여기! 브라우저에서 접근할 URL은 WebConfig의 "/images/**"와 맞춰야 함
            return "/images/" + newFileName;

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "이미지 업로드에 실패했습니다.");
            return null;
        }
    }
}
