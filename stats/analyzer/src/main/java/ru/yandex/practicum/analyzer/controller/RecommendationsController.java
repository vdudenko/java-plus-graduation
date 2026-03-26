package ru.yandex.practicum.analyzer.controller;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.stats.proto.*;
import ru.yandex.practicum.analyzer.service.AnalyzerService;

@GrpcService
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final AnalyzerService analyzerService;

    public RecommendationsController(AnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        analyzerService.getRecommendationsForUser(request.getUserId(), request.getMaxResults())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        analyzerService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            var counts = analyzerService.getInteractionsCount(request.getEventIdList());

            for (Long eventId : request.getEventIdList()) {
                double score = counts.getOrDefault(eventId, 0.0);
                RecommendedEventProto response = RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(score)
                        .build();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}