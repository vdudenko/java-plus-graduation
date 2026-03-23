package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SimilarityCalculator {

    private final Map<Long, Map<Long, Double>> eventUserRatings = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventRatingSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minRatingSums = new ConcurrentHashMap<>();

    public void processUserAction(long userId, long eventId, double rating) {
        Map<Long, Double> userRatings = eventUserRatings.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());

        // ✅ СУММИРУЕМ все рейтинги пользователя для события
        double currentRating = userRatings.getOrDefault(userId, 0.0);
        userRatings.put(userId, currentRating + rating);

        // Обновляем сумму рейтингов события
        eventRatingSums.compute(eventId, (k, v) -> (v == null ? 0.0 : v) + rating);

        for (Long other : eventUserRatings.keySet()) {
            if (other.equals(eventId)) {
                continue;
            }
            if (!eventUserRatings.get(other).containsKey(userId)) {
                continue;
            }
            updatePair(eventId, other);
        }
    }

    private void updatePair(long a, long b) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);
        double sum = calculateMinSum(first, second);
        minRatingSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>()).put(second, sum);
    }

    private double calculateMinSum(long e1, long e2) {
        Map<Long, Double> r1 = eventUserRatings.getOrDefault(e1, Map.of());
        Map<Long, Double> r2 = eventUserRatings.getOrDefault(e2, Map.of());
        double sum = 0;
        for (Long u : r1.keySet()) {
            if (r2.containsKey(u)) {
                sum += Math.min(r1.get(u), r2.get(u));
            }
        }
        return sum;
    }

    public double calculateSimilarity(long e1, long e2) {
        Map<Long, Double> r1 = eventUserRatings.getOrDefault(e1, Map.of());
        Map<Long, Double> r2 = eventUserRatings.getOrDefault(e2, Map.of());

        double dot = 0, norm1 = 0, norm2 = 0;

        // Перебираем только общих пользователей
        Set<Long> commonUsers = new HashSet<>(r1.keySet());
        commonUsers.retainAll(r2.keySet());

        for (Long u : commonUsers) {
            double v1 = r1.get(u);
            double v2 = r2.get(u);
            dot += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) return 0.0;

        // ✅ Альтернативная формула
        double sim = dot / Math.sqrt(norm1 * norm2);

        // ✅ Округление через BigDecimal для точности
        return BigDecimal.valueOf(sim).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public Map<Long, Double> getEventRatingSums() {
        return new ConcurrentHashMap<>(eventRatingSums);
    }

    public Set<Long> getUsersForEvent(long eventId) {
        return eventUserRatings.getOrDefault(eventId, Map.of()).keySet();
    }
}