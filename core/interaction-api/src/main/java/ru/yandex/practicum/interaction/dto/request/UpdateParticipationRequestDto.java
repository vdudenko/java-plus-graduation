package ru.yandex.practicum.interaction.dto.request;

import lombok.*;
import ru.yandex.practicum.interaction.enums.RequestStatus;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateParticipationRequestDto {

    private List<Long> requestsId;

    private RequestStatus status;

}
