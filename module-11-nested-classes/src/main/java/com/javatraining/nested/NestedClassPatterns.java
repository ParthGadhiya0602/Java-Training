package com.javatraining.nested;

import java.util.*;
import java.util.function.Function;

/**
 * TOPIC: Real-world patterns with nested classes
 *
 * Pattern 1 - Private static nested for encapsulation
 *   Hide implementation details (Node, Bucket) inside the class that uses them.
 *   Clients never see or depend on the internals.
 *
 * Pattern 2 - Builder as static nested class
 *   The builder lives alongside the target type; both are in the same top-level
 *   class.  Static (not inner) because the builder doesn't need an outer instance.
 *
 * Pattern 3 - Composite pattern
 *   A tree of nodes where both leaf and branch implement the same interface.
 *   Branch holds a List<Component>; all children are Components.
 *   Perfect for: file systems, UI trees, expression trees, org charts.
 */
public class NestedClassPatterns {

    // -------------------------------------------------------------------------
    // Pattern 1 - Immutable singly-linked queue with hidden Node
    //    Node is a private implementation detail - no external code ever
    //    references it.  Making it static avoids holding outer references.
    // -------------------------------------------------------------------------
    static final class ImmutableQueue<T> {

        // Private static nested - purely internal
        // 'next' is mutable so enqueue() can link the old tail to the new node
        private static final class Node<T> {
            final T  value;
            Node<T>  next;   // non-final: enqueue() sets this after creation

            Node(T value) {
                this.value = value;
                this.next  = null;
            }
        }

        // head → ... → tail (oldest to newest)
        private final Node<T> head;
        private final Node<T> tail;
        private final int     size;

        // Private - use empty() or enqueue() to create
        private ImmutableQueue(Node<T> head, Node<T> tail, int size) {
            this.head = head;
            this.tail = tail;
            this.size = size;
        }

        @SuppressWarnings("unchecked")
        static <T> ImmutableQueue<T> empty() {
            return new ImmutableQueue<>(null, null, 0);
        }

        /** Returns a new queue with value appended at the end. */
        ImmutableQueue<T> enqueue(T value) {
            Node<T> node = new Node<>(value);
            if (tail != null) tail.next = node;   // link old tail → new node
            return new ImmutableQueue<>(head == null ? node : head, node, size + 1);
        }

        /** Returns the front element without removing it. */
        T peek() {
            if (isEmpty()) throw new NoSuchElementException("queue is empty");
            return head.value;
        }

        /** Returns a new queue without the front element. */
        ImmutableQueue<T> dequeue() {
            if (isEmpty()) throw new NoSuchElementException("queue is empty");
            return new ImmutableQueue<>(head.next, head.next == null ? null : tail, size - 1);
        }

        boolean isEmpty() { return size == 0; }
        int     size()    { return size; }

        List<T> toList() {
            List<T> list = new ArrayList<>(size);
            for (Node<T> n = head; n != null; n = n.next) list.add(n.value);
            return list;
        }

        @Override public String toString() { return toList().toString(); }
    }

    // -------------------------------------------------------------------------
    // Pattern 2 - Builder as static nested class
    //    QueryBuilder builds a SQL-like query string.  The builder is nested
    //    inside Query so both stay in the same conceptual unit.
    // -------------------------------------------------------------------------
    static final class Query {
        private final String        table;
        private final List<String>  columns;
        private final List<String>  conditions;
        private final String        orderBy;
        private final int           limit;

        private Query(Builder b) {
            this.table      = b.table;
            this.columns    = Collections.unmodifiableList(new ArrayList<>(b.columns));
            this.conditions = Collections.unmodifiableList(new ArrayList<>(b.conditions));
            this.orderBy    = b.orderBy;
            this.limit      = b.limit;
        }

        String        table()      { return table; }
        List<String>  columns()    { return columns; }
        List<String>  conditions() { return conditions; }
        String        orderBy()    { return orderBy; }
        int           limit()      { return limit; }

        /** Renders the query as a SQL string (simplified, no escaping). */
        String toSql() {
            String cols  = columns.isEmpty() ? "*" : String.join(", ", columns);
            StringBuilder sql = new StringBuilder("SELECT ")
                .append(cols)
                .append(" FROM ").append(table);
            if (!conditions.isEmpty())
                sql.append(" WHERE ").append(String.join(" AND ", conditions));
            if (orderBy != null)
                sql.append(" ORDER BY ").append(orderBy);
            if (limit > 0)
                sql.append(" LIMIT ").append(limit);
            return sql.toString();
        }

        @Override public String toString() { return toSql(); }

        // Static nested builder - no outer instance needed
        static final class Builder {
            private String       table;
            private List<String> columns    = new ArrayList<>();
            private List<String> conditions = new ArrayList<>();
            private String       orderBy    = null;
            private int          limit      = 0;

            Builder from(String table) {
                if (table == null || table.isBlank())
                    throw new IllegalArgumentException("table required");
                this.table = table;
                return this;
            }

            Builder select(String... cols) {
                Collections.addAll(columns, cols);
                return this;
            }

            Builder where(String condition) {
                conditions.add(condition);
                return this;
            }

            Builder orderBy(String col)   { this.orderBy = col; return this; }
            Builder limit(int n)          { this.limit = n;     return this; }

            Query build() {
                if (table == null) throw new IllegalStateException("table is required");
                return new Query(this);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Pattern 3 - Composite: file-system tree
    //    FileSystemItem is the component interface.
    //    File is a leaf.  Directory is a composite holding child items.
    //    Both are static nested classes inside FileSystem.
    // -------------------------------------------------------------------------
    static final class FileSystem {

        // Component interface
        interface FileSystemItem {
            String name();
            long   sizeBytes();
            int    depth();
            String render(String indent);
        }

        // Leaf
        static final class File implements FileSystemItem {
            private final String name;
            private final long   sizeBytes;

            File(String name, long sizeBytes) {
                this.name      = Objects.requireNonNull(name);
                this.sizeBytes = sizeBytes;
            }

            @Override public String name()      { return name; }
            @Override public long   sizeBytes() { return sizeBytes; }
            @Override public int    depth()     { return 0; }

            @Override
            public String render(String indent) {
                return indent + "📄 " + name + " (" + sizeBytes + "B)";
            }
        }

        // Composite
        static final class Directory implements FileSystemItem {
            private final String               name;
            private final List<FileSystemItem> children = new ArrayList<>();

            Directory(String name) {
                this.name = Objects.requireNonNull(name);
            }

            Directory add(FileSystemItem item) {
                children.add(item);
                return this;
            }

            List<FileSystemItem> children() { return Collections.unmodifiableList(children); }

            @Override public String name() { return name; }

            @Override
            public long sizeBytes() {
                return children.stream().mapToLong(FileSystemItem::sizeBytes).sum();
            }

            @Override
            public int depth() {
                return children.stream().mapToInt(FileSystemItem::depth).max().orElse(0) + 1;
            }

            @Override
            public String render(String indent) {
                StringBuilder sb = new StringBuilder(indent + "📁 " + name
                    + " (" + sizeBytes() + "B)\n");
                for (FileSystemItem child : children) {
                    sb.append(child.render(indent + "  ")).append("\n");
                }
                return sb.toString().stripTrailing();
            }

            /** Count all leaf files recursively. */
            long fileCount() {
                return children.stream().mapToLong(c ->
                    c instanceof Directory d ? d.fileCount() : 1
                ).sum();
            }

            /** Find items matching a predicate anywhere in the tree. */
            List<FileSystemItem> find(java.util.function.Predicate<FileSystemItem> predicate) {
                List<FileSystemItem> result = new ArrayList<>();
                if (predicate.test(this)) result.add(this);
                for (FileSystemItem child : children) {
                    if (child instanceof Directory d) {
                        result.addAll(d.find(predicate));
                    } else if (predicate.test(child)) {
                        result.add(child);
                    }
                }
                return result;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void queueDemo() {
        System.out.println("=== ImmutableQueue (private static nested Node) ===");
        ImmutableQueue<String> q = ImmutableQueue.<String>empty()
            .enqueue("first")
            .enqueue("second")
            .enqueue("third");

        System.out.println("queue:  " + q);
        System.out.println("peek:   " + q.peek());
        System.out.println("size:   " + q.size());

        ImmutableQueue<String> q2 = q.dequeue();
        System.out.println("after dequeue: " + q2);
        System.out.println("original unchanged: " + q);

        try { ImmutableQueue.empty().dequeue(); }
        catch (NoSuchElementException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    static void queryBuilderDemo() {
        System.out.println("\n=== Query (static nested Builder) ===");

        Query simple = new Query.Builder()
            .from("users")
            .select("id", "name", "email")
            .where("status = 'ACTIVE'")
            .orderBy("name")
            .limit(50)
            .build();

        System.out.println(simple.toSql());

        Query all = new Query.Builder()
            .from("products")
            .where("price > 1000")
            .where("category = 'ELECTRONICS'")
            .build();

        System.out.println(all.toSql());

        try { new Query.Builder().build(); }
        catch (IllegalStateException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    static void fileSystemDemo() {
        System.out.println("\n=== FileSystem Composite (static nested File/Directory) ===");

        FileSystem.Directory root = new FileSystem.Directory("project")
            .add(new FileSystem.File("README.md", 1024))
            .add(new FileSystem.File("pom.xml",   2048))
            .add(new FileSystem.Directory("src")
                .add(new FileSystem.Directory("main")
                    .add(new FileSystem.File("Main.java",   4096))
                    .add(new FileSystem.File("Config.java", 2048)))
                .add(new FileSystem.Directory("test")
                    .add(new FileSystem.File("MainTest.java", 3072))))
            .add(new FileSystem.Directory("docs")
                .add(new FileSystem.File("design.pdf", 204800)));

        System.out.println(root.render(""));
        System.out.println("\ntotal size:  " + root.sizeBytes() + "B");
        System.out.println("file count:  " + root.fileCount());
        System.out.println("depth:       " + root.depth());

        List<FileSystem.FileSystemItem> javaFiles = root.find(
            item -> item instanceof FileSystem.File f && f.name().endsWith(".java"));
        System.out.println("java files:  " + javaFiles.stream()
            .map(FileSystem.FileSystemItem::name).toList());
    }

    public static void main(String[] args) {
        queueDemo();
        queryBuilderDemo();
        fileSystemDemo();
    }
}
