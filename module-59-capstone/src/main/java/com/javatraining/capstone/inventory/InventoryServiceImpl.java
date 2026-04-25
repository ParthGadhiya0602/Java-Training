package com.javatraining.capstone.inventory;

import com.javatraining.capstone.proto.InventoryProto.StockRequest;
import com.javatraining.capstone.proto.InventoryProto.StockResponse;
import com.javatraining.capstone.proto.InventoryServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;

/**
 * gRPC server implementation for inventory stock checks.
 *
 * In a real system this would query a database or call a separate inventory service.
 * The in-memory map simulates stable stock levels across requests.
 */
@GrpcService
public class InventoryServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

    private static final Map<String, Integer> STOCK = Map.of(
            "PROD-1", 100,
            "PROD-2", 50,
            "PROD-3", 200
    );

    @Override
    public void checkStock(StockRequest request, StreamObserver<StockResponse> responseObserver) {
        int stock = STOCK.getOrDefault(request.getProductId(), 0);
        boolean available = stock >= request.getQuantity();
        responseObserver.onNext(StockResponse.newBuilder()
                .setAvailable(available)
                .setCurrentStock(stock)
                .build());
        responseObserver.onCompleted();
    }
}
