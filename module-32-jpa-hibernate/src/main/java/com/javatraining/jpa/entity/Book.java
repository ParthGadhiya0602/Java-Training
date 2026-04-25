package com.javatraining.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Central entity in the module - participates in all three relationship types:
 *
 * <pre>
 *   @ManyToOne  → Author     (owning side, FK = author_id,  LAZY)
 *   @OneToOne   → BookDetail (owning side, FK = detail_id,  EAGER)
 *   @ManyToMany → Tag        (owning side, join table = book_tags, LAZY)
 * </pre>
 *
 * <p><strong>Fetch type summary:</strong>
 * <ul>
 *   <li>{@code author}  - {@code LAZY} (explicit override of JPA default EAGER for @ManyToOne)</li>
 *   <li>{@code detail}  - {@code EAGER} (JPA default for @OneToOne)</li>
 *   <li>{@code tags}    - {@code LAZY}  (JPA default for @ManyToMany)</li>
 * </ul>
 *
 * <p>{@code @DecimalMin("0.01")} on {@code price} is enforced by Bean Validation 3.0
 * at the application layer; Hibernate also adds a column constraint via schema generation.
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Book title must not be blank")
    private String title;

    @Column(nullable = false)
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    private BigDecimal price;

    /**
     * Owning side of Author ↔ Book (LAZY - must be accessed within a transaction).
     * Overrides JPA's default EAGER for @ManyToOne to demonstrate lazy-load behaviour.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    /**
     * Owning side of Book ↔ BookDetail.
     * EAGER (JPA default for @OneToOne) - loaded via JOIN in the same SELECT as Book.
     * cascade=ALL means persisting/removing a Book also persists/removes its detail.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "detail_id")
    private BookDetail detail;

    /**
     * Owning side of Book ↔ Tag many-to-many.
     * CascadeType.PERSIST + MERGE allows new Tags to be saved together with a Book.
     * The join table is {@code book_tags (book_id, tag_id)}.
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_tags",
            joinColumns        = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    protected Book() {}

    public Book(String title, BigDecimal price) {
        this.title = title;
        this.price = price;
    }

    // ── bidirectional many-to-many helper ────────────────────────────────────

    /** Adds a tag and registers this book on the inverse side. */
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getBooks().add(this);
    }

    /** Removes a tag and cleans up the inverse side. */
    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getBooks().remove(this);
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public Long getId()           { return id; }
    public String getTitle()      { return title; }
    public void setTitle(String t){ this.title = t; }
    public BigDecimal getPrice()  { return price; }
    public void setPrice(BigDecimal p) { this.price = p; }
    public Author getAuthor()     { return author; }
    public void setAuthor(Author a)    { this.author = a; }
    public BookDetail getDetail() { return detail; }
    public void setDetail(BookDetail d){ this.detail = d; }
    public Set<Tag> getTags()     { return tags; }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "'}";
    }
}
