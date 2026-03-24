package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.analyzer.model.Interaction;

import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT i FROM Interaction i WHERE i.userId = :uid ORDER BY i.timestamp DESC")
    List<Interaction> findByUserIdOrderByTsDesc(@Param("uid") Long uid, Pageable pageable);

    @Query("SELECT i.eventId FROM Interaction i WHERE i.userId = :uid")
    List<Long> findEventIdsByUserId(Long uid);

    @Query("SELECT i FROM Interaction i WHERE i.eventId IN :eventIds")
    List<Interaction> findByEventIdIn(@Param("eventIds") List<Long> eventIds);

    // Получить взаимодействия пользователя для списка событий
    @Query("SELECT i FROM Interaction i WHERE i.userId = :uid AND i.eventId IN :eventIds")
    List<Interaction> findByUserIdAndEventIdIn(@Param("uid") Long uid, @Param("eventIds") List<Long> eventIds);
}