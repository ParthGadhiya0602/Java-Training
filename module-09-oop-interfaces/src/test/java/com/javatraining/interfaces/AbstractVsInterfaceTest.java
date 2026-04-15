package com.javatraining.interfaces;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class AbstractVsInterfaceTest {

    // -----------------------------------------------------------------------
    // ReportGenerator — Template Method
    // -----------------------------------------------------------------------
    @Test
    void table_report_generate_contains_header_and_footer() {
        List<String[]> rows = List.<String[]>of(new String[]{"A", "B"});
        AbstractVsInterface.TableReport r = new AbstractVsInterface.TableReport(
            "Test Report",
            new String[]{"Col1", "Col2"},
            rows
        );
        String output = r.generate();
        assertTrue(output.contains("Test Report"));
        assertTrue(output.contains("generated in"));
    }

    @Test
    void table_report_contains_data_rows() {
        List<String[]> rows2 = List.<String[]>of(new String[]{"Alice"}, new String[]{"Bob"});
        AbstractVsInterface.TableReport r = new AbstractVsInterface.TableReport(
            "T",
            new String[]{"Name"},
            rows2
        );
        String output = r.generate();
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Bob"));
    }

    @Test
    void summary_report_contains_metrics() {
        AbstractVsInterface.SummaryReport r = new AbstractVsInterface.SummaryReport(
            "Metrics",
            Map.of("Revenue", 1_000_000.0, "Cost", 500_000.0)
        );
        String output = r.generate();
        assertTrue(output.contains("Revenue"));
        assertTrue(output.contains("1000000"));
    }

    // -----------------------------------------------------------------------
    // Employee — Exportable + Cacheable
    // -----------------------------------------------------------------------
    @Test
    void employee_to_csv_format() {
        AbstractVsInterface.Employee e =
            new AbstractVsInterface.Employee(1, "Alice", "Eng", 95_000);
        String csv = e.toCsv();
        assertTrue(csv.contains("1"));
        assertTrue(csv.contains("Alice"));
        assertTrue(csv.contains("Eng"));
    }

    @Test
    void employee_cache_key_contains_id() {
        AbstractVsInterface.Employee e =
            new AbstractVsInterface.Employee(42, "Bob", "HR", 70_000);
        assertTrue(e.cacheKey().contains("42"));
    }

    @Test
    void employee_default_ttl_is_300() {
        AbstractVsInterface.Employee e =
            new AbstractVsInterface.Employee(1, "Alice", "Eng", 95_000);
        assertEquals(300, e.ttlSeconds());
    }

    @Test
    void employee_to_json_wraps_csv() {
        AbstractVsInterface.Employee e =
            new AbstractVsInterface.Employee(1, "Alice", "Eng", 95_000);
        String json = e.toJson();
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    // -----------------------------------------------------------------------
    // EmployeeRepository — Abstract + Interface
    // -----------------------------------------------------------------------
    @Test
    void repository_save_and_find_by_id() {
        AbstractVsInterface.EmployeeRepository repo =
            new AbstractVsInterface.EmployeeRepository();
        AbstractVsInterface.Employee e =
            new AbstractVsInterface.Employee(1, "Alice", "Eng", 95_000);
        repo.save(e);
        assertTrue(repo.findById(1).isPresent());
        assertEquals("Alice", repo.findById(1).get().name());
    }

    @Test
    void repository_find_by_id_absent_returns_empty() {
        AbstractVsInterface.EmployeeRepository repo =
            new AbstractVsInterface.EmployeeRepository();
        assertFalse(repo.findById(999).isPresent());
    }

    @Test
    void repository_count_default_method() {
        AbstractVsInterface.EmployeeRepository repo =
            new AbstractVsInterface.EmployeeRepository();
        repo.save(new AbstractVsInterface.Employee(1, "A", "X", 1));
        repo.save(new AbstractVsInterface.Employee(2, "B", "X", 1));
        assertEquals(2, repo.count());
    }

    @Test
    void repository_delete_removes_entry() {
        AbstractVsInterface.EmployeeRepository repo =
            new AbstractVsInterface.EmployeeRepository();
        repo.save(new AbstractVsInterface.Employee(1, "A", "X", 1));
        repo.delete(1);
        assertFalse(repo.findById(1).isPresent());
        assertEquals(0, repo.count());
    }

    @Test
    void repository_delete_missing_throws() {
        AbstractVsInterface.EmployeeRepository repo =
            new AbstractVsInterface.EmployeeRepository();
        assertThrows(NoSuchElementException.class, () -> repo.delete(999));
    }

    @Test
    void repository_find_by_department() {
        AbstractVsInterface.EmployeeRepository repo =
            new AbstractVsInterface.EmployeeRepository();
        repo.save(new AbstractVsInterface.Employee(1, "Alice", "Eng", 90_000));
        repo.save(new AbstractVsInterface.Employee(2, "Bob",   "HR",  70_000));
        repo.save(new AbstractVsInterface.Employee(3, "Carol", "Eng", 85_000));

        List<AbstractVsInterface.Employee> eng = repo.findByDepartment("Eng");
        assertEquals(2, eng.size());
        assertTrue(eng.stream().allMatch(e -> e.department().equals("Eng")));
    }

    @Test
    void repository_save_overwrites_same_id() {
        AbstractVsInterface.EmployeeRepository repo =
            new AbstractVsInterface.EmployeeRepository();
        repo.save(new AbstractVsInterface.Employee(1, "Alice", "Eng", 90_000));
        repo.save(new AbstractVsInterface.Employee(1, "Alice", "Eng", 95_000)); // update
        assertEquals(1, repo.count());
        assertEquals(95_000.0, repo.findById(1).get().salary(), 1e-9);
    }
}
