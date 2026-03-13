package ru.yandex.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.comment.entity.Comment;
import ru.yandex.practicum.comment.feign.EventClient;
import ru.yandex.practicum.comment.feign.UserClient;
import ru.yandex.practicum.comment.mapper.CommentMapper;
import ru.yandex.practicum.comment.repository.CommentRepository;
import ru.yandex.practicum.interaction.dto.comment.CommentDto;
import ru.yandex.practicum.interaction.dto.comment.CreateCommentDto;
import ru.yandex.practicum.interaction.dto.event.EventFullDto;
import ru.yandex.practicum.interaction.exception.CommentNotExistException;
import ru.yandex.practicum.interaction.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final EventClient eventRepository;
    private final UserClient userRepository;
    private final CommentMapper commentMapper;

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").descending());
        Page<Comment> comments = commentRepository.findAllByEvent(eventId, pageable);

        Set<Long> ownerIds = comments.getContent().stream()
                .map(Comment::getOwner)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> ownersNames = userRepository.getNamesByIds(ownerIds);

        return comments.getContent()
                .stream()
                .map(comment -> commentMapper.toCommentDto(comment, ownersNames.get(comment.getOwner())))
                .toList();
    }

    @Override
    @Transactional
    public CommentDto create(Long eventId, Long userId, CreateCommentDto createCommentDto) {

        if(!eventRepository.exists(eventId)) {
            throw new CommentNotExistException("Not possible create Comment - " +
                    "Does not exist Event with Id " + eventId);
        }

        if (!userRepository.existsById(userId)) {
            throw new CommentNotExistException("Not possible create Comment - " + "Does not exist User with Id " + userId);
        }

        Comment commentFromDto = commentMapper.toComment(createCommentDto);

        commentFromDto.setEvent(eventId);
        commentFromDto.setOwner(userId);
        commentFromDto.setCreated(LocalDateTime.now());

        Comment comment = commentRepository.save(commentFromDto);
        Map<Long, String> ownerNames = userRepository.getNamesByIds(List.of(userId));

        return commentMapper.toCommentDto(comment, ownerNames.get(userId));
    }

    @Override
    @Transactional
    public CommentDto update(Long commentId, Long userId, CreateCommentDto createCommentDto) {
        Comment comment = commentRepository.findByIdAndOwner(commentId, userId)
                .orElseThrow(() -> new CommentNotExistException("Not possible update Comment - " +
                        "Does not exist comment with Id " + commentId + " for user with id " + userId));

        comment.setText(createCommentDto.getText());
        Map<Long, String> ownerNames = userRepository.getNamesByIds(List.of(userId));

        return commentMapper.toCommentDto(commentRepository.save(comment), ownerNames.get(userId));
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long userId) {
        commentRepository.deleteByIdAndOwner(commentId, userId);
    }

    @Override
    public List<CommentDto> findCommentByText(String text) {
        List<Comment> allByTextIsLikeIgnoreCase = commentRepository.findAllByTextIsLikeIgnoreCase(text);

        Set<Long> ownerIds = allByTextIsLikeIgnoreCase.stream()
                .map(Comment::getOwner)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> ownerNames = userRepository.getNamesByIds(ownerIds);

        return allByTextIsLikeIgnoreCase.stream()
                .map(comment -> commentMapper.toCommentDto(comment, ownerNames.get(comment.getOwner())))
                .toList();
    }

    @Override
    public CommentDto getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("комментарий не найден"));

        Map<Long, String> ownerNames = userRepository.getNamesByIds(List.of(comment.getOwner()));

        return commentMapper.toCommentDto(comment, ownerNames.get(comment.getOwner()));
    }

    public List<EventFullDto> getTopEventByCommentsCount(int count) {
        return eventRepository.getTopEvents(count);
    }
}