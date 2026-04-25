package com.javatraining.nosql.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document mapped to the {@code products} collection.
 *
 * <p>Key differences from a JPA entity:
 * <ul>
 *   <li>No table joins - tags are an embedded array, not a separate collection.</li>
 *   <li>Id is a {@code String} (MongoDB ObjectId) not a {@code Long}.</li>
 *   <li>{@code @Field} controls the JSON field name in the stored document
 *       ({@code in_stock} rather than the Java field name {@code inStock}).</li>
 *   <li>No schema enforcement - documents in the same collection can have
 *       different shapes; the Java class is the agreed schema.</li>
 * </ul>
 */
@Document("products")
public class Product {

    @Id
    private String id;

    @Indexed
    private String name;

    @Indexed
    private String category;

    /**
     * Stored as MongoDB's {@code Decimal128} (not a string) so that numeric
     * operators ({@code $lt}, {@code $gte}, {@code $multiply}, {@code $avg}) work correctly.
     */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    /** Stored in MongoDB as {@code in_stock}. */
    @Field("in_stock")
    private boolean inStock;

    /** Embedded array - no join table, no foreign key. */
    private List<String> tags = new ArrayList<>();

    protected Product() {}

    public Product(String name, String category, BigDecimal price, boolean inStock) {
        this.name     = name;
        this.category = category;
        this.price    = price;
        this.inStock  = inStock;
    }

    public Product tag(String... moreTags) {
        tags.addAll(List.of(moreTags));
        return this;
    }

    // Getters
    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getCategory()    { return category; }
    public BigDecimal getPrice()   { return price; }
    public boolean isInStock()     { return inStock; }
    public List<String> getTags()  { return tags; }

    // Setters
    public void setName(String name)           { this.name = name; }
    public void setCategory(String category)   { this.category = category; }
    public void setPrice(BigDecimal price)     { this.price = price; }
    public void setInStock(boolean inStock)    { this.inStock = inStock; }
    public void setTags(List<String> tags)     { this.tags = tags; }
}
