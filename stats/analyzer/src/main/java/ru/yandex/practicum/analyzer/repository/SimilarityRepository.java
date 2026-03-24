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

    @Query("SELECT s FROM Similarity s " +
            "WHERE (s.event1 = :eid OR s.event2 = :eid) " +
            "AND s.similarity > 0 " +
            "ORDER BY s.similarity DESC")
    List<Similarity> findAllByEventIdOrdered(@Param("eid") Long eid);

    @Query("SELECT s FROM Similarity s WHERE s.event1 IN :eventIds OR s.event2 IN :eventIds")
    List<Similarity> findAllByEventIds(@Param("eventIds") List<Long> eventIds);
}
