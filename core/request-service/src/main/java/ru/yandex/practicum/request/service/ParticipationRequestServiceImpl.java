package ru.yandex.practicum.request.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.dto.event.EventFullDto;
import ru.yandex.practicum.interaction.dto.event.UpdateParticipationRequestListDto;
import ru.yandex.practicum.interaction.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.interaction.dto.request.UpdateParticipationRequestDto;
import ru.yandex.practicum.interaction.enums.EventState;
import ru.yandex.practicum.interaction.enums.RequestStatus;
import ru.yandex.practicum.interaction.exception.ConflictException;
import ru.yandex.practicum.interaction.exception.ForbiddenException;
import ru.yandex.practicum.interaction.exception.NotFoundException;
import ru.yandex.practicum.request.entity.ParticipationRequest;
import ru.yandex.practicum.request.feign.EventClient;
import ru.yandex.practicum.request.mapper.ParticipationRequestMapper;
import ru.yandex.practicum.request.feign.UserClient;
import ru.yandex.practicum.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final UserClient userRepository;
    private final EventClient eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper participationRequestMapper;

    @Transactional
    public List<ParticipationRequest> getUserRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=%d was not found".formatted(userId));
        }

        return requestRepository.findByRequester(userId);
    }

    @Transactional
    public ParticipationRequest addRequest(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=%d was not found".formatted(userId));
        }

        try {
            EventFullDto event = eventRepository.getEventWithoutStatistic(eventId);

            if (event.getInitiator().equals(userId)) {
                throw new ConflictException("Initiator cannot request own event");
            }

            if (!event.getState().equals(EventState.PUBLISHED.name())) {
                throw new ConflictException("Event is not published");
            }

            if (requestRepository.existsByRequesterAndEvent(userId, eventId)) {
                throw new ConflictException("Request already exists");
            }
            log.info("Event limit {}, confirmed request {}, event id {}", event.getParticipantLimit(), event.getConfirmedRequests(), event.getId());
            if (event.getParticipantLimit() > 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
                throw new ConflictException("Participant limit reached");
            }

            ParticipationRequest request = new ParticipationRequest();
            request.setRequester(userId);
            request.setEvent(event.getId());
            request.setCreated(LocalDateTime.now());
            if (event.getRequestModeration() && event.getParticipantLimit() != 0) {
                request.setStatus(RequestStatus.PENDING);
            } else {
                request.setStatus(RequestStatus.CONFIRMED);
                log.info("Update event {}", event.getConfirmedRequests() + 1);
                event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                updateEventConfirmedRequests(eventId, event.getConfirmedRequests());
            }

            return requestRepository.save(request);
        } catch (FeignException e) {
            log.info("Получение событияЖ" + e.getMessage());
            if (e.status() == 404) {
                throw new NotFoundException("Event with id=" + eventId + " was not found");
            }
            throw new RuntimeException("Error calling event service", e);
        }
    }

    @Transactional
    public ParticipationRequest cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=%d was not found".formatted(requestId)));

        if (!request.getRequester().equals(userId)) {
            throw new ForbiddenException("Cannot cancel another user's request");
        }

        request.setStatus(RequestStatus.CANCELED);
        return requestRepository.save(request);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequestsByEventId(Long userId, Long eventId) {
        try {
            EventFullDto event = eventRepository.getEventWithoutStatistic(eventId);

            if (!event.getInitiator().equals(userId)) {
                throw new ForbiddenException("User is not the initiator of the event");
            }

            List<ParticipationRequest> requests = requestRepository.findByEvent(eventId);

            return requests.stream()
                    .map(participationRequestMapper::toDto)
                    .collect(Collectors.toList());
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new NotFoundException("Event with id=" + eventId + " was not found");
            }
            throw new RuntimeException("Error calling event service", e);
        }
    }

    @Override
    @Transactional
    public UpdateParticipationRequestListDto updateUserRequestsByEventId(Long userId, Long eventId, UpdateParticipationRequestDto updateDto) {
        try {
            EventFullDto event = eventRepository.getEventWithoutStatistic(eventId);

            if (!event.getInitiator().equals(userId)) {
                throw new ForbiddenException("User is not the initiator of the event");
            }

            List<Long> requestIds = updateDto.getRequestsId();
            if (requestIds == null || requestIds.isEmpty()) {
                List<ParticipationRequest> pendingRequests = requestRepository.findByEventAndStatus(eventId, RequestStatus.PENDING);
                if (pendingRequests.isEmpty()) {
                    throw new ConflictException("No pending requests found for this event");
                }
                requestIds = pendingRequests.stream()
                        .map(ParticipationRequest::getId)
                        .collect(Collectors.toList());
            }

            List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);

            if (requests.size() != requestIds.size()) {
                throw new NotFoundException("Some request IDs were not found");
            }

            for (ParticipationRequest request : requests) {
                if (!request.getEvent().equals(eventId)) {
                    throw new ForbiddenException("Request with id=" + request.getId() + " does not belong to this event");
                }

                if (updateDto.getStatus() == RequestStatus.CONFIRMED && request.getStatus() == RequestStatus.CONFIRMED) {
                    throw new ConflictException("Request is already confirmed");
                }
            }

            RequestStatus newStatus = updateDto.getStatus();

            if (newStatus == RequestStatus.CONFIRMED) {
                long currentConfirmed = requestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
                long limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
                if (limit == 0) {
                    requests.forEach(r -> r.setStatus(RequestStatus.CONFIRMED));
                } else {
                    long availableSlots = limit - currentConfirmed;

                    if (availableSlots <= 0) {
                        requests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
                    } else {
                        List<ParticipationRequest> toConfirm = requests.stream()
                                .filter(r -> r.getStatus() != RequestStatus.CONFIRMED)
                                .limit(availableSlots)
                                .collect(Collectors.toList());

                        List<ParticipationRequest> toReject = requests.stream()
                                .filter(r -> !toConfirm.contains(r))
                                .collect(Collectors.toList());

                        toConfirm.forEach(r -> r.setStatus(RequestStatus.CONFIRMED));
                        toReject.forEach(r -> r.setStatus(RequestStatus.REJECTED));
                    }
                }
            } else {
                requests.forEach(r -> r.setStatus(newStatus));
            }
            List<ParticipationRequest> updatedRequests = requestRepository.saveAll(requests);
            updateEventConfirmedRequests(event);
            UpdateParticipationRequestListDto result = new UpdateParticipationRequestListDto();

            for (ParticipationRequest request : updatedRequests) {
                if (request.getStatus() == RequestStatus.CONFIRMED) {
                    result.getConfirmedRequests().add(participationRequestMapper.toDto(request));
                } else if (request.getStatus() == RequestStatus.REJECTED) {
                    result.getRejectedRequests().add(participationRequestMapper.toDto(request));
                }
            }

            return result;

        } catch (FeignException e) {
            log.info("Ошибка" + e.getMessage());
            if (e.status() == 404) {
                throw new NotFoundException("Event with id=" + eventId + " was not found");
            }
            throw new RuntimeException("Error calling event service", e);
        }
    }

    private void updateEventConfirmedRequests(EventFullDto event) {
        Long confirmedCount = requestRepository.countByEventAndStatus(event.getId(), RequestStatus.CONFIRMED);
        eventRepository.updateEventConfirmedRequests(event.getId(), confirmedCount);
        log.info("Updated confirmedRequests to {} for event {}", confirmedCount, event.getId());
    }

    private void updateEventConfirmedRequests(Long eventId, Long confirmedCount) {
        eventRepository.updateEventConfirmedRequests(eventId, confirmedCount);
    }
}