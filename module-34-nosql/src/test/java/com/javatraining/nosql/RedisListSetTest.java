package com.javatraining.nosql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates Redis List, Set, and TTL commands.
 *
 * <h2>Redis data types covered here</h2>
 * <pre>
 *   List — ordered sequence; supports head/tail push and pop.
 *          Used for queues (LPUSH + RPOP), recent-activity feeds,
 *          job queues.
 *
 *   Set  — unordered collection of unique strings.
 *          Used for unique visitor tracking, tags, mutual-friend calculation,
 *          "user has seen this" deduplication.
 *
 *   TTL  — time-to-live on any key; Redis deletes the key automatically
 *          when it expires.  Used for session tokens, caches, rate-limit
 *          windows, OTP codes.
 * </pre>
 *
 * <p>Same container as {@link RedisStringHashTest} — Docker must be running.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RedisListSetTest {

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

    // ── List operations ───────────────────────────────────────────────────────

    @Test
    void list_left_push_and_range_shows_stack_order() {
        ListOperations<String, String> list = redis.opsForList();
        list.leftPush("stack", "first");
        list.leftPush("stack", "second");
        list.leftPush("stack", "third");

        // LRANGE 0 -1 returns all elements from head to tail
        List<String> all = list.range("stack", 0, -1);
        assertThat(all).containsExactly("third", "second", "first");
    }

    @Test
    void list_right_push_and_left_pop_implements_fifo_queue() {
        ListOperations<String, String> list = redis.opsForList();
        // Enqueue: RPUSH adds to tail
        list.rightPush("queue", "job-1");
        list.rightPush("queue", "job-2");
        list.rightPush("queue", "job-3");

        // Dequeue: LPOP removes from head (first-in, first-out)
        assertThat(list.leftPop("queue")).isEqualTo("job-1");
        assertThat(list.leftPop("queue")).isEqualTo("job-2");
        assertThat(list.size("queue")).isEqualTo(1L);
    }

    @Test
    void list_size_returns_number_of_elements() {
        ListOperations<String, String> list = redis.opsForList();
        list.rightPushAll("items", List.of("a", "b", "c", "d"));
        assertThat(list.size("items")).isEqualTo(4L);
    }

    @Test
    void list_range_slices_elements_by_index() {
        ListOperations<String, String> list = redis.opsForList();
        list.rightPushAll("letters", List.of("a", "b", "c", "d", "e"));

        // index 1 to 3 inclusive
        List<String> slice = list.range("letters", 1, 3);
        assertThat(slice).containsExactly("b", "c", "d");
    }

    // ── Set operations ────────────────────────────────────────────────────────

    @Test
    void set_add_stores_unique_values_only() {
        SetOperations<String, String> set = redis.opsForSet();
        set.add("tags", "java", "spring", "java", "redis"); // "java" added twice

        assertThat(set.size("tags")).isEqualTo(3L); // java, spring, redis
    }

    @Test
    void set_is_member_tests_presence() {
        SetOperations<String, String> set = redis.opsForSet();
        set.add("visited", "page-1", "page-2", "page-3");

        assertThat(set.isMember("visited", "page-2")).isTrue();
        assertThat(set.isMember("visited", "page-99")).isFalse();
    }

    @Test
    void set_members_returns_all_elements() {
        SetOperations<String, String> set = redis.opsForSet();
        set.add("colors", "red", "green", "blue");

        Set<String> members = set.members("colors");
        assertThat(members).containsExactlyInAnyOrder("red", "green", "blue");
    }

    // ── TTL / key expiry ──────────────────────────────────────────────────────

    @Test
    void expire_sets_ttl_and_key_is_deleted_after_expiry() throws InterruptedException {
        redis.opsForValue().set("temp-key", "data");
        redis.expire("temp-key", Duration.ofMillis(200));

        assertThat(redis.opsForValue().get("temp-key")).isEqualTo("data");
        Thread.sleep(300);
        assertThat(redis.opsForValue().get("temp-key")).isNull();
    }

    @Test
    void get_expire_returns_remaining_seconds() {
        redis.opsForValue().set("session", "token-abc");
        redis.expire("session", Duration.ofSeconds(60));

        Long ttl = redis.getExpire("session", TimeUnit.SECONDS);
        // TTL is between 1 and 60 (Redis may have ticked a second)
        assertThat(ttl).isBetween(1L, 60L);
    }
}
