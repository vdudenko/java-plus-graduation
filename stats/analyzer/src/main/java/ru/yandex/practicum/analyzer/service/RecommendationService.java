package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.*;
import ru.yandex.practicum.analyzer.repository.*;
import java.util.*;
import java.util.stream.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {
    private final SimilarityRepository simRepo;
    private final InteractionRepository intRepo;
    private static final int K = 10;

    public List<EventScore> getSimilarEvents(long eid, long uid, int max) {
        Set<Long> seen = new HashSet<>(intRepo.findEventIdsByUserId(uid));
        return simRepo.findByEventId(eid).stream()
                .map(s -> new EventScore(s.getEvent1() == eid ? s.getEvent2() : s.getEvent1(), s.getSimilarity()))
                .filter(r -> !seen.contains(r.eventId()))
                .sorted(Comparator.comparingDouble(EventScore::score).reversed())
                .limit(max).collect(Collectors.toList());
    }

    public List<EventScore> getRecommendationsForUser(long uid, int max) {
        List<Interaction> recent = intRepo.findByUserIdOrderByTsDesc(uid, K);
        if (recent.isEmpty()) return Collections.emptyList();
        Set<Long> interacted = recent.stream().map(Interaction::getEventId).collect(Collectors.toSet());
        Map<Long, Double> candidates = new HashMap<>();
        for (Interaction i : recent) {
            for (var s : simRepo.findByEventId(i.getEventId())) {
                long cand = Objects.equals(s.getEvent1(), i.getEventId()) ? s.getEvent2() : s.getEvent1();
                if (!interacted.contains(cand))
                    candidates.merge(cand, s.getSimilarity() * i.getRating(), Double::sum);
            }
        }
        return candidates.entrySet().stream()
                .map(e -> new EventScore(e.getKey(), predict(uid, e.getKey(), e.getValue())))
                .sorted(Comparator.comparingDouble(EventScore::score).reversed()).limit(max).collect(Collectors.toList());
    }

    private double predict(long uid, long target, double base) {
        List<Similarity> sims = simRepo.findByEventId(target).stream()
                .filter(s -> {
                    long o = s.getEvent1() == target ? s.getEvent2() : s.getEvent1();
                    return intRepo.findByUserIdAndEventId(uid, o).isPresent();
                }).limit(K).toList();
        if (sims.isEmpty()) return base;
        double ws = 0, ss = 0;
        for (var s : sims) {
            long o = s.getEvent1() == target ? s.getEvent2() : s.getEvent1();
            var i = intRepo.findByUserIdAndEventId(uid, o).orElseThrow();
            ws += s.getSimilarity() * i.getRating(); ss += s.getSimilarity();
        }
        return ss > 0 ? ws / ss : base;
    }

    public Map<Long, Double> getInteractionsCount(List<Long> eids) {
        Map<Long, Double> r = new HashMap<>();
        for (Long id : eids) r.put(id, Optional.ofNullable(intRepo.sumRatingsByEventId(id)).orElse(0.0));
        return r;
    }

    public record EventScore(long eventId, double score) {}
}
