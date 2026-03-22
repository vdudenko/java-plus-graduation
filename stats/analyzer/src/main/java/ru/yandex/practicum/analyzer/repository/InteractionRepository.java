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

    // ✅ ИСПРАВЛЕНО: используем Pageable вместо int limit
    @Query("SELECT i FROM Interaction i WHERE i.userId = :uid ORDER BY i.timestamp DESC")
    List<Interaction> findByUserIdOrderByTsDesc(@Param("uid") Long uid, Pageable pageable);

    @Query("SELECT i.eventId FROM Interaction i WHERE i.userId = :uid")
    List<Long> findEventIdsByUserId(Long uid);

    @Query("SELECT SUM(i.rating) FROM Interaction i WHERE i.eventId = :eid")
    Double sumRatingsByEventId(Long eid);

    @Query(value = """
        SELECT i.event_id, AVG(i.rating) as avg_rating
        FROM interactions i
        WHERE i.user_id != :userId
          AND i.event_id IN (SELECT s.event2 FROM similarities s WHERE s.event1 IN (SELECT event_id FROM interactions WHERE user_id = :userId))
        GROUP BY i.event_id
        ORDER BY avg_rating DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Object[]> findRecommendedEvents(@Param("userId") Long userId, @Param("maxResults") int maxResults);
}