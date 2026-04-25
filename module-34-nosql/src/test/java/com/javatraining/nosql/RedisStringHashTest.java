package com.javatraining.nosql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates Redis String and Hash commands via {@link StringRedisTemplate}.
 *
 * <p><b>Infrastructure:</b> Testcontainers starts a real Redis instance in Docker.
 * Requires Docker to be running.  {@code @DynamicPropertySource} feeds the mapped
 * port into {@code spring.data.redis.*} before the application context is created.
 *
 * <h2>Redis data types covered here</h2>
 * <pre>
 *   String - the simplest type: one key → one value.
 *            Used for counters, session tokens, rate-limiting, cached pages.
 *
 *   Hash   - one key → a map of fields → values.
 *            Used for user profiles, session data, structured records.
 * </pre>
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RedisStringHashTest {

    @Container
    static final GenericContainer<?> REDIS =
        new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired StringRedisTemplate redis;

    @AfterEach
    void clean() {
        Set<String> keys = redis.keys("*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    // ── String operations ─────────────────────────────────────────────────────

    @Test
    void string_set_and_get_round_trip() {
        ValueOperations<String, String> values = redis.opsForValue();
        values.set("greeting", "hello");
        assertThat(values.get("greeting")).isEqualTo("hello");
    }

    @Test
    void string_overwrite_replaces_previous_value() {
        ValueOperations<String, String> values = redis.opsForValue();
        values.set("key", "first");
        values.set("key", "second");
        assertThat(values.get("key")).isEqualTo("second");
    }

    @Test
    void string_set_with_expiry_key_disappears_after_ttl() throws InterruptedException {
        ValueOperations<String, String> values = redis.opsForValue();
        values.set("ephemeral", "data", Duration.ofMillis(200));

        assertThat(values.get("ephemeral")).isEqualTo("data");
        Thread.sleep(300);
        assertThat(values.get("ephemeral")).isNull();
    }

    @Test
    void string_increment_treats_value_as_counter() {
        ValueOperations<String, String> values = redis.opsForValue();
        values.set("counter", "10");

        redis.opsForValue().increment("counter");
        redis.opsForValue().increment("counter");
        redis.opsForValue().increment("counter");

        assertThat(values.get("counter")).isEqualTo("13");
    }

    @Test
    void string_key_does_not_exist_get_returns_null() {
        assertThat(redis.opsForValue().get("no-such-key")).isNull();
    }

    // ── Hash operations ───────────────────────────────────────────────────────

    @Test
    void hash_put_and_get_individual_field() {
        HashOperations<String, String, String> hash = redis.opsForHash();
        hash.put("user:1", "name", "Alice");
        hash.put("user:1", "email", "alice@example.com");
        hash.put("user:1", "role", "admin");

        assertThat(hash.get("user:1", "name")).isEqualTo("Alice");
        assertThat(hash.get("user:1", "email")).isEqualTo("alice@example.com");
    }

    @Test
    void hash_entries_returns_all_fields_as_map() {
        HashOperations<String, String, String> hash = redis.opsForHash();
        hash.putAll("profile:42", Map.of(
            "firstName", "Bob",
            "lastName",  "Smith",
            "age",       "30"
        ));

        Map<String, String> profile = hash.entries("profile:42");
        assertThat(profile).containsEntry("firstName", "Bob")
                           .containsEntry("lastName",  "Smith")
                           .containsEntry("age",       "30");
    }

    @Test
    void hash_delete_removes_specific_field() {
        HashOperations<String, String, String> hash = redis.opsForHash();
        hash.putAll("session:99", Map.of("token", "abc123", "userId", "7"));

        hash.delete("session:99", "token");

        assertThat(hash.hasKey("session:99", "token")).isFalse();
        assertThat(hash.hasKey("session:99", "userId")).isTrue();
    }
}
