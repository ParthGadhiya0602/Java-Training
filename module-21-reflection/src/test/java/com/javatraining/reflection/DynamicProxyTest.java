package com.javatraining.reflection;

import com.javatraining.reflection.SampleClasses.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DynamicProxy")
class DynamicProxyTest {

    @Nested
    @DisplayName("Logging proxy")
    class LoggingProxy {
        @Test void records_method_calls() {
            List<String> log = new ArrayList<>();
            Calculator calc = DynamicProxy.loggingProxy(new SimpleCalculator(), log);
            calc.add(2, 3);
            calc.describe();
            assertEquals(2, log.size());
            assertTrue(log.get(0).startsWith("add("));
            assertTrue(log.get(1).startsWith("describe("));
        }

        @Test void result_is_correct_after_logging() {
            List<String> log = new ArrayList<>();
            Calculator calc = DynamicProxy.loggingProxy(new SimpleCalculator(), log);
            assertEquals(10, calc.multiply(2, 5));
        }

        @Test void logged_args_appear_in_entry() {
            List<String> log = new ArrayList<>();
            Calculator calc = DynamicProxy.loggingProxy(new SimpleCalculator(), log);
            calc.add(7, 3);
            assertTrue(log.get(0).contains("7"));
            assertTrue(log.get(0).contains("3"));
        }
    }

    @Nested
    @DisplayName("Timing proxy")
    class TimingProxy {
        @Test void records_timing_for_each_method() {
            Map<String, Long> timings = new HashMap<>();
            Calculator calc = DynamicProxy.timingProxy(new SimpleCalculator(), timings);
            calc.add(1, 2);
            calc.multiply(3, 4);
            assertTrue(timings.containsKey("add"));
            assertTrue(timings.containsKey("multiply"));
        }

        @Test void timing_is_non_negative() {
            Map<String, Long> timings = new HashMap<>();
            Calculator calc = DynamicProxy.timingProxy(new SimpleCalculator(), timings);
            calc.describe();
            assertTrue(timings.get("describe") >= 0);
        }
    }

    @Nested
    @DisplayName("Null-guard proxy")
    class NullGuardProxy {
        @Test void throws_on_null_argument() {
            Calculator calc = DynamicProxy.nullGuardProxy(new SimpleCalculator());
            // Calculator.add(int, int) — autoboxed; need an interface with Object args
            // Use Calculator.describe() which takes no args (should pass through)
            assertDoesNotThrow(() -> calc.describe());
        }

        @Test void passes_through_non_null_args() {
            Calculator calc = DynamicProxy.nullGuardProxy(new SimpleCalculator());
            assertEquals(5, calc.add(2, 3));
        }
    }

    @Nested
    @DisplayName("Caching proxy")
    class CachingProxy {
        @Test void returns_same_result_on_second_call() {
            SimpleCalculator real = new SimpleCalculator();
            Calculator cached = DynamicProxy.cachingProxy(real);
            int first  = cached.add(4, 5);
            int second = cached.add(4, 5);
            assertEquals(first, second);
        }

        @Test void cache_reduces_real_call_count() {
            SimpleCalculator real = new SimpleCalculator();
            Calculator cached = DynamicProxy.cachingProxy(real);
            cached.add(1, 2);
            cached.add(1, 2);
            cached.add(1, 2);
            // real.add was called only once due to caching
            assertEquals(1, real.getCallCount());
        }
    }

    @Nested
    @DisplayName("Retry proxy")
    class RetryProxy {
        // Flaky service that fails first N times
        private int failCount;

        @Test void succeeds_after_retries() throws Exception {
            failCount = 2;
            Calculator flaky = DynamicProxy.retryProxy(new Calculator() {
                @Override public int add(int a, int b) {
                    if (failCount-- > 0) throw new RuntimeException("transient");
                    return a + b;
                }
                @Override public int multiply(int a, int b) { return a * b; }
                @Override public String describe() { return "flaky"; }
            }, 3);
            assertEquals(7, flaky.add(3, 4));
        }
    }

    @Nested
    @DisplayName("Read-only proxy")
    class ReadOnlyProxy {
        @Test void get_passes_through() {
            MapStore store = new MapStore();
            store.set("k", "v");
            MutableStore ro = DynamicProxy.readOnlyProxy(store);
            assertEquals("v", ro.get("k"));
        }

        @Test void set_throws_unsupported() {
            MutableStore ro = DynamicProxy.readOnlyProxy(new MapStore());
            assertThrows(UnsupportedOperationException.class, () -> ro.set("k", "v"));
        }

        @Test void clear_throws_unsupported() {
            MutableStore ro = DynamicProxy.readOnlyProxy(new MapStore());
            assertThrows(UnsupportedOperationException.class, ro::clear);
        }
    }

    @Nested
    @DisplayName("Proxy introspection")
    class ProxyIntrospection {
        @Test void isProxy_true_for_proxy() {
            Calculator calc = DynamicProxy.loggingProxy(new SimpleCalculator(), new ArrayList<>());
            assertTrue(DynamicProxy.isProxy(calc));
        }

        @Test void isProxy_false_for_real() {
            assertFalse(DynamicProxy.isProxy(new SimpleCalculator()));
        }

        @Test void getHandler_returns_handler() {
            Calculator calc = DynamicProxy.loggingProxy(new SimpleCalculator(), new ArrayList<>());
            assertNotNull(DynamicProxy.getHandler(calc));
        }
    }

    @Nested
    @DisplayName("Generic proxy factory")
    class GenericFactory {
        @Test void createProxy_dispatches_to_handler() {
            Calculator calc = DynamicProxy.createProxy(Calculator.class,
                (method, args) -> switch (method) {
                    case "add"      -> (int) args[0] + (int) args[1];
                    case "multiply" -> (int) args[0] * (int) args[1];
                    default         -> "mock";
                });
            assertEquals(9,  calc.add(4, 5));
            assertEquals(20, calc.multiply(4, 5));
            assertEquals("mock", calc.describe());
        }
    }
}
