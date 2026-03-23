package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityPublisher {
    private static final String TOPIC = "stats.events-similarity.v1";

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final SimilarityCalculator calc;

    public void publish(long triggered) {
        Set<Long> triggeredUsers = calc.getUsersForEvent(triggered);
        if (triggeredUsers.isEmpty()) {
            log.debug("No users for triggered event {}, skipping", triggered);
            return;
        }

        for (Long other : calc.getEventRatingSums().keySet()) {
            if (other.equals(triggered)) continue;

            Set<Long> otherUsers = calc.getUsersForEvent(other);
            if (Collections.disjoint(triggeredUsers, otherUsers)) {
                continue; // Нет общих пользователей
            }

            long e1 = Math.min(triggered, other);
            long e2 = Math.max(triggered, other);
            double sim = calc.calculateSimilarity(triggered, other);

            EventSimilarityAvro msg = EventSimilarityAvro.newBuilder()
                    .setEventA(e1)
                    .setEventB(e2)
                    .setScore(sim)
                    .setTimestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, String.valueOf(e1), msg)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("✅ Sent: e1={}, e2={}, sim={}", e1, e2, sim);
                        } else {
                            log.error("❌ Failed to send: e1={}, e2={}, error={}", e1, e2, ex.getMessage());
                        }
                    });
        }
    }
}