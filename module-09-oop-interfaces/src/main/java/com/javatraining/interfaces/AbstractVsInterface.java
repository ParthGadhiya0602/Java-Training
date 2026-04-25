package com.javatraining.interfaces;

import java.util.*;

/**
 * TOPIC: Abstract class vs Interface - when to use each
 *
 * Use an ABSTRACT CLASS when:
 *   • You need instance fields (shared state)
 *   • You need constructors (enforce setup invariants)
 *   • You want non-public members (protected helpers)
 *   • You're providing a partial implementation (Template Method)
 *   • The relationship is IS-A with shared behaviour
 *
 * Use an INTERFACE when:
 *   • You're defining a CAPABILITY or ROLE (Serializable, Comparable, Runnable)
 *   • Multiple unrelated classes share the same capability
 *   • You want multiple inheritance of type
 *   • You're defining a contract without imposing an implementation
 *
 * An abstract class with ALL abstract methods is technically possible,
 * but an interface is almost always a better choice in that case.
 */
public class AbstractVsInterface {

    // -------------------------------------------------------------------------
    // 1. Abstract class - Report generator (Template Method pattern)
    //    Shared infrastructure: header/footer formatting, timing, error handling.
    //    Variable parts: gatherData() and formatBody() are deferred to subclasses.
    // -------------------------------------------------------------------------
    static abstract class ReportGenerator {

        private final String reportName;

        ReportGenerator(String reportName) {
            this.reportName = reportName;
        }

        // Template method - the algorithm skeleton; final to lock the order
        final String generate() {
            long start = System.nanoTime();
            StringBuilder sb = new StringBuilder();
            sb.append(header());
            try {
                List<String> data = gatherData();   // abstract hook
                sb.append(formatBody(data));        // abstract hook
            } catch (Exception e) {
                sb.append("  [ERROR] ").append(e.getMessage()).append("\n");
            }
            sb.append(footer((System.nanoTime() - start) / 1_000_000));
            return sb.toString();
        }

        // Hooks - subclasses must implement
        protected abstract List<String> gatherData();
        protected abstract String formatBody(List<String> data);

        // Concrete helpers shared by all subclasses
        protected String header() {
            return "=== " + reportName + " ===\n";
        }

        protected String footer(long elapsedMs) {
            return "--- generated in " + elapsedMs + "ms ---\n";
        }

        String name() { return reportName; }
    }

    // Concrete subclass - table report
    static class TableReport extends ReportGenerator {
        private final List<String[]> rows;
        private final String[]       headers;

        TableReport(String name, String[] headers, List<String[]> rows) {
            super(name);
            this.headers = headers;
            this.rows    = rows;
        }

        @Override
        protected List<String> gatherData() {
            // In reality: DB query, API call, etc.
            return rows.stream()
                .map(r -> String.join("|", r))
                .toList();
        }

        @Override
        protected String formatBody(List<String> data) {
            StringBuilder sb = new StringBuilder();
            // header row
            sb.append("  ").append(String.join(" | ", headers)).append("\n");
            sb.append("  ").append("─".repeat(40)).append("\n");
            data.forEach(row -> {
                String[] parts = row.split("\\|");
                sb.append("  ");
                for (String p : parts) sb.append(String.format("%-12s ", p));
                sb.append("\n");
            });
            return sb.toString();
        }
    }

    // Concrete subclass - summary report
    static class SummaryReport extends ReportGenerator {
        private final Map<String, Double> metrics;

        SummaryReport(String name, Map<String, Double> metrics) {
            super(name);
            this.metrics = metrics;
        }

        @Override
        protected List<String> gatherData() {
            return metrics.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toList();
        }

        @Override
        protected String formatBody(List<String> data) {
            StringBuilder sb = new StringBuilder();
            data.forEach(d -> {
                String[] kv = d.split("=");
                sb.append(String.format("  %-20s %s%n", kv[0], kv[1]));
            });
            return sb.toString();
        }
    }

    // -------------------------------------------------------------------------
    // 2. Capability interfaces - unrelated classes sharing a role
    //    Serializable, Printable, Exportable are roles, not IS-A relationships.
    // -------------------------------------------------------------------------
    interface Exportable {
        String toCsv();
        default String toJson() {
            // naive default; implementors may override for proper JSON
            return "{\"csv\":\"" + toCsv().replace("\"", "\\\"") + "\"}";
        }
    }

    interface Cacheable {
        String cacheKey();
        default int ttlSeconds() { return 300; } // 5 min default
    }

    // A class that IS-A Employee AND has capabilities: Exportable + Cacheable
    static class Employee implements Exportable, Cacheable {
        private final int    id;
        private final String name;
        private final String department;
        private final double salary;

        Employee(int id, String name, String department, double salary) {
            this.id         = id;
            this.name       = name;
            this.department = department;
            this.salary     = salary;
        }

        int    id()         { return id; }
        String name()       { return name; }
        String department() { return department; }
        double salary()     { return salary; }

        @Override
        public String toCsv() {
            return id + "," + name + "," + department + "," + salary;
        }

        @Override
        public String cacheKey() { return "employee:" + id; }

        @Override
        public String toString() {
            return String.format("Employee{id=%d, name=%s, dept=%s, salary=%.0f}",
                id, name, department, salary);
        }
    }

    // A completely unrelated class that is also Exportable
    static class Invoice implements Exportable {
        private final String number;
        private final double amount;
        private final String vendor;

        Invoice(String number, double amount, String vendor) {
            this.number = number;
            this.amount = amount;
            this.vendor = vendor;
        }

        @Override
        public String toCsv() { return number + "," + amount + "," + vendor; }
    }

    // -------------------------------------------------------------------------
    // 3. Abstract class + interface - common in Java standard library
    //    (e.g. AbstractList implements List; AbstractMap implements Map)
    // -------------------------------------------------------------------------
    interface Repository<T, ID> {
        Optional<T> findById(ID id);
        List<T> findAll();
        void save(T entity);
        void delete(ID id);
        default int count() { return findAll().size(); }
    }

    // Abstract partial implementation - handles the Map storage
    static abstract class InMemoryRepository<T, ID> implements Repository<T, ID> {
        protected final Map<ID, T> store = new LinkedHashMap<>();

        @Override
        public Optional<T> findById(ID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<T> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void delete(ID id) {
            if (!store.containsKey(id))
                throw new NoSuchElementException("Not found: " + id);
            store.remove(id);
        }

        // Abstract method: subclasses define how to extract the ID from the entity
        protected abstract ID idOf(T entity);

        @Override
        public void save(T entity) {
            store.put(idOf(entity), entity);
        }
    }

    // Concrete repository - one line of logic (idOf)
    static class EmployeeRepository extends InMemoryRepository<Employee, Integer> {
        @Override
        protected Integer idOf(Employee e) { return e.id(); }

        // Extra query - not in interface
        List<Employee> findByDepartment(String dept) {
            return findAll().stream()
                .filter(e -> e.department().equals(dept))
                .toList();
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void templateMethodDemo() {
        System.out.println("=== Abstract Class - Template Method ===");

        TableReport table = new TableReport(
            "Employee Table",
            new String[]{"Name", "Dept", "Salary"},
            List.of(
                new String[]{"Alice", "Engineering", "95000"},
                new String[]{"Bob",   "Marketing",   "72000"},
                new String[]{"Carol", "Engineering", "88000"}
            )
        );
        System.out.print(table.generate());

        SummaryReport summary = new SummaryReport(
            "Q1 Metrics",
            Map.of("Revenue", 4_500_000.0, "Expenses", 2_100_000.0, "Profit", 2_400_000.0)
        );
        System.out.print(summary.generate());
    }

    static void capabilityInterfaceDemo() {
        System.out.println("=== Capability Interfaces (Exportable + Cacheable) ===");

        Employee emp = new Employee(1, "Alice", "Engineering", 95_000);
        Invoice  inv = new Invoice("INV-001", 50_000, "Vendor Corp");

        System.out.println("Employee CSV:  " + emp.toCsv());
        System.out.println("Employee JSON: " + emp.toJson());
        System.out.println("Cache key:     " + emp.cacheKey());
        System.out.println("TTL:           " + emp.ttlSeconds() + "s");
        System.out.println("Invoice CSV:   " + inv.toCsv());

        // Polymorphism via interface reference
        List<Exportable> exportables = List.of(emp, inv);
        System.out.println("\nAll as CSV:");
        exportables.forEach(e -> System.out.println("  " + e.toCsv()));
    }

    static void repositoryDemo() {
        System.out.println("\n=== Abstract + Interface: Repository ===");

        EmployeeRepository repo = new EmployeeRepository();
        repo.save(new Employee(1, "Alice", "Engineering", 95_000));
        repo.save(new Employee(2, "Bob",   "Marketing",   72_000));
        repo.save(new Employee(3, "Carol", "Engineering", 88_000));
        repo.save(new Employee(4, "Dave",  "HR",          65_000));

        System.out.println("Count: " + repo.count());
        System.out.println("FindById(2): " + repo.findById(2).orElse(null));
        System.out.println("Engineering team:");
        repo.findByDepartment("Engineering")
            .forEach(e -> System.out.println("  " + e));

        repo.delete(4);
        System.out.println("After delete(4), count: " + repo.count());

        try { repo.delete(99); }
        catch (NoSuchElementException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        templateMethodDemo();
        capabilityInterfaceDemo();
        repositoryDemo();
    }
}
