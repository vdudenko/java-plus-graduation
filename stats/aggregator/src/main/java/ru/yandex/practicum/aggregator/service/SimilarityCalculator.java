package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SimilarityCalculator {

    private final Map<Long, Map<Long, Double>> eventUserRatings = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventRatingSums = new ConcurrentHashMap<>();
    // ✅ Изменить структуру: строковый ключ вместо вложенной мапы
    private final Map<String, Double> minRatingSums = new ConcurrentHashMap<>();

    public void processUserAction(long userId, long eventId, double rating) {
        Map<Long, Double> userRatings = eventUserRatings.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Double oldWeight = userRatings.get(userId);

        // Не обновляем, если новый рейтинг не больше старого
        if (oldWeight != null && rating <= oldWeight) {
            return;
        }

        double oldW = oldWeight != null ? oldWeight : 0.0;
        userRatings.put(userId, rating);

        // ✅ Инкрементальное обновление суммы рейтингов
        eventRatingSums.compute(eventId, (k, v) -> (v == null ? 0.0 : v) + (rating - oldW));

        // ✅ Инкрементальное обновление пар
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserRatings.entrySet()) {
            Long otherEvent = entry.getKey();
            if (otherEvent.equals(eventId)) continue;

            Map<Long, Double> otherRatings = entry.getValue();
            if (!otherRatings.containsKey(userId)) continue;

            double otherWeight = otherRatings.get(userId);
            updatePairIncremental(eventId, otherEvent, userId, oldW, rating, otherWeight);
        }
    }

    private void updatePairIncremental(long eventA, long eventB, long userId,
                                       double oldWeightA, double newWeightA, double weightB) {
        String pairKey = buildPairKey(eventA, eventB);

        double oldMin = Math.min(oldWeightA, weightB);
        double newMin = Math.min(newWeightA, weightB);

        // Если min не изменился — не обновляем
        if (oldMin == newMin) {
            return;
        }

        // ✅ Инкрементальное обновление: вычитаем старый min, добавляем новый
        double currentSum = minRatingSums.getOrDefault(pairKey, 0.0);
        double updatedSum = currentSum - oldMin + newMin;
        minRatingSums.put(pairKey, updatedSum);
    }

    // ✅ Правильная формула: minSum / (sqrt(sumA) * sqrt(sumB))
    public double calculateSimilarity(long e1, long e2) {
        String pairKey = buildPairKey(e1, e2);
        Double minSum = minRatingSums.getOrDefault(pairKey, 0.0);

        if (minSum == 0.0) return 0.0;

        Double sum1 = eventRatingSums.getOrDefault(e1, 0.0);
        Double sum2 = eventRatingSums.getOrDefault(e2, 0.0);

        if (sum1 == 0.0 || sum2 == 0.0) return 0.0;

        return minSum / (Math.sqrt(sum1) * Math.sqrt(sum2));
    }

    // ✅ Возвращает список изменённых пар для отправки
    public List<EventSimilarityAvro> getUpdatedSimilarities(long triggered, Instant timestamp) {
        List<EventSimilarityAvro> results = new ArrayList<>();

        for (Long other : eventRatingSums.keySet()) {
            if (other.equals(triggered)) continue;

            String pairKey = buildPairKey(triggered, other);
            if (!minRatingSums.containsKey(pairKey)) continue;

            double similarity = calculateSimilarity(triggered, other);
            long e1 = Math.min(triggered, other);
            long e2 = Math.max(triggered, other);

            results.add(EventSimilarityAvro.newBuilder()
                    .setEventA(e1)
                    .setEventB(e2)
                    .setScore(similarity)
                    .setTimestamp(timestamp)
                    .build());
        }

        return results;
    }

    private String buildPairKey(Long id1, Long id2) {
        return id1 < id2 ? id1 + "_" + id2 : id2 + "_" + id1;
    }
}