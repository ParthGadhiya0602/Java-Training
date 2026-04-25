package com.javatraining.graphql.author;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class AuthorRepository {

    private static final Map<Long, Author> AUTHORS = Map.of(
            1L, new Author(1L, "Joshua Bloch"),
            2L, new Author(2L, "Robert C. Martin")
    );

    public List<Author> findAll() {
        return List.copyOf(AUTHORS.values());
    }

    public Optional<Author> findById(Long id) {
        return Optional.ofNullable(AUTHORS.get(id));
    }

    public List<Author> findAllByIds(List<Long> ids) {
        return ids.stream()
                .map(AUTHORS::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
