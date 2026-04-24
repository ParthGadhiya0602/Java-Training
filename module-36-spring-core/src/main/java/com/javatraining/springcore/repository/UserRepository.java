package com.javatraining.springcore.repository;

import java.util.Optional;

public interface UserRepository {
    Optional<String> findById(long id);
    void save(long id, String name);
}
