package ru.yandex.practicum.comment.mapper;

import org.mapstruct.*;
import ru.yandex.practicum.comment.entity.Comment;
import ru.yandex.practicum.interaction.dto.comment.CommentDto;
import ru.yandex.practicum.interaction.dto.comment.CreateCommentDto;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "ownerName", ignore = true)
    CommentDto toCommentDto(Comment comment, @Context String ownerName);

    @AfterMapping
    default void setOwnerName(@Context String ownerName, @MappingTarget CommentDto dto) {
        if (ownerName != null) {
            dto.setOwnerName(ownerName);
        }
    }

    Comment toComment(CreateCommentDto createCommentDto);
}