package ru.yandex.practicum.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.main.dto.comment.CommentDto;
import ru.yandex.practicum.main.dto.comment.CreateCommentDto;
import ru.yandex.practicum.main.entity.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(source = "owner.name", target = "ownerName")
    @Mapping(source = "event.id", target = "event")
    CommentDto toCommentDto(Comment comment);

    Comment toComment(CreateCommentDto createCommentDto);
}