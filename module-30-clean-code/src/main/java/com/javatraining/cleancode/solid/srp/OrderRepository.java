package com.javatraining.cleancode.solid.srp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SRP - single responsibility: store and retrieve orders.
 * Only changes when the storage mechanism changes.
 */
public class OrderRepository {

    private final List<Order> store = new ArrayList<>();

    public void save(Order order) {
        store.add(order);
    }

    public Optional<Order> findById(String id) {
        return store.stream().filter(o -> o.id().equals(id)).findFirst();
    }

    public List<Order> findAll() {
        return List.copyOf(store);
    }
}
