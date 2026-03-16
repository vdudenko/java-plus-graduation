package ru.yandex.practicum.interaction.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import static ru.yandex.practicum.interaction.util.DateFormatter.PATTERN;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {

    private Long id;

    @NotBlank
    private String annotation;

    @NotNull
    private Long category;


    private Long confirmedRequests;

    @NotBlank
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN)
    private String eventDate;

    @NotNull
    private Long initiator;

    @NotNull
    private Boolean paid;

    @NotNull
    private String title;

    private Long views;
}
