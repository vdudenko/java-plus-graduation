package ru.yandex.practicum.user.service;


import ru.yandex.practicum.interaction.dto.user.NewUserRequest;
import ru.yandex.practicum.interaction.dto.user.UserDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto createUser(NewUserRequest newUser);

    void deleteUser(Long userId);

    boolean exists(Long userId);

    Map<Long, String> findNamesByIds(Collection<Long> userIds);
}