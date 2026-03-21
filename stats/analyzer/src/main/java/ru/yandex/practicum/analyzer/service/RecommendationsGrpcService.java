package ru.yandex.practicum.analyzer.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.stats.proto.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService rs;

    @Override public void getRecommendationsForUser(UserPredictionsRequestProto req, StreamObserver<RecommendedEventProto> obs) {
        try {
            for (var r : rs.getRecommendationsForUser(req.getUserId(), req.getMaxResults()))
                obs.onNext(RecommendedEventProto.newBuilder().setEventId(r.eventId()).setScore(r.score()).build());
            obs.onCompleted();
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e); obs.onError(e);
        }
    }

    @Override public void getSimilarEvents(SimilarEventsRequestProto req, StreamObserver<RecommendedEventProto> obs) {
        try {
            for (var r : rs.getSimilarEvents(req.getEventId(), req.getUserId(), req.getMaxResults()))
                obs.onNext(RecommendedEventProto.newBuilder().setEventId(r.eventId()).setScore(r.score()).build());
            obs.onCompleted();
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e); obs.onError(e);
        }
    }

    @Override public void getInteractionsCount(InteractionsCountRequestProto req, StreamObserver<RecommendedEventProto> obs) {
        try {
            var counts = rs.getInteractionsCount(req.getEventIdList());
            for (Long id : req.getEventIdList())
                obs.onNext(RecommendedEventProto.newBuilder().setEventId(id).setScore(counts.getOrDefault(id, 0.0)).build());
            obs.onCompleted();
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e); obs.onError(e);
        }
    }
}
