package ru.yandex.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.interaction.dto.user.NewUserRequest;
import ru.yandex.practicum.interaction.dto.user.UserDto;
import ru.yandex.practicum.interaction.exception.ConflictException;
import ru.yandex.practicum.interaction.exception.UserNotExistException;
import ru.yandex.practicum.user.entity.User;
import ru.yandex.practicum.user.mapper.UserMapper;
import ru.yandex.practicum.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        Pageable page = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<User> users;

        if (ids == null || ids.isEmpty()) {
            Page<User> pageResult = repository.findAll(page);
            users = pageResult.getContent();
        } else {
            users = repository.findAllById(ids)
                    .stream()
                    .sorted(Comparator.comparingLong(User::getId))
                    .skip(from)
                    .limit(size)
                    .toList();
        }

        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }


    @Override
    public UserDto createUser(NewUserRequest newUser) {
        String email = newUser.getEmail();
        if (repository.existsByEmail(email)) {
            throw new ConflictException("User with email=%s already exists.".formatted(email));
        }

        User user = userMapper.toEntity(newUser);
        return userMapper.toDto(repository.save(user));
    }

    @Override
    public void deleteUser(Long userId) {
        if (!repository.existsById(userId)) {
            throw new UserNotExistException("User with id=%d not found.".formatted(userId));
        }

        repository.deleteById(userId);
    }

    @Override
    public boolean exists(Long userId) {
        return repository.existsById(userId);
    }

    @Override
    public Map<Long, String> findNamesByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = repository.findAllById(userIds);

        return users.stream()
                .filter(user -> user != null && user.getId() != null)
                .collect(Collectors.toMap(
                        User::getId,
                        user -> user.getName() != null ? user.getName() : "",
                        (existing, replacement) -> existing
                ));
    }
}