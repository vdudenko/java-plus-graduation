package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityPublisher {
    private static final String TOPIC = "stats.events-similarity.v1";
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    public void publish(EventSimilarityAvro sim) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(sim.getEventA()), sim);
            log.debug("Sent: e1={}, e2={}, sim={}",
                    sim.getEventA(), sim.getEventB(), sim.getScore());
        } catch (Exception e) {
            log.error("Failed to send: e1={}, e2={}, error={}",
                    sim.getEventA(), sim.getEventB(), e.getMessage(), e);
        }
    }
}