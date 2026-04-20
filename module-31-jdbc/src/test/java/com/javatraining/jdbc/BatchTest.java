package com.javatraining.jdbc;

import com.javatraining.jdbc.batch.BatchImporter;
import com.javatraining.jdbc.core.DatabaseInitializer;
import com.javatraining.jdbc.model.Product;
import com.javatraining.jdbc.repository.ProductRepository;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies JDBC batch processing:
 * <ul>
 *   <li>All rows in a batch are inserted/updated</li>
 *   <li>The returned row-count array has one entry per batched statement</li>
 *   <li>Empty batch returns an empty array without error</li>
 *   <li>Large batches (1 000 rows) complete correctly</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BatchTest {

    private Connection      conn;
    private ProductRepository productRepo;
    private BatchImporter   importer;

    @BeforeAll
    void setUpDatabase() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:jdbcbatch;DB_CLOSE_DELAY=-1", "sa", "");
        DatabaseInitializer.initialize(conn);
        productRepo = new ProductRepository(conn);
        importer    = new BatchImporter(conn);
    }

    @AfterAll
    void tearDown() throws Exception {
        DatabaseInitializer.dropAll(conn);
        conn.close();
    }

    @BeforeEach
    void clearData() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM products");
        }
    }

    // ── Batch insert ─────────────────────────────────────────────────────────

    @Test
    void batch_insert_stores_all_products() throws Exception {
        List<Product> products = List.of(
                Product.of("Widget A", 10.00, 50),
                Product.of("Widget B", 20.00, 30),
                Product.of("Widget C", 30.00, 20)
        );
        importer.insertBatch(products);
        assertEquals(3, productRepo.count());
    }

    @Test
    void batch_insert_returns_one_row_count_per_statement() throws Exception {
        List<Product> products = List.of(
                Product.of("Alpha", 1.00, 1),
                Product.of("Beta",  2.00, 2)
        );
        int[] counts = importer.insertBatch(products);
        assertEquals(2, counts.length);
        for (int c : counts) assertEquals(1, c, "Each INSERT should affect exactly 1 row");
    }

    @Test
    void batch_insert_empty_list_returns_empty_array_without_error() throws Exception {
        int[] counts = importer.insertBatch(List.of());
        assertEquals(0, counts.length);
        assertEquals(0, productRepo.count());
    }

    @Test
    void batch_insert_large_volume_completes_correctly() throws Exception {
        List<Product> products = IntStream.rangeClosed(1, 1_000)
                .mapToObj(i -> Product.of("Product-" + i, i * 0.99, i))
                .toList();
        int[] counts = importer.insertBatch(products);
        assertEquals(1_000, counts.length);
        assertEquals(1_000, productRepo.count());
    }

    @Test
    void batch_insert_preserves_insertion_order_by_id() throws Exception {
        importer.insertBatch(List.of(
                Product.of("First",  1.00, 1),
                Product.of("Second", 2.00, 2),
                Product.of("Third",  3.00, 3)
        ));
        List<Product> all = productRepo.findAll();
        assertEquals("First",  all.get(0).name());
        assertEquals("Second", all.get(1).name());
        assertEquals("Third",  all.get(2).name());
    }

    // ── Batch update ─────────────────────────────────────────────────────────

    @Test
    void batch_update_modifies_all_specified_prices() throws Exception {
        int id1 = productRepo.insert(Product.of("Item 1", 10.00, 5));
        int id2 = productRepo.insert(Product.of("Item 2", 20.00, 5));

        importer.updatePricesBatch(Map.of(
                id1, new BigDecimal("15.00"),
                id2, new BigDecimal("25.00")
        ));

        assertEquals(new BigDecimal("15.00"), productRepo.findById(id1).orElseThrow().price());
        assertEquals(new BigDecimal("25.00"), productRepo.findById(id2).orElseThrow().price());
    }

    @Test
    void batch_update_returns_one_row_count_per_entry() throws Exception {
        int id1 = productRepo.insert(Product.of("X", 10.00, 1));
        int id2 = productRepo.insert(Product.of("Y", 20.00, 1));

        int[] counts = importer.updatePricesBatch(Map.of(
                id1, new BigDecimal("11.00"),
                id2, new BigDecimal("21.00")
        ));
        assertEquals(2, counts.length);
        for (int c : counts) assertEquals(1, c);
    }
}
