package ru.yandex.practicum.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.comment.entity.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findAllByEvent(Long eventId, Pageable pageable);

    Optional<Comment> findByIdAndOwner(Long commentId, Long ownerId);

    void deleteByIdAndOwner(Long commentId, Long ownerId);

    List<Comment> findAllByTextIsLikeIgnoreCase(String text);

}
