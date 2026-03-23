package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityPublisher {
    private static final String TOPIC = "stats.events-similarity.v1";
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final SimilarityCalculator calc;

    public void publish(long triggered, Instant timestamp) {
        List<EventSimilarityAvro> updated = calc.getUpdatedSimilarities(triggered, timestamp);

        for (EventSimilarityAvro msg : updated) {
            kafkaTemplate.send(TOPIC, String.valueOf(msg.getEventA()), msg)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("✅ Sent: e1={}, e2={}, sim={}",
                                    msg.getEventA(), msg.getEventB(), msg.getScore());
                        }
                    });
        }
    }
}