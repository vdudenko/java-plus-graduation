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

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @KafkaListener(
            topics = "stats.user-actions.v1",
            groupId = "analyzer-user-actions-group",
            containerFactory = "userActionsKafkaListenerContainerFactory"
    )
    @Transactional
    public void onUserAction(ConsumerRecord<String, UserActionAvro> record) {
        log.info("📥 [USER-ACTION] Получено сообщение из Kafka");
        log.info("📥 [USER-ACTION] Topic: {}, Partition: {}, Offset: {}",
                record.topic(), record.partition(), record.offset());
        log.info("📥 [USER-ACTION] Key: {}", record.key());

        if (record.value() == null) {
            log.error("❌ [USER-ACTION] Получено null значение!");
            return;
        }

        UserActionAvro action = record.value();
        log.info("📥 [USER-ACTION] UserId: {}, EventId: {}, ActionType: {}, Timestamp: {}",
                action.getUserId(), action.getEventId(), action.getActionType(), action.getTimestamp());

        try {
            double rating = switch (action.getActionType()) {
                case VIEW -> 0.4;
                case REGISTER -> 0.8;
                case LIKE -> 1.0;
            };
            log.info("📊 [USER-ACTION] Рассчитанный rating: {}", rating);

            Instant ts = action.getTimestamp();

            var existingOpt = interactionRepository.findByUserIdAndEventId(action.getUserId(), action.getEventId());

            if (existingOpt.isPresent()) {
                var existing = existingOpt.get();
                log.info("🔄 [USER-ACTION] Найдено существующее взаимодействие. Текущий rating: {}", existing.getRating());

                if (rating > existing.getRating()) {
                    log.info("✅ [USER-ACTION] Обновляем взаимодействие (новый rating выше)");
                    existing.setRating(rating);
                    existing.setTimestamp(ts);
                    interactionRepository.save(existing);
                    log.info("💾 [USER-ACTION] Взаимодействие обновлено в БД");
                } else {
                    log.info("⏭️ [USER-ACTION] Пропускаем обновление (текущий rating выше или равен)");
                }
            } else {
                log.info("➕ [USER-ACTION] Создаём новое взаимодействие");
                Interaction interaction = Interaction.builder()
                        .userId(action.getUserId())
                        .eventId(action.getEventId())
                        .rating(rating)
                        .timestamp(ts)
                        .build();
                interactionRepository.save(interaction);
                log.info("💾 [USER-ACTION] Взаимодействие сохранено в БД. Id: {}", interaction.getId());
            }

            log.info("✅ [USER-ACTION] Обработка завершена успешно");

        } catch (Exception e) {
            log.error("❌ [USER-ACTION] Ошибка при обработке: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "stats.events-similarity.v1",
            groupId = "analyzer-similarities-group",
            containerFactory = "similaritiesKafkaListenerContainerFactory"
    )
    @Transactional
    public void onSimilarity(ConsumerRecord<String, EventSimilarityAvro> record) {
        log.info("📥 [SIMILARITY] Получено сообщение из Kafka");
        log.info("📥 [SIMILARITY] Topic: {}, Partition: {}, Offset: {}",
                record.topic(), record.partition(), record.offset());
        log.info("📥 [SIMILARITY] Key: {}", record.key());

        if (record.value() == null) {
            log.error("❌ [SIMILARITY] Получено null значение!");
            return;
        }

        EventSimilarityAvro avro = record.value();
        log.info("📥 [SIMILARITY] EventA: {}, EventB: {}, Score: {}, Timestamp: {}",
                avro.getEventA(), avro.getEventB(), avro.getScore(), avro.getTimestamp());

        try {
            long e1 = Math.min(avro.getEventA(), avro.getEventB());
            long e2 = Math.max(avro.getEventA(), avro.getEventB());
            log.info("🔢 [SIMILARITY] Нормализованные ID: e1={}, e2={}", e1, e2);

            Instant ts = avro.getTimestamp();

            var existingOpt = similarityRepository.findByOrdered(e1, e2);

            if (existingOpt.isPresent()) {
                var existing = existingOpt.get();
                log.info("🔄 [SIMILARITY] Найдено существующее сходство. Текущий score: {}", existing.getSimilarity());

                existing.setSimilarity(avro.getScore());
                existing.setTimestamp(ts);
                similarityRepository.save(existing);
                log.info("💾 [SIMILARITY] Сходство обновлено в БД");
            } else {
                log.info("➕ [SIMILARITY] Создаём новое сходство");
                Similarity similarity = Similarity.builder()
                        .event1(e1)
                        .event2(e2)
                        .similarity(avro.getScore())
                        .timestamp(ts)
                        .build();
                similarityRepository.save(similarity);
                log.info("💾 [SIMILARITY] Сходство сохранено в БД. Id: {}", similarity.getId());
            }

            log.info("✅ [SIMILARITY] Обработка завершена успешно");

        } catch (Exception e) {
            log.error("❌ [SIMILARITY] Ошибка при обработке: {}", e.getMessage(), e);
            throw e;
        }
    }
}