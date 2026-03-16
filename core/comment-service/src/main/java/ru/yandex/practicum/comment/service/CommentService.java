package ru.yandex.practicum.comment.service;


import ru.yandex.practicum.interaction.dto.comment.CommentDto;
import ru.yandex.practicum.interaction.dto.comment.CreateCommentDto;
import ru.yandex.practicum.interaction.dto.event.EventFullDto;

import java.util.List;

public interface CommentService {
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    CommentDto create(Long eventId, Long userId, CreateCommentDto createCommentDto);

    CommentDto update(Long commentId, Long userId, CreateCommentDto createCommentDto);

    void delete(Long userId, Long commentId);

    List<CommentDto> findCommentByText(String text);

    CommentDto getCommentById(Long commentId);

    List<EventFullDto> getTopEventByCommentsCount(int count);
}
