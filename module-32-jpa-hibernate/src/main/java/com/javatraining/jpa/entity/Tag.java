package com.javatraining.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

/**
 * A label/category that can be applied to many books — inverse side of the
 * Book ↔ Tag {@code @ManyToMany} relationship.
 *
 * <p>The join table ({@code book_tags}) is owned by {@link Book#tags}.
 * {@code Tag} sees the relationship as {@code mappedBy = "tags"} and does
 * not control the join table.
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Tag name must not be blank")
    private String name;

    /**
     * Inverse side of Book ↔ Tag.
     * LAZY by default for {@code @ManyToMany}.
     * Accessing this outside an open EntityManager → LazyInitializationException.
     */
    @ManyToMany(mappedBy = "tags")
    private Set<Book> books = new HashSet<>();

    protected Tag() {}

    public Tag(String name) {
        this.name = name;
    }

    public Long getId()         { return id; }
    public String getName()     { return name; }
    public Set<Book> getBooks() { return books; }

    @Override
    public String toString() {
        return "Tag{id=" + id + ", name='" + name + "'}";
    }
}
