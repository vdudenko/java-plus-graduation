package ru.yandex.practicum.request.service;

import ru.yandex.practicum.interaction.dto.event.UpdateParticipationRequestListDto;
import ru.yandex.practicum.interaction.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.interaction.dto.request.UpdateParticipationRequestDto;
import ru.yandex.practicum.request.entity.ParticipationRequest;

import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequest> getUserRequests(Long userId);

    ParticipationRequest addRequest(Long userId, Long eventId);

    ParticipationRequest cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getUserRequestsByEventId(Long userId, Long eventId);

    UpdateParticipationRequestListDto updateUserRequestsByEventId(Long userId, Long eventId, UpdateParticipationRequestDto updateParticipationRequestDto);
}
