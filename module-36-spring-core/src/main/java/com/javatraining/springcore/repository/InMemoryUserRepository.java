package com.javatraining.springcore.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, String> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(long id, String name) {
        store.put(id, name);
    }
}
