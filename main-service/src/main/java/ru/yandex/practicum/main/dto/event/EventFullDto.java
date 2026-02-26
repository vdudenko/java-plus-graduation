package ru.yandex.practicum.main.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.main.dto.category.CategoryDto;
import ru.yandex.practicum.main.dto.user.UserShortDto;

import static ru.yandex.practicum.main.util.DateFormatter.PATTERN;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {

    private Long id;

    @NotBlank
    private String annotation;

    @NotNull
    private CategoryDto category;


    private Long confirmedRequests;

    private String description;

    @NotBlank
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN)
    private String eventDate;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN)
    private String createdOn;

    @NotNull
    private UserShortDto initiator;

    @NotNull
    private LocationDto location;

    @NotNull
    private Boolean paid;

    private Integer participantLimit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN)
    private String publishedOn;

    private Boolean requestModeration;

    private String state;

    @NotBlank
    private String title;

    private Long views;

}
