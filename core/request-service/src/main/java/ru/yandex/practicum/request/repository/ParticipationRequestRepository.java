package ru.yandex.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.interaction.enums.RequestStatus;
import ru.yandex.practicum.request.entity.ParticipationRequest;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequester(Long userId);

    Boolean existsByRequesterAndEvent(Long requesterId, Long eventId);

    List<ParticipationRequest> findAllByRequesterAndEvent(Long userId, Long eventId);

    List<ParticipationRequest> findByEventAndStatus(Long eventId, RequestStatus status);

    Long countByEventAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findByEvent(Long eventId);
}