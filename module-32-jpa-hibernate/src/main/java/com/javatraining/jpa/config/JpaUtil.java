package com.javatraining.jpa.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for creating EntityManagerFactories and running transactions.
 *
 * <h3>Why a helper?</h3>
 * In production Spring applications, {@code @Transactional} methods are wrapped
 * by a Spring AOP proxy that handles {@code tx.begin()}, {@code tx.commit()},
 * and {@code tx.rollback()}.  In this plain JPA module we replicate that pattern
 * manually using {@link #inTransaction} so every test has the same clean
 * begin/commit/rollback structure.
 *
 * <h3>Per-class database isolation</h3>
 * {@link #createEmf(String)} overrides {@code jakarta.persistence.jdbc.url} to
 * use a distinct H2 in-memory database name for each test class.  This prevents
 * cross-class schema conflicts and lets each class use {@code create-drop} DDL
 * for a pristine schema.
 */
public final class JpaUtil {

    private JpaUtil() {}

    /**
     * Creates an {@link EntityManagerFactory} connected to an H2 in-memory
     * database whose name is {@code dbName}.  All other settings come from
     * {@code persistence.xml} (dialect, DDL auto, statistics, etc.).
     *
     * @param dbName unique H2 database name, e.g. {@code "jpa_lifecycle"}
     */
    public static EntityManagerFactory createEmf(String dbName) {
        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url",
                  "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        return Persistence.createEntityManagerFactory("jpatraining", props);
    }

    /**
     * Executes {@code work} inside a transaction.  Commits on success,
     * rolls back on any exception, and re-throws the original exception.
     *
     * @param <T>  return type of the work function
     * @return whatever {@code work} returns
     */
    public static <T> T inTransaction(EntityManager em, Function<EntityManager, T> work) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        }
    }

    /** Overload for work that returns nothing (void). */
    public static void inTransaction(EntityManager em, Consumer<EntityManager> work) {
        inTransaction(em, e -> { work.accept(e); return null; });
    }
}
