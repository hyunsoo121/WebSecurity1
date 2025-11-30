package com.example.demo.board;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardPostService {

    private final BoardPostRepository boardPostRepository;

    public BoardPostService(BoardPostRepository boardPostRepository) {
        this.boardPostRepository = boardPostRepository;
    }

    // 목록
    public List<BoardPost> findAll() {
        return boardPostRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    // 단건 조회
    public BoardPost findById(Long id) {
        return boardPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다. id=" + id));
    }

    // 작성
    public BoardPost create(BoardPost post) {
        return boardPostRepository.save(post);
    }

    // 수정
    public BoardPost update(Long id, BoardPost updated, String inputPassword) {
        BoardPost post = findById(id);

        if (post.getPassword() != null && !post.getPassword().equals(inputPassword)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        post.setTitle(updated.getTitle());
        post.setContent(updated.getContent());
        post.setWriter(updated.getWriter());
        // 비밀번호도 바꾸고 싶으면:
        // post.setPassword(updated.getPassword());

        return boardPostRepository.save(post);
    }

    // 삭제
    public void delete(Long id, String inputPassword) {
        BoardPost post = findById(id);

        if (post.getPassword() != null && !post.getPassword().equals(inputPassword)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        boardPostRepository.delete(post);
    }

    // 조회수 증가
    public void increaseViewCount(Long id) {
        BoardPost post = findById(id);
        post.setViewCount(post.getViewCount() + 1);
        boardPostRepository.save(post);
    }
}
