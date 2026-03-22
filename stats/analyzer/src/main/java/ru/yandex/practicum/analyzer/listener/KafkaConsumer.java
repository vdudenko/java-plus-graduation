package ru.yandex.practicum.analyzer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Interaction;
import ru.yandex.practicum.analyzer.model.Similarity;
import ru.yandex.practicum.analyzer.repository.InteractionRepository;
import ru.yandex.practicum.analyzer.repository.SimilarityRepository;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.stats.avro.UserActionAvro;

import java.time.Instant;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-user-actions-group")
    @Transactional
    public void onUserAction(ConsumerRecord<String, UserActionAvro> rec) {
        try {
            var action = rec.value();

            // Конвертация веса действия
            double rating = switch (action.getActionType()) {
                case VIEW -> 0.4;
                case REGISTER -> 0.8;
                case LIKE -> 1.0;
            };

            // Конвертация timestamp из Avro (long ms) в ZonedDateTime
            Instant ts = action.getTimestamp();

            interactionRepository.findByUserIdAndEventId(action.getUserId(), action.getEventId())
                    .ifPresentOrElse(
                            existing -> {
                                if (rating > existing.getRating()) {
                                    existing.setRating(rating);
                                    existing.setTimestamp(ts);
                                    interactionRepository.save(existing);
                                }
                            },
                            () -> {
                                Interaction interaction = Interaction.builder()
                                        .userId(action.getUserId())
                                        .eventId(action.getEventId())
                                        .rating(rating)
                                        .timestamp(ts)
                                        .build();
                                interactionRepository.save(interaction);
                            }
                    );
        } catch (Exception e) {
            log.error("Error processing user action: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-similarities-group")
    @Transactional
    public void onSimilarity(ConsumerRecord<String, EventSimilarityAvro> rec) {
        try {
            var avro = rec.value();

            // Упорядочивание eventA < eventB
            long e1 = Math.min(avro.getEventA(), avro.getEventB());
            long e2 = Math.max(avro.getEventA(), avro.getEventB());

            // Конвертация timestamp
            Instant ts = avro.getTimestamp();

            similarityRepository.findByOrdered(e1, e2)
                    .ifPresentOrElse(
                            existing -> {
                                existing.setSimilarity(avro.getScore());
                                existing.setTimestamp(ts);
                                similarityRepository.save(existing);
                            },
                            () -> {
                                Similarity similarity = Similarity.builder()
                                        .event1(e1)
                                        .event2(e2)
                                        .similarity(avro.getScore())
                                        .timestamp(ts)
                                        .build();
                                similarityRepository.save(similarity);
                            }
                    );
        } catch (Exception e) {
            log.error("Error processing similarity: {}", e.getMessage(), e);
        }
    }
}