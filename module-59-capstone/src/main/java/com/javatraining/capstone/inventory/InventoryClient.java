package com.javatraining.capstone.inventory;

import com.javatraining.capstone.proto.InventoryProto.StockRequest;
import com.javatraining.capstone.proto.InventoryProto.StockResponse;
import com.javatraining.capstone.proto.InventoryServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.stereotype.Component;

/**
 * gRPC client wrapper. OrderService calls this to check stock before creating an order.
 *
 * In tests, @MockBean replaces this bean entirely so no real gRPC call is made.
 * The InventoryGrpcServiceTest tests the server-side logic in isolation.
 */
@Component
public class InventoryClient {

    private final InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    public InventoryClient(ManagedChannel inventoryChannel) {
        this.stub = InventoryServiceGrpc.newBlockingStub(inventoryChannel);
    }

    public boolean checkStock(String productId, int quantity) {
        StockResponse response = stub.checkStock(StockRequest.newBuilder()
                .setProductId(productId)
                .setQuantity(quantity)
                .build());
        return response.getAvailable();
    }
}
