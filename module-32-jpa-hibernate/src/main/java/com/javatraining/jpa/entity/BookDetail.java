package com.javatraining.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Extra metadata for a book - models the {@code OneToOne} relationship.
 *
 * <p>{@link Book#detail} owns the FK column ({@code detail_id}) and is declared
 * {@code EAGER}, so {@code BookDetail} is loaded in the same query as {@code Book}
 * (usually via a LEFT JOIN).
 *
 * <p>This class is intentionally kept simple to focus attention on the
 * fetch-type and cascade demonstrations in the tests.
 */
@Entity
@Table(name = "book_details")
public class BookDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String isbn;

    @Column(name = "page_count")
    @Min(value = 1, message = "Page count must be at least 1")
    private int pageCount;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    protected BookDetail() {}

    public BookDetail(String isbn, int pageCount, String synopsis) {
        this.isbn      = isbn;
        this.pageCount = pageCount;
        this.synopsis  = synopsis;
    }

    public Long getId()        { return id; }
    public String getIsbn()    { return isbn; }
    public int getPageCount()  { return pageCount; }
    public String getSynopsis(){ return synopsis; }

    @Override
    public String toString() {
        return "BookDetail{isbn='" + isbn + "', pages=" + pageCount + "}";
    }
}
