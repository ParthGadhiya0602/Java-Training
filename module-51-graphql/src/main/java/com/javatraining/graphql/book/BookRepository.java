package com.javatraining.graphql.book;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class BookRepository {

    private final Map<Long, Book> books = new ConcurrentHashMap<>(Map.of(
            1L, new Book(1L, "Effective Java",             "Programming", 1L),
            2L, new Book(2L, "Clean Code",                 "Programming", 2L),
            3L, new Book(3L, "The Pragmatic Programmer",   "Programming", 2L)
    ));
    private final AtomicLong sequence = new AtomicLong(4);

    public List<Book> findAll() {
        return List.copyOf(books.values());
    }

    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(books.get(id));
    }

    public Book save(Book book) {
        Book persisted = book.id() == null
                ? new Book(sequence.getAndIncrement(), book.title(), book.genre(), book.authorId())
                : book;
        books.put(persisted.id(), persisted);
        return persisted;
    }

    public boolean deleteById(Long id) {
        return books.remove(id) != null;
    }
}
