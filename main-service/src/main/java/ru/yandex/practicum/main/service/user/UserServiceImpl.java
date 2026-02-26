package ru.yandex.practicum.main.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.main.dto.user.NewUserRequest;
import ru.yandex.practicum.main.dto.user.UserDto;
import ru.yandex.practicum.main.entity.User;
import ru.yandex.practicum.main.exception.ConflictException;
import ru.yandex.practicum.main.exception.UserNotExistException;
import ru.yandex.practicum.main.mapper.UserMapper;
import ru.yandex.practicum.main.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

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
}