package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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

import java.util.*;
import java.util.stream.Collectors;

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

    @KafkaListener(
        topics = "stats.user-actions.v1",
        groupId = "analyzer-user-actions-group",
        containerFactory = "userActionsKafkaListenerContainerFactory"
    )
    @Transactional
    public void processUserAction(UserActionAvro action) {
        double rating = getRatingForAction(action.getActionType());

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
        log.info("[SERVICE] Действие обработано");
    }

    @KafkaListener(
        topics = "stats.events-similarity.v1",
        groupId = "analyzer-similarities-group",
        containerFactory = "similaritiesKafkaListenerContainerFactory"
    )
    @Transactional
    public void processSimilarity(EventSimilarityAvro similarity) {
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

        log.info("[SERVICE] Сходство обработано");
    }

    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, int maxResults) {
        List<Interaction> recentInteractions = interactionRepository
                .findByUserIdOrderByTsDesc(userId, PageRequest.of(0, 20));

        if (recentInteractions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> interactedEvents = recentInteractions.stream()
                .map(Interaction::getEventId)
                .collect(Collectors.toSet());


        List<Similarity> allSimilarities = similarityRepository
                .findAllByEventIds(new ArrayList<>(interactedEvents));

        Map<Long, Double> candidateScores = allSimilarities.stream()
                .map(sim -> {
                    Long candidateId = interactedEvents.contains(sim.getEvent1())
                            ? sim.getEvent2()
                            : sim.getEvent1();
                    return new AbstractMap.SimpleEntry<>(candidateId, sim.getSimilarity());
                })
                .filter(entry -> !interactedEvents.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Double::sum
                ));

        // 4. Возвращаем топ-N
        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        List<Similarity> results = similarityRepository.findAllByEventIdOrdered(eventId);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> candidateIds = results.stream()
                .map(sim -> getPairEventId(sim, eventId))
                .collect(Collectors.toList());

        Set<Long> interactedEventIds = new HashSet<>(
                interactionRepository.findByUserIdAndEventIdIn(userId, candidateIds)
                        .stream()
                        .map(Interaction::getEventId)
                        .collect(Collectors.toList())
        );

        return results.stream()
                .map(sim -> {
                    Long candidateId = getPairEventId(sim, eventId);
                    return new AbstractMap.SimpleEntry<>(candidateId, sim.getSimilarity());
                })
                .filter(entry -> !interactedEventIds.contains(entry.getKey()))
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private Long getPairEventId(Similarity similarity, Long sourceEventId) {
        return similarity.getEvent1().equals(sourceEventId)
                ? similarity.getEvent2()
                : similarity.getEvent1();
    }

    private double getRatingForAction(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }

    public Map<Long, Double> getInteractionsCount(List<Long> eids) {
        return interactionRepository.findByEventIdIn(eids)
                .stream()
                .collect(Collectors.groupingBy(
                        Interaction::getEventId,
                        Collectors.summingDouble(Interaction::getRating)
                ));
    }
}