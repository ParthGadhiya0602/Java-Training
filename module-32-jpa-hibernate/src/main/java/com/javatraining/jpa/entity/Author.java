package com.javatraining.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates:
 * <ul>
 *   <li>{@code @Entity} / {@code @Table} - class is mapped to a DB table</li>
 *   <li>{@code @Id} + {@code @GeneratedValue(IDENTITY)} - surrogate primary key</li>
 *   <li>{@code @OneToMany(mappedBy, cascade=ALL, orphanRemoval=true)} - parent side
 *       of a bidirectional relationship; cascades persist/remove to books</li>
 *   <li>{@code @NotBlank} / {@code @Email} - Bean Validation 3.0 constraints</li>
 * </ul>
 *
 * <p><strong>Bidirectional relationship rule:</strong> always keep both sides in sync.
 * {@link #addBook} and {@link #removeBook} are helper methods that maintain consistency
 * so Hibernate always sees a coherent object graph.
 */
@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Author name must not be blank")
    private String name;

    @Column(nullable = false, unique = true)
    @Email(message = "Author email must be a valid e-mail address")
    private String email;

    /**
     * Inverse side of Author ↔ Book.
     * <ul>
     *   <li>{@code mappedBy="author"} - the OWNING side is {@link Book#author}</li>
     *   <li>{@code CascadeType.ALL} - persist/merge/remove on Author cascades to Books</li>
     *   <li>{@code orphanRemoval=true} - a Book removed from this collection is DELETE'd</li>
     *   <li>{@code FetchType.LAZY} (explicit, same as default for @OneToMany)</li>
     * </ul>
     */
    @OneToMany(mappedBy = "author",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();

    /** Required by JPA: no-arg constructor (may be protected). */
    protected Author() {}

    public Author(String name, String email) {
        this.name  = name;
        this.email = email;
    }

    // ── bidirectional helpers ─────────────────────────────────────────────────

    /** Adds a book and sets the back-reference so both sides are consistent. */
    public void addBook(Book book) {
        books.add(book);
        book.setAuthor(this);
    }

    /**
     * Removes a book from the collection and clears the back-reference.
     * With {@code orphanRemoval=true}, Hibernate will DELETE the book on next flush.
     */
    public void removeBook(Book book) {
        books.remove(book);
        book.setAuthor(null);
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public Long getId()    { return id; }
    public String getName()  { return name; }
    public void setName(String name)    { this.name  = name; }
    public String getEmail() { return email; }
    public void setEmail(String email)  { this.email = email; }
    public List<Book> getBooks() { return books; }

    @Override
    public String toString() {
        return "Author{id=" + id + ", name='" + name + "'}";
    }
}
