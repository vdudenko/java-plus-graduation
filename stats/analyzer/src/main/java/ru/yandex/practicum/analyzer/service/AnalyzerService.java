package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Interaction;
import ru.yandex.practicum.analyzer.model.Similarity;
import ru.yandex.practicum.analyzer.repository.InteractionRepository;
import ru.yandex.practicum.analyzer.repository.SimilarityRepository;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.stats.avro.UserActionAvro;
import ru.yandex.practicum.stats.avro.ActionTypeAvro;
import ru.yandex.practicum.stats.proto.RecommendedEventProto;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzerService {
    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @Value("${analyzer.action-weight.view:0.4}")
    private double viewWeight;

    @Value("${analyzer.action-weight.register:0.8}")
    private double registerWeight;

    @Value("${analyzer.action-weight.like:1.0}")
    private double likeWeight;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-user-actions-group")
    @Transactional
    public void processUserAction(UserActionAvro action) {
        log.info("🎯 [SERVICE] Обработка действия пользователя");
        log.info("🎯 [SERVICE] UserId: {}, EventId: {}, Type: {}",
                action.getUserId(), action.getEventId(), action.getActionType());
        double rating = getRatingForAction(action.getActionType());
        log.info("🎯 [SERVICE] Rating: {}", rating);

        interactionRepository.findByUserIdAndEventId(action.getUserId(), action.getEventId())
                .ifPresentOrElse(
                        existing -> {
                            if (rating > existing.getRating()) {
                                existing.setRating(rating);
                                existing.setTimestamp(action.getTimestamp());
                                interactionRepository.save(existing);
                            }
                        },
                        () -> {
                            Interaction interaction = new Interaction();
                            interaction.setUserId(action.getUserId());
                            interaction.setEventId(action.getEventId());
                            interaction.setRating(rating);
                            interaction.setTimestamp(action.getTimestamp());
                            interactionRepository.save(interaction);
                        }
                );
        log.info("✅ [SERVICE] Действие обработано");
    }

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-similarities-group")
    @Transactional
    public void processSimilarity(EventSimilarityAvro similarity) {
        log.info("🎯 [SERVICE] Обработка сходства мероприятий");
        log.info("🎯 [SERVICE] EventA: {}, EventB: {}, Score: {}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());

        long e1 = Math.min(similarity.getEventA(), similarity.getEventB());
        long e2 = Math.max(similarity.getEventA(), similarity.getEventB());

        similarityRepository.findByOrdered(e1, e2)
                .ifPresentOrElse(
                        existing -> {
                            existing.setSimilarity(similarity.getScore());
                            existing.setTimestamp(similarity.getTimestamp());
                            similarityRepository.save(existing);
                        },
                        () -> {
                            Similarity sim = new Similarity();
                            sim.setEvent1(e1);
                            sim.setEvent2(e2);
                            sim.setSimilarity(similarity.getScore());
                            sim.setTimestamp(similarity.getTimestamp());
                            similarityRepository.save(sim);
                        }
                );

        log.info("✅ [SERVICE] Сходство обработано");
    }

    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("🔍 [SERVICE] Запрос рекомендаций для userId: {}, maxResults: {}", userId, maxResults);

        List<Object[]> results = interactionRepository.findRecommendedEvents(userId, maxResults);
        log.info("🔍 [SERVICE] Найдено {} рекомендаций", results.size());
        var result = results.stream()
                .map(obj -> RecommendedEventProto.newBuilder().setEventId((Long) obj[0]).setScore(((Number) obj[1]).doubleValue()).build())
                .toList();
        log.info("✅ [SERVICE] Рекомендации сформированы");
        return result;
    }

    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        log.info("🔍 [SERVICE] Запрос похожих мероприятий. eventId: {}, userId: {}, max: {}",
                eventId, userId, maxResults);
        List<Object[]> results = similarityRepository.findSimilarEvents(eventId, userId, maxResults);
        log.info("🔍 [SERVICE] Найдено {} похожих мероприятий", results.size());

        var result = results.stream()
                .map(obj -> RecommendedEventProto.newBuilder()
                        .setEventId((Long) obj[0])
                        .setScore(((Number) obj[1]).doubleValue())
                        .build())
                .toList();

        log.info("✅ [SERVICE] Похожие мероприятия сформированы");
        return result;
    }

    private double getRatingForAction(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }
}