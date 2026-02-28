package ru.yandex.practicum.main.dto.request;

import lombok.*;
import ru.yandex.practicum.main.enums.RequestStatus;

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
