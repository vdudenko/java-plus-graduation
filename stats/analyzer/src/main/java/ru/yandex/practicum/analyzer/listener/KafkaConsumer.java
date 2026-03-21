package ru.yandex.practicum.analyzer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.stats.avro.*;
import ru.yandex.practicum.analyzer.model.*;
import ru.yandex.practicum.analyzer.repository.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-group")
    @Transactional
    public void onUserAction(ConsumerRecord<String, UserActionAvro> rec) {
        try {
            var a = rec.value();
            double r = switch (a.getActionType()) {
                case VIEW -> 0.4;
                case REGISTER -> 0.8;
                case LIKE -> 1.0;
            };
            interactionRepository.findByUserIdAndEventId(a.getUserId(), a.getEventId())
                    .ifPresentOrElse(
                            ex -> {
                                if (r > ex.getRating()) {
                                    ex.setRating(r);
                                    ex.setTimestamp(a.getTimestamp());
                                    interactionRepository.save(ex);
                                }
                            },
                            () -> {
                                interactionRepository.save(
                                        Interaction.builder()
                                                .userId(a.getUserId())
                                                .eventId(a.getEventId())
                                                .rating(r)
                                                .timestamp(a.getTimestamp())
                                                .build()
                                );
                            }
                    );
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-group")
    @Transactional
    public void onSimilarity(ConsumerRecord<String, EventSimilarityAvro> rec) {
        try {
            var avro = rec.value();
            long e1 = Math.min(avro.getEventA(), avro.getEventB());
            long e2 = Math.max(avro.getEventA(), avro.getEventB());
            similarityRepository.findByOrdered(e1, e2)
                    .ifPresentOrElse(
                            ex -> {
                                ex.setSimilarity(avro.getScore());
                                ex.setTimestamp(avro.getTimestamp());
                                similarityRepository.save(ex);
                            },
                            () -> {
                                similarityRepository.save(
                                        Similarity.builder()
                                                .event1(e1)
                                                .event2(e2)
                                                .similarity(avro.getScore())
                                                .timestamp(avro.getTimestamp())
                                                .build()
                                );
                            }
                    );
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }
}