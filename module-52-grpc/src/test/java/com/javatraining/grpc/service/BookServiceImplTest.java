package com.javatraining.grpc.service;

import com.javatraining.grpc.interceptor.LoggingInterceptor;
import com.javatraining.grpc.proto.BookResponse;
import com.javatraining.grpc.proto.BookServiceGrpc;
import com.javatraining.grpc.proto.GetBookRequest;
import com.javatraining.grpc.proto.ListBooksRequest;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure JUnit 5 — no Spring context needed.
 * InProcessServerBuilder wires the service and interceptor directly in-memory,
 * giving fast, hermetic tests with no port binding.
 */
class BookServiceImplTest {

    private static final String SERVER_NAME = "test-book-service";

    private Server server;
    private ManagedChannel channel;
    private BookServiceGrpc.BookServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws IOException {
        server = InProcessServerBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .addService(new BookServiceImpl())
                .intercept(new LoggingInterceptor())
                .build()
                .start();

        channel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();

        stub = BookServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void getBook_returns_book_for_known_id() {
        GetBookRequest request = GetBookRequest.newBuilder().setId(1L).build();

        BookResponse response = stub.getBook(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Effective Java");
        assertThat(response.getAuthor()).isEqualTo("Joshua Bloch");
    }

    @Test
    void getBook_throws_NOT_FOUND_for_unknown_id() {
        GetBookRequest request = GetBookRequest.newBuilder().setId(999L).build();

        assertThatThrownBy(() -> stub.getBook(request))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("NOT_FOUND");
    }

    @Test
    void listBooks_streams_all_books_for_matching_genre() {
        ListBooksRequest request = ListBooksRequest.newBuilder().setGenre("Programming").build();

        List<BookResponse> responses = new ArrayList<>();
        stub.listBooks(request).forEachRemaining(responses::add);

        assertThat(responses).hasSize(3);
        assertThat(responses).allMatch(b -> b.getGenre().equals("Programming"));
    }

    @Test
    void listBooks_returns_empty_stream_for_unknown_genre() {
        ListBooksRequest request = ListBooksRequest.newBuilder().setGenre("Fantasy").build();

        List<BookResponse> responses = new ArrayList<>();
        stub.listBooks(request).forEachRemaining(responses::add);

        assertThat(responses).isEmpty();
    }
}
