package ru.yandex.practicum.main.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.main.dto.user.NewUserRequest;
import ru.yandex.practicum.main.dto.user.UserDto;
import ru.yandex.practicum.main.dto.user.UserShortDto;
import ru.yandex.practicum.main.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    User toEntity(NewUserRequest dto);

    UserShortDto toUserShortDto(User user);
}