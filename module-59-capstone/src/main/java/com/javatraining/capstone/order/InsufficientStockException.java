package com.javatraining.capstone.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId) {
        super("Insufficient stock for product: " + productId);
    }
}
