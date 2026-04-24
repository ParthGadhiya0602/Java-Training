package com.javatraining.microservices.saga;

import com.javatraining.microservices.inventory.InventoryClient;
import com.javatraining.microservices.order.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreationSagaTest {

    @Mock OrderService orderService;
    @Mock InventoryClient inventoryClient;
    @InjectMocks OrderCreationSaga saga;

    private static final OrderRequest REQUEST = new OrderRequest(1L, 2);

    private static final Order PENDING = Order.builder()
            .id(1L).productId(1L).quantity(2).status(OrderStatus.PENDING).build();

    @Test
    void confirms_order_when_inventory_is_available() {
        Order confirmed = Order.builder().id(1L).status(OrderStatus.CONFIRMED).build();
        when(orderService.createPendingOrder(REQUEST)).thenReturn(PENDING);
        when(inventoryClient.reserve(1L, 2)).thenReturn(true);
        when(orderService.confirmOrder(1L)).thenReturn(confirmed);

        Order result = saga.execute(REQUEST);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderService).confirmOrder(1L);
        verify(orderService, never()).cancelOrder(any());
    }

    @Test
    void cancels_order_when_inventory_is_unavailable() {
        Order cancelled = Order.builder().id(1L).status(OrderStatus.CANCELLED).build();
        when(orderService.createPendingOrder(REQUEST)).thenReturn(PENDING);
        when(inventoryClient.reserve(1L, 2)).thenReturn(false);
        when(orderService.cancelOrder(1L)).thenReturn(cancelled);

        Order result = saga.execute(REQUEST);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderService).cancelOrder(1L);
        verify(orderService, never()).confirmOrder(any());
    }

    @Test
    void always_creates_pending_order_before_calling_inventory() {
        Order confirmed = Order.builder().id(1L).status(OrderStatus.CONFIRMED).build();
        when(orderService.createPendingOrder(REQUEST)).thenReturn(PENDING);
        when(inventoryClient.reserve(1L, 2)).thenReturn(true);
        when(orderService.confirmOrder(1L)).thenReturn(confirmed);

        saga.execute(REQUEST);

        var inOrder = inOrder(orderService, inventoryClient);
        inOrder.verify(orderService).createPendingOrder(REQUEST);
        inOrder.verify(inventoryClient).reserve(1L, 2);
        inOrder.verify(orderService).confirmOrder(1L);
    }
}
