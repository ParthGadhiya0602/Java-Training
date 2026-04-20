package com.javatraining.jpa;

import com.javatraining.jpa.config.JpaUtil;
import com.javatraining.jpa.entity.Author;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the JPA entity lifecycle and first-level (L1) cache.
 *
 * <p>Entity states:
 * <pre>
 *   NEW/TRANSIENT  — created with {@code new}, unknown to any EntityManager
 *   MANAGED        — tracked by an open EntityManager; changes auto-synced on flush
 *   DETACHED       — was managed; EntityManager closed or {@code em.detach()} called
 *   REMOVED        — {@code em.remove()} called; will be DELETE'd on next flush
 * </pre>
 *
 * <p>L1 cache (persistence context):
 * Within a single EntityManager, {@code em.find()} for the same id always returns
 * the identical Java object — the second call hits the in-memory cache, not the DB.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityLifecycleTest {

    private EntityManagerFactory emf;
    private Statistics            stats;
    private EntityManager         em;

    @BeforeAll
    void setUpEmf() {
        emf   = JpaUtil.createEmf("jpa_lifecycle");
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
    }

    @AfterAll
    void tearDownEmf() {
        emf.close();
    }

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        JpaUtil.inTransaction(em, e -> {
            e.createNativeQuery("DELETE FROM book_tags").executeUpdate();
            e.createNativeQuery("DELETE FROM books").executeUpdate();
            e.createNativeQuery("DELETE FROM authors").executeUpdate();
            e.createNativeQuery("DELETE FROM tags").executeUpdate();
            e.createNativeQuery("DELETE FROM book_details").executeUpdate();
        });
    }

    @AfterEach
    void tearDownEm() {
        if (em.isOpen()) em.close();
    }

    // ── NEW / MANAGED ────────────────────────────────────────────────────────

    @Test
    void new_entity_has_null_id_before_persist() {
        Author author = new Author("Alice", "alice@test.com");
        assertNull(author.getId(), "Id not assigned until persist");
    }

    @Test
    void persist_assigns_generated_id() {
        JpaUtil.inTransaction(em, e -> {
            Author author = new Author("Alice", "alice@test.com");
            e.persist(author);                   // NEW → MANAGED; id assigned after flush
            assertNotNull(author.getId());
            assertTrue(author.getId() > 0);
        });
    }

    @Test
    void entity_is_managed_after_persist_and_before_commit() {
        JpaUtil.inTransaction(em, e -> {
            Author author = new Author("Bob", "bob@test.com");
            e.persist(author);
            assertTrue(e.contains(author), "Entity is MANAGED after persist");
        });
    }

    // ── L1 cache ────────────────────────────────────────────────────────────

    @Test
    void find_returns_same_java_instance_from_l1_cache() {
        Long id = JpaUtil.inTransaction(em, e -> {
            Author a = new Author("Carol", "carol@test.com");
            e.persist(a);
            return a.getId();
        });
        em.clear(); // evict L1 cache so next find hits DB

        Author a1 = em.find(Author.class, id); // DB hit → stored in L1 cache
        Author a2 = em.find(Author.class, id); // L1 cache hit → same Java object

        assertSame(a1, a2, "L1 cache: both finds return the identical Java reference");
    }

    @Test
    void l1_cache_means_only_one_db_query_for_two_finds() {
        Long id = JpaUtil.inTransaction(em, e -> {
            Author a = new Author("Dave", "dave@test.com");
            e.persist(a);
            return a.getId();
        });
        em.clear();
        stats.clear();

        em.find(Author.class, id); // DB hit: 1 prepared statement
        em.find(Author.class, id); // L1 hit: 0 additional statements

        assertEquals(1L, stats.getPrepareStatementCount(),
                "Second find() served from L1 cache — only 1 DB query total");
    }

    // ── Dirty checking ───────────────────────────────────────────────────────

    /**
     * Dirty checking is automatic: Hibernate compares the entity's current
     * field values against the snapshot taken when it was loaded/persisted.
     * At commit/flush, changed fields produce an UPDATE — no explicit merge needed.
     */
    @Test
    void dirty_checking_generates_update_without_explicit_persist_or_merge() {
        Long id = JpaUtil.inTransaction(em, e -> {
            Author a = new Author("Eve Original", "eve@test.com");
            e.persist(a);
            return a.getId();
        });

        // Transaction 2: load, modify — no em.persist() / em.merge() called
        JpaUtil.inTransaction(em, e -> {
            Author a = e.find(Author.class, id); // MANAGED
            a.setName("Eve Updated");            // dirty — Hibernate will flush an UPDATE
            // commit automatically flushes changes
        });

        em.clear(); // evict to force DB read
        Author found = em.find(Author.class, id);
        assertEquals("Eve Updated", found.getName(), "Dirty check generated the UPDATE");
    }

    // ── DETACHED ─────────────────────────────────────────────────────────────

    @Test
    void detached_entity_changes_are_lost_without_merge() {
        Long id = JpaUtil.inTransaction(em, e -> {
            Author a = new Author("Frank Stable", "frank@test.com");
            e.persist(a);
            return a.getId();
        });

        Author author = em.find(Author.class, id);
        em.detach(author);                          // MANAGED → DETACHED

        author.setName("Frank Modified");           // change on detached object
        // No em.merge() — change is NOT propagated

        em.clear();
        Author inDb = em.find(Author.class, id);
        assertEquals("Frank Stable", inDb.getName(),
                "Detached entity changes are not persisted without merge");
    }

    @Test
    void merge_reattaches_detached_entity_and_propagates_changes() {
        Long id = JpaUtil.inTransaction(em, e -> {
            Author a = new Author("Grace Original", "grace@test.com");
            e.persist(a);
            return a.getId();
        });

        Author detached = em.find(Author.class, id);
        em.detach(detached);
        detached.setName("Grace Merged");

        // merge() returns a NEW managed copy; original reference stays detached
        JpaUtil.inTransaction(em, e -> { e.merge(detached); });

        em.clear();
        assertEquals("Grace Merged", em.find(Author.class, id).getName());
    }

    // ── REMOVED ──────────────────────────────────────────────────────────────

    @Test
    void remove_deletes_entity_from_database() {
        Long id = JpaUtil.inTransaction(em, e -> {
            Author a = new Author("Henry", "henry@test.com");
            e.persist(a);
            return a.getId();
        });

        JpaUtil.inTransaction(em, e -> {
            Author a = e.find(Author.class, id);
            e.remove(a); // MANAGED → REMOVED; DELETE on commit
        });

        em.clear();
        assertNull(em.find(Author.class, id), "Entity gone from DB after remove");
    }
}
