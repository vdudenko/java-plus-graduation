package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityPublisher {
    private static final String TOPIC = "stats.events-similarity.v1";
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    public void publish(List<EventSimilarityAvro> similarities) {
        for (EventSimilarityAvro sim : similarities) {
            kafkaTemplate.send(TOPIC, String.valueOf(sim.getEventA()), sim)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("✅ Sent: e1={}, e2={}, sim={}",
                                    sim.getEventA(), sim.getEventB(), sim.getScore());
                        }
                    });
        }
    }
}