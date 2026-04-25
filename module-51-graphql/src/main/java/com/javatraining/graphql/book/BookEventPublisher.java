package com.javatraining.graphql.book;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Decouples the subscription source from the mutation logic so both can be
 * independently mocked in @GraphQlTest.
 */
@Component
public class BookEventPublisher {

    private final Sinks.Many<Book> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(Book book) {
        sink.tryEmitNext(book);
    }

    public Flux<Book> getStream() {
        return sink.asFlux();
    }
}
