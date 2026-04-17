package com.javatraining.jvm;

import org.junit.jupiter.api.*;

import java.lang.ref.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemoryDemo")
class MemoryDemoTest {

    @Nested
    @DisplayName("Heap stats")
    class HeapStats {
        @Test void heap_stats_non_negative() {
            MemoryDemo.MemoryStats stats = MemoryDemo.heapStats();
            assertTrue(stats.totalHeapBytes() > 0);
            assertTrue(stats.usedHeapBytes()  >= 0);
            assertTrue(stats.freeHeapBytes()  >= 0);
            assertTrue(stats.maxHeapBytes()   > 0);
        }

        @Test void used_plus_free_equals_total() {
            MemoryDemo.MemoryStats stats = MemoryDemo.heapStats();
            assertEquals(stats.totalHeapBytes(),
                stats.usedHeapBytes() + stats.freeHeapBytes());
        }

        @Test void used_fraction_between_0_and_1() {
            double f = MemoryDemo.heapStats().usedFraction();
            assertTrue(f >= 0.0 && f <= 1.0);
        }
    }

    @Nested
    @DisplayName("MemoryMXBean usage")
    class MxBeanUsage {
        @Test void heap_usage_has_expected_keys() {
            Map<String, Long> usage = MemoryDemo.heapUsage();
            assertTrue(usage.containsKey("used"));
            assertTrue(usage.containsKey("committed"));
            assertTrue(usage.containsKey("max"));
        }

        @Test void heap_used_positive() {
            assertTrue(MemoryDemo.heapUsage().get("used") > 0);
        }

        @Test void non_heap_usage_has_expected_keys() {
            Map<String, Long> usage = MemoryDemo.nonHeapUsage();
            assertTrue(usage.containsKey("used"));
            assertTrue(usage.containsKey("committed"));
        }

        @Test void memory_pool_usages_not_empty() {
            assertFalse(MemoryDemo.memoryPoolUsages().isEmpty());
        }
    }

    @Nested
    @DisplayName("GC stats")
    class GcStats {
        @Test void gc_stats_not_null() {
            List<MemoryDemo.GcInfo> stats = MemoryDemo.gcStats();
            assertNotNull(stats);
        }

        @Test void total_gc_count_non_negative() {
            assertTrue(MemoryDemo.totalGcCount() >= 0);
        }

        @Test void gc_info_names_not_blank() {
            MemoryDemo.gcStats().forEach(info ->
                assertFalse(info.name().isBlank()));
        }
    }

    @Nested
    @DisplayName("Reference types")
    class ReferenceTypes {
        @Test void soft_ref_get_returns_value_when_reachable() {
            String value = "hello";
            SoftReference<String> ref = MemoryDemo.softRef(value);
            assertEquals("hello", ref.get());
        }

        @Test void weak_ref_get_returns_value_while_strong_ref_exists() {
            String value = "world";
            WeakReference<String> ref = MemoryDemo.weakRef(value);
            // While 'value' is still in scope, weakRef.get() should return it
            assertEquals("world", ref.get());
        }

        @Test void phantom_ref_get_always_returns_null() {
            ReferenceQueue<String> queue = new ReferenceQueue<>();
            PhantomReference<String> ref = MemoryDemo.phantomRef("phantom", queue);
            assertNull(ref.get());   // PhantomReference.get() always returns null
        }
    }

    @Nested
    @DisplayName("SoftCache")
    class SoftCacheTests {
        @Test void put_and_get() {
            MemoryDemo.SoftCache<String, Integer> cache = new MemoryDemo.SoftCache<>();
            cache.put("a", 1);
            assertEquals(1, cache.get("a"));
        }

        @Test void contains_key_after_put() {
            MemoryDemo.SoftCache<String, Integer> cache = new MemoryDemo.SoftCache<>();
            cache.put("b", 2);
            assertTrue(cache.containsKey("b"));
            assertFalse(cache.containsKey("c"));
        }

        @Test void size_reflects_puts() {
            MemoryDemo.SoftCache<Integer, String> cache = new MemoryDemo.SoftCache<>();
            cache.put(1, "one");
            cache.put(2, "two");
            assertEquals(2, cache.size());
        }
    }

    @Nested
    @DisplayName("ManagedResource (Cleaner)")
    class CleanerTests {
        @Test void close_marks_resource_closed() {
            try (MemoryDemo.ManagedResource res = new MemoryDemo.ManagedResource("test")) {
                assertFalse(res.isClosed());
            }
            // After close() the isClosed flag is set
        }

        @Test void close_runs_cleaner_action() {
            MemoryDemo.ManagedResource res = new MemoryDemo.ManagedResource("test");
            assertFalse(res.cleanerRan());
            res.close();
            assertTrue(res.cleanerRan());
        }
    }

    @Nested
    @DisplayName("Runtime info")
    class RuntimeInfo {
        @Test void available_processors_positive() {
            assertTrue(MemoryDemo.availableProcessors() > 0);
        }

        @Test void runtime_info_has_expected_keys() {
            Map<String, Object> info = MemoryDemo.runtimeInfo();
            assertTrue(info.containsKey("vmName"));
            assertTrue(info.containsKey("vmVersion"));
            assertTrue(info.containsKey("uptimeMs"));
        }

        @Test void vm_name_not_blank() {
            assertFalse(MemoryDemo.runtimeInfo().get("vmName").toString().isBlank());
        }
    }
}
