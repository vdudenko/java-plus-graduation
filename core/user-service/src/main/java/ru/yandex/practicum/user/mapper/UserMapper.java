package ru.yandex.practicum.user.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.interaction.dto.user.NewUserRequest;
import ru.yandex.practicum.interaction.dto.user.UserDto;
import ru.yandex.practicum.interaction.dto.user.UserShortDto;
import ru.yandex.practicum.user.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    User toEntity(NewUserRequest dto);

    UserShortDto toUserShortDto(User user);
}