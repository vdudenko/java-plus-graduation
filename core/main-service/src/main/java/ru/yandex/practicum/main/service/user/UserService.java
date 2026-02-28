package ru.yandex.practicum.main.service.user;

import ru.yandex.practicum.main.dto.user.NewUserRequest;
import ru.yandex.practicum.main.dto.user.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto createUser(NewUserRequest newUser);

    void deleteUser(Long userId);
}