package com.javatraining.grpc.service;

import com.javatraining.grpc.proto.BookResponse;
import com.javatraining.grpc.proto.BookServiceGrpc;
import com.javatraining.grpc.proto.GetBookRequest;
import com.javatraining.grpc.proto.ListBooksRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Map;

@GrpcService
public class BookServiceImpl extends BookServiceGrpc.BookServiceImplBase {

    private static final Map<Long, BookResponse> BOOKS = Map.of(
            1L, book(1L, "Effective Java",              "Programming",   "Joshua Bloch"),
            2L, book(2L, "Clean Code",                  "Programming",   "Robert C. Martin"),
            3L, book(3L, "Designing Data-Intensive Apps","Programming",  "Martin Kleppmann"),
            4L, book(4L, "Clean Architecture",          "Architecture",  "Robert C. Martin")
    );

    @Override
    public void getBook(GetBookRequest request, StreamObserver<BookResponse> responseObserver) {
        BookResponse book = BOOKS.get(request.getId());
        if (book == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Book not found: " + request.getId())
                            .asRuntimeException()
            );
            return;
        }
        responseObserver.onNext(book);
        responseObserver.onCompleted();
    }

    @Override
    public void listBooks(ListBooksRequest request, StreamObserver<BookResponse> responseObserver) {
        String genre = request.getGenre();
        List<BookResponse> matches = BOOKS.values().stream()
                .filter(b -> genre.isBlank() || b.getGenre().equalsIgnoreCase(genre))
                .toList();

        for (BookResponse book : matches) {
            responseObserver.onNext(book);
        }
        responseObserver.onCompleted();
    }

    private static BookResponse book(long id, String title, String genre, String author) {
        return BookResponse.newBuilder()
                .setId(id).setTitle(title).setGenre(genre).setAuthor(author)
                .build();
    }
}
