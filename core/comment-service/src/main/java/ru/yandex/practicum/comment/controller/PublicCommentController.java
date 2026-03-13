package ru.yandex.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.comment.service.CommentService;
import ru.yandex.practicum.interaction.dto.comment.CommentDto;
import ru.yandex.practicum.interaction.dto.event.EventFullDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping("/{eventId}/comments")
    public List<CommentDto> getEventComments(@PathVariable(name = "eventId") Long eventId,
                                             @RequestParam(defaultValue = "0") int from,
                                             @RequestParam(defaultValue = "10") int size) {
        return commentService.getEventComments(eventId, from, size);
    }

    @GetMapping("/comments/top")
    public List<EventFullDto> getTopEvents(@RequestParam(name = "count", defaultValue = "5") int count) {
        return commentService.getTopEventByCommentsCount(count);
    }

    @GetMapping("/comments/{commentId}")
    public CommentDto getCommentById(@PathVariable(name = "commentId") Long commentId) {
        return commentService.getCommentById(commentId);
    }

}