package com.javatraining.graphql.book;

import com.javatraining.graphql.author.Author;
import com.javatraining.graphql.author.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @GraphQlTest loads the GraphQL controller layer and a GraphQlTester backed by
 * ExecutionGraphQlService - no HTTP server, no Tomcat - making tests fast.
 *
 * @MockBean replaces the real repositories and event publisher so each test
 * controls exactly what data flows through the resolvers.
 */
@GraphQlTest(BookController.class)
class BookControllerTest {

    @Autowired GraphQlTester graphQlTester;
    @MockBean  BookRepository bookRepository;
    @MockBean  AuthorRepository authorRepository;
    @MockBean  BookEventPublisher bookEventPublisher;

    final Author author1 = new Author(1L, "Joshua Bloch");
    final Author author2 = new Author(2L, "Robert C. Martin");
    final Book   book1   = new Book(1L, "Effective Java",           "Programming", 1L);
    final Book   book2   = new Book(2L, "Clean Code",               "Programming", 2L);

    @Test
    void books_query_returns_all_books() {
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2));

        graphQlTester.document("{ books { id title genre } }")
                .execute()
                .path("books")
                .entityList(Map.class)
                .hasSize(2);
    }

    @Test
    void book_query_returns_book_with_author() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book1));
        when(authorRepository.findAllByIds(any())).thenReturn(List.of(author1));

        graphQlTester.document("{ book(id: \"1\") { title author { name } } }")
                .execute()
                .path("book.title").entity(String.class).isEqualTo("Effective Java")
                .path("book.author.name").entity(String.class).isEqualTo("Joshua Bloch");
    }

    @Test
    void book_query_returns_null_for_unknown_id() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        graphQlTester.document("{ book(id: \"99\") { title } }")
                .execute()
                .path("book")
                .valueIsNull();
    }

    @Test
    void addBook_mutation_saves_book_and_notifies_publisher() {
        Book saved = new Book(4L, "New Book", "Fiction", 1L);
        when(bookRepository.save(any())).thenReturn(saved);

        graphQlTester.document("""
                mutation { addBook(title: "New Book", genre: "Fiction", authorId: "1") { id title genre } }
                """)
                .execute()
                .path("addBook.title").entity(String.class).isEqualTo("New Book")
                .path("addBook.genre").entity(String.class).isEqualTo("Fiction");

        verify(bookEventPublisher).publish(saved);
    }

    @Test
    void deleteBook_mutation_returns_true_when_found() {
        when(bookRepository.deleteById(1L)).thenReturn(true);

        graphQlTester.document("mutation { deleteBook(id: \"1\") }")
                .execute()
                .path("deleteBook").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void bookAdded_subscription_emits_books_from_publisher() {
        Book newBook = new Book(5L, "GraphQL in Action", "Technical", 1L);
        when(bookEventPublisher.getStream()).thenReturn(Flux.just(newBook));
        when(authorRepository.findAllByIds(any())).thenReturn(List.of(author1));

        graphQlTester.document("subscription { bookAdded { id title } }")
                .executeSubscription()
                .toFlux("bookAdded", Map.class)
                .as(StepVerifier::create)
                .assertNext(book -> assertThat(book.get("title")).isEqualTo("GraphQL in Action"))
                .verifyComplete();
    }

    /**
     * Proves that @BatchMapping calls authorRepository.findAllByIds exactly once
     * for a books query that returns multiple books - not once per book (N+1).
     */
    @Test
    void batch_mapping_loads_all_authors_in_one_call() {
        when(bookRepository.findAll()).thenReturn(List.of(book1, book2));
        when(authorRepository.findAllByIds(any())).thenReturn(List.of(author1, author2));

        graphQlTester.document("{ books { title author { name } } }")
                .execute()
                .path("books")
                .entityList(Map.class)
                .hasSize(2);

        verify(authorRepository, times(1)).findAllByIds(any());
    }
}
