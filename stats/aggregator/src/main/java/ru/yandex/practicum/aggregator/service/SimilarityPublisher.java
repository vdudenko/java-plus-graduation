package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityPublisher {
    private static final String TOPIC = "stats.events-similarity.v1";
    private static final double CHANGE_THRESHOLD = 0.01; // Порог изменения

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final SimilarityCalculator calc;

    private final Map<String, Double> lastSent = new ConcurrentHashMap<>();

    public void publish(long triggered) {
        // Получаем пользователей, которые взаимодействовали с triggered event
        Set<Long> triggeredUsers = getUsersForEvent(triggered);
        if (triggeredUsers.isEmpty()) return;

        for (Long other : calc.getEventRatingSums().keySet()) {
            if (other.equals(triggered)) continue;

            Set<Long> otherUsers = getUsersForEvent(other);
            if (Collections.disjoint(triggeredUsers, otherUsers)) {
                continue;
            }

            long e1 = Math.min(triggered, other);
            long e2 = Math.max(triggered, other);
            double newSim = calc.calculateSimilarity(triggered, other);
            newSim = Math.round(newSim * 100.0) / 100.0;

            String key = e1 + ":" + e2;
            Double oldSim = lastSent.get(key);

            if (oldSim == null || Math.abs(newSim - oldSim) >= CHANGE_THRESHOLD) {
                EventSimilarityAvro msg = EventSimilarityAvro.newBuilder()
                        .setEventA(e1)
                        .setEventB(e2)
                        .setScore(newSim)
                        .setTimestamp(Instant.now())
                        .build();

                double finalNewSim = newSim;
                kafkaTemplate.send(TOPIC, String.valueOf(e1), msg)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                lastSent.put(key, finalNewSim);
                                log.debug("Sent: e1={}, e2={}, sim={}", e1, e2, finalNewSim);
                            }
                        });
            }
        }
    }

    private Set<Long> getUsersForEvent(long eventId) {
        return calc.getUsersForEvent(eventId);
    }
}