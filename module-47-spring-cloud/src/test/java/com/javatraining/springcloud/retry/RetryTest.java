package com.javatraining.springcloud.retry;

import com.javatraining.springcloud.inventory.InventoryClient;
import com.javatraining.springcloud.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
class RetryTest {

    @Autowired InventoryService inventoryService;
    @MockBean InventoryClient inventoryClient;

    @Test
    void retries_up_to_max_attempts_then_invokes_fallback() {
        when(inventoryClient.reserve(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("unavailable"));

        boolean result = inventoryService.reserve(1L, 1);

        assertThat(result).isFalse();
        verify(inventoryClient, times(3)).reserve(anyLong(), anyInt());
    }

    @Test
    void succeeds_on_second_attempt_without_exhausting_retries() {
        when(inventoryClient.reserve(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("transient error"))
                .thenReturn(true);

        boolean result = inventoryService.reserve(1L, 1);

        assertThat(result).isTrue();
        verify(inventoryClient, times(2)).reserve(anyLong(), anyInt());
    }

    @Test
    void no_retry_when_first_attempt_succeeds() {
        when(inventoryClient.reserve(anyLong(), anyInt())).thenReturn(true);

        boolean result = inventoryService.reserve(1L, 1);

        assertThat(result).isTrue();
        verify(inventoryClient, times(1)).reserve(anyLong(), anyInt());
    }
}
