package ru.yandex.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.service.CollectorService;
import ru.yandex.practicum.stats.proto.UserActionProto;
import ru.yandex.practicum.stats.proto.UserActionControllerGrpc;

@GrpcService
public class CollectorController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final CollectorService collectorService;

    public CollectorController(CollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @Override
    public void collectUserAction(UserActionProto request,
                                  StreamObserver<Empty> responseObserver) {
        collectorService.collectUserAction(request);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}