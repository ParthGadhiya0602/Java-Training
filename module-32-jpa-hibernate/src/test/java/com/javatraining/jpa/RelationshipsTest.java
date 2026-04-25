package com.javatraining.jpa;

import com.javatraining.jpa.config.JpaUtil;
import com.javatraining.jpa.entity.Author;
import com.javatraining.jpa.entity.Book;
import com.javatraining.jpa.entity.BookDetail;
import com.javatraining.jpa.entity.Tag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates all three JPA relationship types - OneToMany, OneToOne, ManyToMany -
 * plus the effects of cascade operations and orphan removal.
 *
 * <p>Domain used:
 * <pre>
 *   Author  ──(1:N)──  Book  ──(1:1)──  BookDetail
 *              │
 *              └──(N:M, join table = book_tags)──  Tag
 * </pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelationshipsTest {

    private EntityManagerFactory emf;
    private EntityManager        em;

    @BeforeAll
    void setUpEmf() {
        emf = JpaUtil.createEmf("jpa_relations");
    }

    @AfterAll
    void tearDownEmf() {
        emf.close();
    }

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        JpaUtil.inTransaction(em, e -> {
            e.createNativeQuery("DELETE FROM book_tags").executeUpdate();
            e.createNativeQuery("DELETE FROM books").executeUpdate();
            e.createNativeQuery("DELETE FROM authors").executeUpdate();
            e.createNativeQuery("DELETE FROM tags").executeUpdate();
            e.createNativeQuery("DELETE FROM book_details").executeUpdate();
        });
    }

    @AfterEach
    void tearDownEm() {
        if (em.isOpen()) em.close();
    }

    // ── OneToMany / ManyToOne ─────────────────────────────────────────────────

    @Test
    void one_to_many_books_cascade_persisted_from_author() {
        JpaUtil.inTransaction(em, e -> {
            Author author = new Author("J.K. Rowling", "jk@example.com");
            author.addBook(new Book("Harry Potter 1", new BigDecimal("15.99")));
            author.addBook(new Book("Harry Potter 2", new BigDecimal("16.99")));
            e.persist(author);   // CascadeType.ALL → both books are also persisted
        });

        em.clear();
        Author found = em.createQuery(
                "SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.email = :email",
                Author.class)
                .setParameter("email", "jk@example.com")
                .getSingleResult();
        assertEquals(2, found.getBooks().size());
    }

    @Test
    void bidirectional_back_reference_is_set_by_helper_method() {
        Long authorId = JpaUtil.inTransaction(em, e -> {
            Author author = new Author("Author A", "aa@test.com");
            Book book = new Book("My Book", new BigDecimal("10.00"));
            author.addBook(book); // sets book.author = author
            e.persist(author);
            return author.getId();
        });

        em.clear();
        // Load book's author via the owning-side FK
        Author found = em.find(Author.class, authorId);
        List<Book> books = em.createQuery(
                "SELECT b FROM Book b WHERE b.author = :a", Book.class)
                .setParameter("a", found)
                .getResultList();
        assertEquals(1, books.size());
        assertEquals(found, books.get(0).getAuthor());
    }

    @Test
    void cascade_remove_author_deletes_all_books() {
        Long authorId = JpaUtil.inTransaction(em, e -> {
            Author author = new Author("Temp Author", "temp@test.com");
            author.addBook(new Book("Book X", new BigDecimal("9.99")));
            author.addBook(new Book("Book Y", new BigDecimal("8.99")));
            e.persist(author);
            return author.getId();
        });

        JpaUtil.inTransaction(em, e -> {
            Author a = e.find(Author.class, authorId);
            e.remove(a); // CascadeType.ALL: both books are also removed
        });

        em.clear();
        assertEquals(0L,
                em.createQuery("SELECT COUNT(b) FROM Book b", Long.class)
                  .getSingleResult(),
                "All books removed via cascade when author is deleted");
    }

    @Test
    void orphan_removal_deletes_book_removed_from_collection() {
        Long authorId = JpaUtil.inTransaction(em, e -> {
            Author author = new Author("Orphan Author", "orphan@test.com");
            author.addBook(new Book("Keep Me",   new BigDecimal("10.00")));
            author.addBook(new Book("Remove Me", new BigDecimal("10.00")));
            e.persist(author);
            return author.getId();
        });

        JpaUtil.inTransaction(em, e -> {
            Author author = e.createQuery(
                    "SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id",
                    Author.class)
                    .setParameter("id", authorId)
                    .getSingleResult();
            Book toRemove = author.getBooks().stream()
                    .filter(b -> b.getTitle().equals("Remove Me"))
                    .findFirst().orElseThrow();
            author.removeBook(toRemove); // orphanRemoval=true → DELETE on flush
        });

        em.clear();
        Author a = em.createQuery(
                "SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id",
                Author.class)
                .setParameter("id", authorId)
                .getSingleResult();
        assertEquals(1, a.getBooks().size());
        assertEquals("Keep Me", a.getBooks().get(0).getTitle());
    }

    // ── OneToOne ─────────────────────────────────────────────────────────────

    @Test
    void one_to_one_book_detail_cascaded_and_loaded_eagerly() {
        Long bookId = JpaUtil.inTransaction(em, e -> {
            BookDetail detail = new BookDetail("978-0-7432-7356-5", 320, "Classic novel");
            Author author = new Author("Jane Austen", "jane@test.com");
            Book book = new Book("Pride and Prejudice", new BigDecimal("12.99"));
            book.setDetail(detail);             // OneToOne cascade = ALL
            author.addBook(book);
            e.persist(author);
            return book.getId();
        });

        em.clear();
        Book found = em.find(Book.class, bookId); // detail loaded EAGERLY
        assertNotNull(found.getDetail(), "Detail loaded eagerly with Book");
        assertEquals("978-0-7432-7356-5", found.getDetail().getIsbn());
        assertEquals(320, found.getDetail().getPageCount());
    }

    // ── ManyToMany ───────────────────────────────────────────────────────────

    @Test
    void many_to_many_tags_linked_via_join_table() {
        JpaUtil.inTransaction(em, e -> {
            Tag classic  = new Tag("classic");
            Tag dystopia = new Tag("dystopia");

            Author author = new Author("George Orwell", "orwell@test.com");
            Book book = new Book("Nineteen Eighty-Four", new BigDecimal("11.99"));
            book.addTag(classic);
            book.addTag(dystopia);
            author.addBook(book);
            e.persist(author); // cascades to book; CascadeType.PERSIST on tags
        });

        em.clear();
        List<Book> books = em.createQuery(
                "SELECT b FROM Book b LEFT JOIN FETCH b.tags WHERE b.title = :t",
                Book.class)
                .setParameter("t", "Nineteen Eighty-Four")
                .getResultList();
        assertEquals(1, books.size());
        assertEquals(2, books.get(0).getTags().size());
    }

    @Test
    void remove_tag_from_book_removes_join_table_row_only() {
        Long bookId = JpaUtil.inTransaction(em, e -> {
            Tag fiction = new Tag("fiction");
            Author author = new Author("Author B", "ab@test.com");
            Book book = new Book("Some Novel", new BigDecimal("9.99"));
            book.addTag(fiction);
            author.addBook(book);
            e.persist(author);
            return book.getId();
        });

        JpaUtil.inTransaction(em, e -> {
            Book book = e.createQuery(
                    "SELECT b FROM Book b LEFT JOIN FETCH b.tags WHERE b.id = :id",
                    Book.class)
                    .setParameter("id", bookId).getSingleResult();
            Tag t = book.getTags().iterator().next();
            book.removeTag(t); // removes join-table row; Tag entity is NOT deleted
        });

        em.clear();
        // Tag entity still exists; book has zero tags
        Book book = em.createQuery(
                "SELECT b FROM Book b LEFT JOIN FETCH b.tags WHERE b.id = :id",
                Book.class)
                .setParameter("id", bookId).getSingleResult();
        assertEquals(0, book.getTags().size(), "Tag removed from book");
        assertEquals(1L,
                em.createQuery("SELECT COUNT(t) FROM Tag t", Long.class).getSingleResult(),
                "Tag entity itself is not deleted - only the join-table row");
    }
}
