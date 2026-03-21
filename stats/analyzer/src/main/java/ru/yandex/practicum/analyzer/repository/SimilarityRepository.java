package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.analyzer.model.Similarity;

import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
    @Query("SELECT s FROM Similarity s WHERE s.event1 = :eid OR s.event2 = :eid")
    List<Similarity> findByEventId(Long eid);

    @Query("SELECT s FROM Similarity s WHERE s.event1 = :e1 AND s.event2 = :e2")
    Optional<Similarity> findByOrdered(Long e1, Long e2);

    @Query(value = """
        SELECT s.event2, AVG(i.rating) as avg_rating
        FROM similarities s
        JOIN interactions i ON s.event2 = i.event_id
        WHERE s.event1 = :eventId AND i.user_id != :userId
        ORDER BY s.similarity DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Object[]> findSimilarEvents(@Param("eventId") Long eventId, @Param("userId") Long userId, @Param("maxResults") int maxResults);
}
