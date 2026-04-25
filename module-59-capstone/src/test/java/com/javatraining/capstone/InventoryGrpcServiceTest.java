package com.javatraining.capstone;

import com.javatraining.capstone.inventory.InventoryServiceImpl;
import com.javatraining.capstone.proto.InventoryProto.StockRequest;
import com.javatraining.capstone.proto.InventoryProto.StockResponse;
import com.javatraining.capstone.proto.InventoryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the gRPC InventoryService in isolation using an in-process server —
 * no network port, no Spring context. Same pattern as module 52.
 */
class InventoryGrpcServiceTest {

    private static final String SERVER_NAME = "inventory-test";

    private Server grpcServer;
    private ManagedChannel channel;
    private InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws IOException {
        grpcServer = InProcessServerBuilder.forName(SERVER_NAME)
                .directExecutor()
                .addService(new InventoryServiceImpl())
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(SERVER_NAME)
                .directExecutor()
                .build();

        stub = InventoryServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        grpcServer.shutdownNow();
    }

    @Test
    void checkStock_returns_available_when_quantity_is_within_stock() {
        StockResponse response = stub.checkStock(StockRequest.newBuilder()
                .setProductId("PROD-1")
                .setQuantity(10)
                .build());

        assertThat(response.getAvailable()).isTrue();
        assertThat(response.getCurrentStock()).isEqualTo(100);
    }

    @Test
    void checkStock_returns_unavailable_when_quantity_exceeds_stock() {
        StockResponse response = stub.checkStock(StockRequest.newBuilder()
                .setProductId("PROD-1")
                .setQuantity(9999)
                .build());

        assertThat(response.getAvailable()).isFalse();
        assertThat(response.getCurrentStock()).isEqualTo(100);
    }
}
