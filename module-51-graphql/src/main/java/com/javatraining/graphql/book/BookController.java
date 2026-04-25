package com.javatraining.graphql.book;

import com.javatraining.graphql.author.Author;
import com.javatraining.graphql.author.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class BookController {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookEventPublisher bookEventPublisher;

    @QueryMapping
    public List<Book> books() {
        return bookRepository.findAll();
    }

    @QueryMapping
    public Book book(@Argument Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    @QueryMapping
    public List<Author> authors() {
        return authorRepository.findAll();
    }

    @MutationMapping
    public Book addBook(@Argument String title, @Argument String genre, @Argument Long authorId) {
        Book book = bookRepository.save(new Book(null, title, genre, authorId));
        bookEventPublisher.publish(book);
        return book;
    }

    @MutationMapping
    public boolean deleteBook(@Argument Long id) {
        return bookRepository.deleteById(id);
    }

    @SubscriptionMapping
    public Flux<Book> bookAdded() {
        return bookEventPublisher.getStream();
    }

    /**
     * @BatchMapping resolves Book.author for all books in a single call instead of
     * one call per book, preventing the N+1 problem.
     *
     * Without batching: fetching N books triggers N separate author lookups.
     * With @BatchMapping: Spring for GraphQL collects all parent books from a single
     * execution, then invokes this method once with the full list.
     */
    @BatchMapping(typeName = "Book")
    public Map<Book, Author> author(List<Book> books) {
        List<Long> authorIds = books.stream()
                .map(Book::authorId)
                .distinct()
                .toList();
        Map<Long, Author> authorMap = authorRepository.findAllByIds(authorIds)
                .stream()
                .collect(Collectors.toMap(Author::id, Function.identity()));
        return books.stream()
                .collect(Collectors.toMap(Function.identity(), b -> authorMap.get(b.authorId())));
    }
}
