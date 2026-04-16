package com.javatraining.nested;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NestedClassPatternsTest {

    // ── ImmutableQueue ────────────────────────────────────────────────────────

    @Nested
    class ImmutableQueueTests {

        @Test
        void empty_queue_is_empty() {
            assertTrue(NestedClassPatterns.ImmutableQueue.empty().isEmpty());
            assertEquals(0, NestedClassPatterns.ImmutableQueue.empty().size());
        }

        @Test
        void enqueue_increases_size() {
            var q = NestedClassPatterns.ImmutableQueue.<String>empty()
                .enqueue("a").enqueue("b");
            assertEquals(2, q.size());
        }

        @Test
        void peek_returns_front() {
            var q = NestedClassPatterns.ImmutableQueue.<Integer>empty()
                .enqueue(1).enqueue(2).enqueue(3);
            assertEquals(1, q.peek());
        }

        @Test
        void dequeue_removes_front_FIFO() {
            var q = NestedClassPatterns.ImmutableQueue.<String>empty()
                .enqueue("first").enqueue("second").enqueue("third");
            assertEquals("first",  q.peek());
            assertEquals("second", q.dequeue().peek());
            assertEquals("third",  q.dequeue().dequeue().peek());
        }

        @Test
        void dequeue_does_not_mutate_original() {
            var q  = NestedClassPatterns.ImmutableQueue.<String>empty()
                .enqueue("a").enqueue("b");
            var q2 = q.dequeue();
            assertEquals(2, q.size());   // original unchanged
            assertEquals(1, q2.size());
        }

        @Test
        void toList_preserves_FIFO_order() {
            var q = NestedClassPatterns.ImmutableQueue.<Integer>empty()
                .enqueue(10).enqueue(20).enqueue(30);
            assertEquals(List.of(10, 20, 30), q.toList());
        }

        @Test
        void peek_on_empty_throws() {
            assertThrows(NoSuchElementException.class,
                () -> NestedClassPatterns.ImmutableQueue.empty().peek());
        }

        @Test
        void dequeue_on_empty_throws() {
            assertThrows(NoSuchElementException.class,
                () -> NestedClassPatterns.ImmutableQueue.empty().dequeue());
        }
    }

    // ── Query / Query.Builder ─────────────────────────────────────────────────

    @Nested
    class QueryBuilderTests {

        @Test
        void simple_select_star() {
            String sql = new NestedClassPatterns.Query.Builder()
                .from("users")
                .build()
                .toSql();
            assertEquals("SELECT * FROM users", sql);
        }

        @Test
        void select_specific_columns() {
            String sql = new NestedClassPatterns.Query.Builder()
                .from("products")
                .select("id", "name", "price")
                .build()
                .toSql();
            assertTrue(sql.startsWith("SELECT id, name, price FROM products"));
        }

        @Test
        void single_where_clause() {
            String sql = new NestedClassPatterns.Query.Builder()
                .from("orders")
                .where("status = 'ACTIVE'")
                .build()
                .toSql();
            assertTrue(sql.contains("WHERE status = 'ACTIVE'"));
        }

        @Test
        void multiple_where_clauses_joined_with_AND() {
            String sql = new NestedClassPatterns.Query.Builder()
                .from("orders")
                .where("price > 100")
                .where("category = 'A'")
                .build()
                .toSql();
            assertTrue(sql.contains("WHERE price > 100 AND category = 'A'"));
        }

        @Test
        void order_by_appended() {
            String sql = new NestedClassPatterns.Query.Builder()
                .from("users")
                .orderBy("name")
                .build()
                .toSql();
            assertTrue(sql.endsWith("ORDER BY name"));
        }

        @Test
        void limit_appended() {
            String sql = new NestedClassPatterns.Query.Builder()
                .from("users")
                .limit(10)
                .build()
                .toSql();
            assertTrue(sql.endsWith("LIMIT 10"));
        }

        @Test
        void columns_list_is_unmodifiable() {
            NestedClassPatterns.Query q = new NestedClassPatterns.Query.Builder()
                .from("t")
                .select("a", "b")
                .build();
            assertThrows(UnsupportedOperationException.class,
                () -> q.columns().add("c"));
        }

        @Test
        void missing_table_throws() {
            assertThrows(IllegalStateException.class,
                () -> new NestedClassPatterns.Query.Builder().build());
        }

        @Test
        void blank_table_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> new NestedClassPatterns.Query.Builder().from("  ").build());
        }
    }

    // ── FileSystem composite ──────────────────────────────────────────────────

    @Nested
    class FileSystemTests {

        NestedClassPatterns.FileSystem.Directory root;

        @BeforeEach
        void setUp() {
            root = new NestedClassPatterns.FileSystem.Directory("root")
                .add(new NestedClassPatterns.FileSystem.File("a.txt",  100))
                .add(new NestedClassPatterns.FileSystem.File("b.txt",  200))
                .add(new NestedClassPatterns.FileSystem.Directory("sub")
                    .add(new NestedClassPatterns.FileSystem.File("c.java", 400)));
        }

        @Test
        void directory_size_sums_children_recursively() {
            assertEquals(700, root.sizeBytes());
        }

        @Test
        void file_size_is_its_own_value() {
            NestedClassPatterns.FileSystem.File f =
                new NestedClassPatterns.FileSystem.File("x.txt", 512);
            assertEquals(512, f.sizeBytes());
        }

        @Test
        void file_depth_is_zero() {
            assertEquals(0, new NestedClassPatterns.FileSystem.File("f", 1).depth());
        }

        @Test
        void directory_depth_is_max_child_depth_plus_one() {
            // root has a sub-dir containing one file → depth 2
            assertEquals(2, root.depth());
        }

        @Test
        void file_count_counts_leaves_recursively() {
            assertEquals(3, root.fileCount());
        }

        @Test
        void find_returns_matching_files() {
            List<NestedClassPatterns.FileSystem.FileSystemItem> javaFiles =
                root.find(item -> item instanceof NestedClassPatterns.FileSystem.File f
                                  && f.name().endsWith(".java"));
            assertEquals(1, javaFiles.size());
            assertEquals("c.java", javaFiles.get(0).name());
        }

        @Test
        void find_returns_matching_directories() {
            List<NestedClassPatterns.FileSystem.FileSystemItem> dirs =
                root.find(item -> item instanceof NestedClassPatterns.FileSystem.Directory);
            // root itself + "sub"
            assertEquals(2, dirs.size());
        }

        @Test
        void children_list_is_unmodifiable() {
            assertThrows(UnsupportedOperationException.class,
                () -> root.children().add(
                    new NestedClassPatterns.FileSystem.File("x", 1)));
        }

        @Test
        void render_contains_file_names() {
            String rendered = root.render("");
            assertTrue(rendered.contains("a.txt"));
            assertTrue(rendered.contains("b.txt"));
            assertTrue(rendered.contains("c.java"));
        }
    }
}
