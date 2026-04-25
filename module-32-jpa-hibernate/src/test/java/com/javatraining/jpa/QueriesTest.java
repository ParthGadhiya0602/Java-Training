package com.javatraining.jpa;

import com.javatraining.jpa.config.JpaUtil;
import com.javatraining.jpa.entity.Author;
import com.javatraining.jpa.entity.Book;
import org.junit.jupiter.api.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the three query mechanisms available in JPA:
 *
 * <pre>
 *   JPQL (Java Persistence Query Language)
 *     - object-oriented; refers to entity class names and field names, not table/column
 *     - "SELECT a FROM Author a WHERE a.name LIKE :pattern"
 *
 *   Criteria API
 *     - programmatic, type-safe; useful when query structure is dynamic
 *     - CriteriaBuilder + CriteriaQuery + Root
 *
 *   Native SQL
 *     - escape hatch for DB-specific features; bypasses object mapping
 *     - used in @BeforeEach for bulk DELETE (cascades are not triggered)
 * </pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueriesTest {

    private EntityManagerFactory emf;
    private EntityManager        em;

    @BeforeAll
    void setUpEmf() {
        emf = JpaUtil.createEmf("jpa_queries");
    }

    @AfterAll
    void tearDownEmf() {
        emf.close();
    }

    @BeforeEach
    void seedData() {
        em = emf.createEntityManager();
        JpaUtil.inTransaction(em, e -> {
            e.createNativeQuery("DELETE FROM book_tags").executeUpdate();
            e.createNativeQuery("DELETE FROM books").executeUpdate();
            e.createNativeQuery("DELETE FROM authors").executeUpdate();
            e.createNativeQuery("DELETE FROM tags").executeUpdate();
            e.createNativeQuery("DELETE FROM book_details").executeUpdate();
        });

        // Seed: 2 authors, 2 books each
        JpaUtil.inTransaction(em, e -> {
            Author dickens = new Author("Charles Dickens", "dickens@classic.com");
            dickens.addBook(new Book("Oliver Twist",       new BigDecimal("12.99")));
            dickens.addBook(new Book("Great Expectations", new BigDecimal("14.99")));
            e.persist(dickens);

            Author austen = new Author("Jane Austen", "austen@classic.com");
            austen.addBook(new Book("Pride and Prejudice", new BigDecimal("13.99")));
            austen.addBook(new Book("Emma",                new BigDecimal("11.99")));
            e.persist(austen);
        });
        em.clear();
    }

    @AfterEach
    void tearDownEm() {
        if (em.isOpen()) em.close();
    }

    // ── JPQL - basic selects ─────────────────────────────────────────────────

    @Test
    void jpql_select_all_returns_all_authors() {
        List<Author> authors = em.createQuery("SELECT a FROM Author a ORDER BY a.name", Author.class)
                                  .getResultList();
        assertEquals(2, authors.size());
        assertEquals("Charles Dickens", authors.get(0).getName());
        assertEquals("Jane Austen",     authors.get(1).getName());
    }

    @Test
    void jpql_like_finds_books_by_partial_title() {
        List<Book> books = em.createQuery(
                "SELECT b FROM Book b WHERE b.title LIKE :pattern ORDER BY b.title",
                Book.class)
                .setParameter("pattern", "%Expectations%")
                .getResultList();
        assertEquals(1, books.size());
        assertEquals("Great Expectations", books.get(0).getTitle());
    }

    @Test
    void jpql_named_parameter_finds_author_by_exact_email() {
        Author found = em.createQuery(
                "SELECT a FROM Author a WHERE a.email = :email", Author.class)
                .setParameter("email", "austen@classic.com")
                .getSingleResult();
        assertEquals("Jane Austen", found.getName());
    }

    @Test
    void jpql_join_with_where_filters_by_association() {
        // Find all books whose author is named "Charles Dickens"
        List<Book> books = em.createQuery(
                "SELECT b FROM Book b JOIN b.author a WHERE a.name = :name ORDER BY b.title",
                Book.class)
                .setParameter("name", "Charles Dickens")
                .getResultList();
        assertEquals(2, books.size());
        assertEquals("Great Expectations", books.get(0).getTitle());
        assertEquals("Oliver Twist",       books.get(1).getTitle());
    }

    // ── JPQL - aggregations ──────────────────────────────────────────────────

    @Test
    void jpql_count_all_books() {
        Long count = em.createQuery("SELECT COUNT(b) FROM Book b", Long.class)
                       .getSingleResult();
        assertEquals(4L, count);
    }

    @Test
    void jpql_group_by_author_returns_book_count_per_author() {
        // Returns Object[] {authorName, bookCount}
        List<Object[]> rows = em.createQuery(
                "SELECT a.name, COUNT(b) FROM Author a LEFT JOIN a.books b " +
                "GROUP BY a.name ORDER BY a.name",
                Object[].class)
                .getResultList();

        assertEquals(2, rows.size());
        assertEquals("Charles Dickens", rows.get(0)[0]);
        assertEquals(2L,                rows.get(0)[1]);
        assertEquals("Jane Austen",     rows.get(1)[0]);
        assertEquals(2L,                rows.get(1)[1]);
    }

    // ── Criteria API ─────────────────────────────────────────────────────────

    @Test
    void criteria_finds_author_by_exact_name() {
        CriteriaBuilder  cb   = em.getCriteriaBuilder();
        CriteriaQuery<Author> cq = cb.createQuery(Author.class);
        Root<Author>     root = cq.from(Author.class);

        cq.select(root)
          .where(cb.equal(root.get("name"), "Charles Dickens"));

        List<Author> result = em.createQuery(cq).getResultList();
        assertEquals(1, result.size());
        assertEquals("Charles Dickens", result.get(0).getName());
    }

    @Test
    void criteria_finds_books_with_price_above_threshold() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> cq = cb.createQuery(Book.class);
        Root<Book>       root  = cq.from(Book.class);

        cq.select(root)
          .where(cb.greaterThan(root.get("price"), new BigDecimal("13.00")))
          .orderBy(cb.asc(root.get("title")));

        List<Book> expensive = em.createQuery(cq).getResultList();
        // 14.99 (Great Expectations) and 13.99 (Pride and Prejudice) are > 13.00
        assertEquals(2, expensive.size());
        assertTrue(expensive.stream().allMatch(
                b -> b.getPrice().compareTo(new BigDecimal("13.00")) > 0));
    }
}
