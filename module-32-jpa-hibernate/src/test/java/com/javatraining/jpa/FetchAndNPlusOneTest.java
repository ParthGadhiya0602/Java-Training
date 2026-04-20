package com.javatraining.jpa;

import com.javatraining.jpa.config.JpaUtil;
import com.javatraining.jpa.entity.Author;
import com.javatraining.jpa.entity.Book;
import com.javatraining.jpa.entity.BookDetail;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates fetch types and the N+1 query problem — the most common JPA
 * performance trap — using Hibernate's {@link Statistics} to count SQL statements.
 *
 * <p>Setup: 3 authors, each with 2 books.  Statistics are cleared before each
 * measurement so counts reflect only the code under test.
 *
 * <p><strong>N+1 explained:</strong>
 * Querying N authors with a LAZY book collection results in 1 query for the
 * authors plus N additional queries (one per author) to load each book list.
 * {@code LEFT JOIN FETCH} collapses these into a single query.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchAndNPlusOneTest {

    private EntityManagerFactory emf;
    private Statistics            stats;
    private EntityManager         em;

    @BeforeAll
    void setUpEmf() {
        emf   = JpaUtil.createEmf("jpa_fetch");
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
    }

    @AfterAll
    void tearDownEmf() {
        emf.close();
    }

    @BeforeEach
    void seedData() {
        em = emf.createEntityManager();
        // Clear
        JpaUtil.inTransaction(em, e -> {
            e.createNativeQuery("DELETE FROM book_tags").executeUpdate();
            e.createNativeQuery("DELETE FROM books").executeUpdate();
            e.createNativeQuery("DELETE FROM authors").executeUpdate();
            e.createNativeQuery("DELETE FROM tags").executeUpdate();
            e.createNativeQuery("DELETE FROM book_details").executeUpdate();
        });
        // Seed 3 authors × 2 books each
        JpaUtil.inTransaction(em, e -> {
            for (int i = 1; i <= 3; i++) {
                Author a = new Author("Author-" + i, "author" + i + "@test.com");
                a.addBook(new Book("BookA-" + i, new BigDecimal("10.00")));
                a.addBook(new Book("BookB-" + i, new BigDecimal("12.00")));
                e.persist(a);
            }
        });
        em.clear();
        stats.clear();
    }

    @AfterEach
    void tearDownEm() {
        if (em.isOpen()) em.close();
    }

    // ── Lazy loading ─────────────────────────────────────────────────────────

    @Test
    void lazy_association_triggers_extra_select_on_first_access() {
        // Load a book — author is LAZY, so only the book row is fetched initially
        Book book = JpaUtil.inTransaction(em, e -> {
            return e.createQuery("SELECT b FROM Book b", Book.class)
                    .setMaxResults(1).getSingleResult();
        });
        em.clear();
        stats.clear();

        // Inside a new transaction, access lazy author
        JpaUtil.inTransaction(em, e -> {
            Book b = e.find(Book.class, book.getId());
            long afterFind = stats.getPrepareStatementCount(); // 1 SELECT for book

            String authorName = b.getAuthor().getName();       // lazy-load: 1 more SELECT
            long afterAccess  = stats.getPrepareStatementCount();

            assertEquals(afterFind + 1L, afterAccess,
                    "Accessing LAZY @ManyToOne triggers exactly one additional SELECT");
            assertNotNull(authorName);
        });
    }

    @Test
    void eager_one_to_one_loaded_with_book_no_extra_query() {
        // Add a detail to one book
        Long bookId = JpaUtil.inTransaction(em, e -> {
            Book b = e.createQuery("SELECT b FROM Book b", Book.class)
                      .setMaxResults(1).getSingleResult();
            b.setDetail(new BookDetail("978-1-111-1111-1", 200, "Synopsis here"));
            return b.getId();
        });
        em.clear();
        stats.clear();

        Book book = em.find(Book.class, bookId); // EAGER detail loaded here
        long queryCountAfterFind = stats.getPrepareStatementCount();

        // Accessing detail does NOT add more queries
        BookDetail detail = book.getDetail();
        assertEquals(queryCountAfterFind, stats.getPrepareStatementCount(),
                "EAGER @OneToOne: detail already loaded, no additional SELECT");
        assertNotNull(detail);
        assertEquals("978-1-111-1111-1", detail.getIsbn());
    }

    // ── N+1 problem and fix ───────────────────────────────────────────────────

    @Test
    void n_plus_one_occurs_when_lazy_collection_accessed_per_entity() {
        JpaUtil.inTransaction(em, e -> {
            // 1 query: SELECT all authors
            List<Author> authors = e.createQuery("SELECT a FROM Author a", Author.class)
                                    .getResultList();
            // 3 queries: one per author to lazy-load each book collection
            long total = authors.stream()
                                .mapToLong(a -> a.getBooks().size())
                                .sum();
            assertEquals(6L, total, "3 authors × 2 books = 6");
        });

        // N+1: 1 (authors) + 3 (one per author for lazy books) = 4
        assertEquals(4L, stats.getPrepareStatementCount(),
                "N+1: 1 query for authors + 3 queries for lazy book collections = 4 total");
    }

    @Test
    void join_fetch_collapses_n_plus_one_into_a_single_query() {
        JpaUtil.inTransaction(em, e -> {
            // DISTINCT avoids duplicate Author rows caused by the JOIN
            List<Author> authors = e.createQuery(
                    "SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books",
                    Author.class)
                    .getResultList();
            long total = authors.stream()
                                .mapToLong(a -> a.getBooks().size())
                                .sum();
            assertEquals(6L, total);
        });

        // JOIN FETCH: everything loaded in 1 query
        assertEquals(1L, stats.getPrepareStatementCount(),
                "JOIN FETCH: exactly 1 query for all authors and all their books");
    }
}
