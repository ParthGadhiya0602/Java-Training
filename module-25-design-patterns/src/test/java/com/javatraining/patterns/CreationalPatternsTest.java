package com.javatraining.patterns;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CreationalPatternsTest {

    // ── Singleton ──────────────────────────────────────────────────────────────

    @Test
    void singleton_same_instance() {
        CreationalPatterns.AppConfig a = CreationalPatterns.AppConfig.getInstance();
        CreationalPatterns.AppConfig b = CreationalPatterns.AppConfig.getInstance();
        assertSame(a, b);
    }

    @Test
    void singleton_default_settings() {
        CreationalPatterns.AppConfig cfg = CreationalPatterns.AppConfig.getInstance();
        assertEquals("30", cfg.get("timeout"));
        assertEquals("3",  cfg.get("retries"));
    }

    @Test
    void singleton_set_and_get() {
        CreationalPatterns.AppConfig cfg = CreationalPatterns.AppConfig.getInstance();
        cfg.set("env", "test");
        assertEquals("test", cfg.get("env"));
    }

    // ── Factory Method ────────────────────────────────────────────────────────

    @Test
    void factory_method_email_service() {
        CreationalPatterns.NotificationService svc = new CreationalPatterns.EmailService();
        CreationalPatterns.Notification n = svc.createNotification();  // via subclass
        assertEquals("EMAIL", n.type());
    }

    @Test
    void factory_method_sms_service() {
        CreationalPatterns.NotificationService svc = new CreationalPatterns.SmsService();
        assertEquals("SMS", svc.createNotification().type());
    }

    @Test
    void static_factory_all_channels() {
        assertEquals("EMAIL", CreationalPatterns.notificationFor("email").type());
        assertEquals("SMS",   CreationalPatterns.notificationFor("SMS").type());
        assertEquals("PUSH",  CreationalPatterns.notificationFor("PUSH").type());
    }

    @Test
    void static_factory_unknown_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> CreationalPatterns.notificationFor("CARRIER_PIGEON"));
    }

    // ── Abstract Factory ──────────────────────────────────────────────────────

    @Test
    void abstract_factory_light_theme() {
        List<String> rendered = CreationalPatterns.renderUI(new CreationalPatterns.LightThemeFactory());
        assertEquals(List.of("LightButton", "LightCheckbox"), rendered);
    }

    @Test
    void abstract_factory_dark_theme() {
        List<String> rendered = CreationalPatterns.renderUI(new CreationalPatterns.DarkThemeFactory());
        assertEquals(List.of("DarkButton", "DarkCheckbox"), rendered);
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    @Test
    void builder_required_fields() {
        CreationalPatterns.HttpRequest req = CreationalPatterns.HttpRequest
            .newBuilder("GET", "http://example.com")
            .build();
        assertEquals("GET", req.method());
        assertEquals("http://example.com", req.url());
    }

    @Test
    void builder_optional_defaults() {
        CreationalPatterns.HttpRequest req = CreationalPatterns.HttpRequest
            .newBuilder("GET", "http://example.com")
            .build();
        assertEquals(5000, req.timeoutMs());
        assertTrue(req.followRedirects());
        assertNull(req.body());
        assertTrue(req.headers().isEmpty());
    }

    @Test
    void builder_all_fields() {
        CreationalPatterns.HttpRequest req = CreationalPatterns.HttpRequest
            .newBuilder("POST", "http://api.example.com/data")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .body("{\"key\":\"value\"}")
            .timeoutMs(10_000)
            .followRedirects(false)
            .build();

        assertEquals("POST", req.method());
        assertEquals("{\"key\":\"value\"}", req.body());
        assertEquals(10_000, req.timeoutMs());
        assertFalse(req.followRedirects());
        assertEquals("application/json", req.headers().get("Content-Type"));
        assertEquals(2, req.headers().size());
    }

    @Test
    void builder_null_method_throws() {
        assertThrows(NullPointerException.class,
            () -> CreationalPatterns.HttpRequest.newBuilder(null, "http://x.com").build());
    }

    @Test
    void builder_produces_immutable_headers() {
        CreationalPatterns.HttpRequest req = CreationalPatterns.HttpRequest
            .newBuilder("GET", "http://x.com")
            .header("X-Foo", "bar")
            .build();
        assertThrows(UnsupportedOperationException.class,
            () -> req.headers().put("X-New", "value"));
    }

    // ── Prototype ─────────────────────────────────────────────────────────────

    @Test
    void prototype_copy_is_independent() {
        CreationalPatterns.DocumentTemplate original = new CreationalPatterns.DocumentTemplate(
            "Report", "Header", "Footer");
        original.addSection("Section 1");

        CreationalPatterns.DocumentTemplate copy = original.copy();
        copy.addSection("Section 2");

        assertEquals(1, original.sections().size(), "original unchanged");
        assertEquals(2, copy.sections().size());
    }

    @Test
    void prototype_with_title() {
        CreationalPatterns.DocumentTemplate t = new CreationalPatterns.DocumentTemplate("Base", "H", "F");
        CreationalPatterns.DocumentTemplate renamed = t.withTitle("Custom");
        assertEquals("Base",   t.title());
        assertEquals("Custom", renamed.title());
    }

    @Test
    void prototype_shares_header_footer() {
        CreationalPatterns.DocumentTemplate t = new CreationalPatterns.DocumentTemplate("Doc", "MyHeader", "MyFooter");
        CreationalPatterns.DocumentTemplate copy = t.copy();
        assertEquals("MyHeader", copy.header());
        assertEquals("MyFooter", copy.footer());
    }

    // ── Object Pool ───────────────────────────────────────────────────────────

    @Test
    void pool_acquire_and_release() {
        CreationalPatterns.ObjectPool<StringBuilder> pool =
            new CreationalPatterns.ObjectPool<>(StringBuilder::new, 3);

        StringBuilder sb = pool.acquire();
        assertNotNull(sb);
        assertEquals(1, pool.inUseCount());
        assertEquals(0, pool.availableCount());

        pool.release(sb);
        assertEquals(0, pool.inUseCount());
        assertEquals(1, pool.availableCount());
    }

    @Test
    void pool_reuses_released_objects() {
        CreationalPatterns.ObjectPool<StringBuilder> pool =
            new CreationalPatterns.ObjectPool<>(StringBuilder::new, 3);

        StringBuilder first = pool.acquire();
        pool.release(first);
        StringBuilder second = pool.acquire();
        assertSame(first, second, "released object should be reused");
    }

    @Test
    void pool_exhausted_throws() {
        CreationalPatterns.ObjectPool<Object> pool =
            new CreationalPatterns.ObjectPool<>(Object::new, 2);
        pool.acquire();
        pool.acquire();
        assertThrows(IllegalStateException.class, pool::acquire);
    }

    @Test
    void pool_tracks_total_size() {
        CreationalPatterns.ObjectPool<Object> pool =
            new CreationalPatterns.ObjectPool<>(Object::new, 5);
        pool.acquire();
        pool.acquire();
        assertEquals(2, pool.totalSize());
    }
}
