package ru.yandex.practicum.request.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.event.UpdateParticipationRequestListDto;
import ru.yandex.practicum.interaction.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.interaction.dto.request.UpdateParticipationRequestDto;
import ru.yandex.practicum.request.mapper.ParticipationRequestMapper;
import ru.yandex.practicum.request.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
public class PrivateRequestController {

    private final ParticipationRequestService service;
    private final ParticipationRequestMapper participationRequestMapper;

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getRequests(@PathVariable("userId") @NotNull @Positive Long userId) {
        return service.getUserRequests(userId).stream()
                .map(participationRequestMapper::toDto)
                .toList();
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(@PathVariable("userId") @NotNull @Positive Long userId,
                                              @RequestParam("eventId") @NotNull @Positive Long eventId) {
        return participationRequestMapper.toDto(service.addRequest(userId, eventId));
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable("userId") @NotNull @Positive Long userId,
                                                 @PathVariable("requestId") @NotNull @Positive Long requestId) {
        return participationRequestMapper.toDto(service.cancelRequest(userId, requestId));
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getUserRequestsByEventId(@PathVariable Long userId,
                                                                  @PathVariable Long eventId) {
        return service.getUserRequestsByEventId(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public UpdateParticipationRequestListDto updateUserRequestsByEventId(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateParticipationRequestDto updateParticipationRequestDto) {

        log.info("Received update request: userId={}, eventId={}, dto={}",
                userId, eventId, updateParticipationRequestDto.toString());

        return service.updateUserRequestsByEventId(userId, eventId, updateParticipationRequestDto);
    }
}