---
title: "Module 51 — GraphQL with Spring"
nav_order: 51
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-51-graphql/src){: .btn .btn-outline }

# Module 51 — GraphQL with Spring

## What this module covers

Schema-first GraphQL with Spring for GraphQL: queries, mutations, and subscriptions
via `@QueryMapping`/`@MutationMapping`/`@SubscriptionMapping`, solving the N+1
problem with `@BatchMapping`, and testing all three operation types with
`@GraphQlTest` and `GraphQlTester`.

---

## Project structure

```
src/main/java/com/javatraining/graphql/
├── GraphQlApplication.java
├── author/
│   ├── Author.java                  # record: id, name
│   └── AuthorRepository.java        # in-memory store + findAllByIds()
└── book/
    ├── Book.java                    # record: id, title, genre, authorId
    ├── BookRepository.java          # in-memory store
    ├── BookEventPublisher.java      # Sinks.Many<Book> wrapper for subscriptions
    └── BookController.java          # all GraphQL resolvers

src/main/resources/graphql/
└── schema.graphqls                  # schema-first definition
```

---

## Schema

The `.graphqls` file is the single source of truth for the API shape.
Spring for GraphQL loads all files matching `classpath:graphql/**/*.graphqls`.

```graphql
type Query {
    books: [Book!]!
    book(id: ID!): Book
    authors: [Author!]!
}

type Mutation {
    addBook(title: String!, genre: String!, authorId: ID!): Book!
    deleteBook(id: ID!): Boolean!
}

type Subscription {
    bookAdded: Book!
}

type Book {
    id: ID!
    title: String!
    genre: String!
    author: Author!    # resolved by @BatchMapping to prevent N+1
}

type Author {
    id: ID!
    name: String!
}
```

---

## Resolver annotations

| Annotation            | Maps to                        |
|-----------------------|-------------------------------|
| `@QueryMapping`       | `type Query { ... }`          |
| `@MutationMapping`    | `type Mutation { ... }`       |
| `@SubscriptionMapping`| `type Subscription { ... }`   |
| `@BatchMapping`       | sub-field of a parent type    |
| `@Argument`           | inline argument conversion    |

Method names are matched to field names by default; override with `value`.

---

## Queries and mutations

```java
@Controller
@RequiredArgsConstructor
public class BookController {

    @QueryMapping
    public List<Book> books() {
        return bookRepository.findAll();
    }

    @QueryMapping
    public Book book(@Argument Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    @MutationMapping
    public Book addBook(@Argument String title, @Argument String genre,
                        @Argument Long authorId) {
        Book book = bookRepository.save(new Book(null, title, genre, authorId));
        bookEventPublisher.publish(book);
        return book;
    }

    @MutationMapping
    public boolean deleteBook(@Argument Long id) {
        return bookRepository.deleteById(id);
    }
}
```

`@Argument` applies Spring's `ConversionService` — the GraphQL `ID` scalar
arrives as a `String`; `Long id` triggers automatic `String → Long` conversion.

---

## Subscriptions

`@SubscriptionMapping` returns a reactive `Flux`. Spring for GraphQL streams each
emitted element to the client as a separate GraphQL response.

`BookEventPublisher` wraps a `Sinks.Many` to decouple the emission point (mutation)
from the subscription source, which also makes each component independently mockable.

```java
@Component
public class BookEventPublisher {
    private final Sinks.Many<Book> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(Book book)    { sink.tryEmitNext(book); }
    public Flux<Book> getStream()     { return sink.asFlux(); }
}

@SubscriptionMapping
public Flux<Book> bookAdded() {
    return bookEventPublisher.getStream();
}
```

---

## N+1 problem and `@BatchMapping`

### The problem

Fetching `books { author { name } }` with a naive `@SchemaMapping` triggers one
`findById(authorId)` per book — N books = N+1 database calls.

### The fix

`@BatchMapping` receives ALL parent objects from a single GraphQL execution at once
and returns a map from parent → child. Spring for GraphQL invokes it exactly once,
regardless of how many books were fetched.

```java
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
```

The method name `author` matches the `Book.author` field in the schema.
`typeName = "Book"` targets the correct parent type.

---

## Testing with `@GraphQlTest`

`@GraphQlTest` loads only the GraphQL controller layer — no Tomcat, no HTTP —
backed by `ExecutionGraphQlService`. Queries, mutations, and subscriptions all
run through the same tester.

```java
@GraphQlTest(BookController.class)
class BookControllerTest {

    @Autowired GraphQlTester graphQlTester;
    @MockBean  BookRepository bookRepository;
    @MockBean  AuthorRepository authorRepository;
    @MockBean  BookEventPublisher bookEventPublisher;
```

### Query

```java
graphQlTester.document("{ book(id: \"1\") { title author { name } } }")
    .execute()
    .path("book.title").entity(String.class).isEqualTo("Effective Java")
    .path("book.author.name").entity(String.class).isEqualTo("Joshua Bloch");
```

### Mutation with side-effect verification

```java
graphQlTester.document("""
    mutation { addBook(title: "New Book", genre: "Fiction", authorId: "1") { id title } }
    """)
    .execute()
    .path("addBook.title").entity(String.class).isEqualTo("New Book");

verify(bookEventPublisher).publish(saved);
```

### Subscription with `StepVerifier`

`executeSubscription().toFlux()` returns the subscription `Flux` directly. Mock
`bookEventPublisher.getStream()` with a finite `Flux` to control what is emitted.

```java
when(bookEventPublisher.getStream()).thenReturn(Flux.just(newBook));

graphQlTester.document("subscription { bookAdded { id title } }")
    .executeSubscription()
    .toFlux("bookAdded", Map.class)
    .as(StepVerifier::create)
    .assertNext(book -> assertThat(book.get("title")).isEqualTo("GraphQL in Action"))
    .verifyComplete();
```

### Batch mapping verification

```java
when(bookRepository.findAll()).thenReturn(List.of(book1, book2));
when(authorRepository.findAllByIds(any())).thenReturn(List.of(author1, author2));

graphQlTester.document("{ books { title author { name } } }")
    .execute()
    .path("books").entityList(Map.class).hasSize(2);

verify(authorRepository, times(1)).findAllByIds(any());
```

`times(1)` proves the batch mapping called `findAllByIds` once for 2 books,
not twice (which would indicate N+1 regression).

---

## Tests

| Class               | Type           | Count |
|---------------------|----------------|-------|
| `BookControllerTest`| `@GraphQlTest` | 7     |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **7/7 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `@BatchMapping` over `@SchemaMapping` with DataLoader | `@BatchMapping` is Spring for GraphQL's idiomatic N+1 fix — no manual DataLoader registration needed |
| `BookEventPublisher` as a separate component | Decouples sink from controller, making mutation and subscription independently mockable in `@GraphQlTest` |
| `Sinks.many().multicast().onBackpressureBuffer()` | Hot source that buffers for each slow subscriber; replaces the old `EmitterProcessor` (deprecated in Reactor 3.5) |
| Subscription mock returns `Flux.just(...)` | Finite flux that completes immediately keeps `StepVerifier` tests synchronous and deterministic |
| Schema-first over annotation-first | Schema file is language-agnostic documentation; implementation auto-validated against it at startup |
