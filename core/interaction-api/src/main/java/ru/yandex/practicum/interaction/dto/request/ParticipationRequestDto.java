package ru.yandex.practicum.interaction.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.interaction.enums.RequestStatus;
import ru.yandex.practicum.interaction.util.DateFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateFormatter.PATTERN)
    private String created;

    private Long requester;

    private Long event;

    private RequestStatus status;
}
