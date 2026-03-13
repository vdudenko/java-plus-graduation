package ru.yandex.practicum.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.event.entity.Event;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByInitiator(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiator(Long eventId, Long userId);

    List<Event> findByIdIn(List<Long> eventIds);

    Boolean existsByCategory(Long categoryId);

    Optional<Event> findByIdAndPublishedOnIsNotNull(Long id);

    @Query("""
                SELECT e
                FROM Event e
                ORDER BY e.views DESC
                LIMIT :count
            """)
    List<Event> getTopByComments(@Param("count") int count);

}