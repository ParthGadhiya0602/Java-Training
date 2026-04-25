package com.javatraining.springcore.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demonstrates all five AOP advice types.
 *
 * <p>Pointcut expression anatomy:
 * <pre>
 *   execution( [modifier] returnType [declaring-type] method-name(params) [throws] )
 *
 *   execution(* com.javatraining.springcore.service.*.*(..))
 *     *     - any return type
 *     com…service.*  - any class in the service package
 *     .*  - any method name
 *     (..) - any number of parameters
 * </pre>
 *
 * <p>Advice execution order for a method call (no exception):
 * <pre>
 *   @Around (before proceed())
 *     @Before
 *       → target method executes
 *     @AfterReturning
 *   @After (always)
 *   @Around (after proceed())
 * </pre>
 *
 * <p>Advice execution order when the method throws:
 * <pre>
 *   @Around (before proceed())
 *     @Before
 *       → target method throws
 *     @AfterThrowing
 *   @After (always)
 *   @Around rethrows (or swallows)
 * </pre>
 */
@Aspect
@Component
public class LoggingAspect {

    // Recorded calls - inspected in tests
    private final List<String> log = new ArrayList<>();

    // ── Pointcut declarations ─────────────────────────────────────────────────

    /** All methods in the service package */
    @Pointcut("execution(* com.javatraining.springcore.service.*.*(..))")
    public void serviceLayer() {}

    /** Only UserService methods */
    @Pointcut("execution(* com.javatraining.springcore.service.UserService.*(..))")
    public void userServiceMethods() {}

    // ── Advice ───────────────────────────────────────────────────────────────

    /**
     * @Before - runs before the method.
     * Cannot stop the method from executing (use @Around for that).
     */
    @Before("userServiceMethods()")
    public void logBefore(JoinPoint jp) {
        log.add("BEFORE " + jp.getSignature().getName());
    }

    /**
     * @AfterReturning - runs after successful return.
     * The 'returning' binding captures the actual return value.
     */
    @AfterReturning(pointcut = "userServiceMethods()", returning = "result")
    public void logAfterReturning(JoinPoint jp, Object result) {
        log.add("AFTER_RETURNING " + jp.getSignature().getName() + " → " + result);
    }

    /**
     * @AfterThrowing - runs when the method throws an exception.
     * The 'throwing' binding captures the actual exception.
     * Does NOT suppress the exception - it still propagates.
     */
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logAfterThrowing(JoinPoint jp, Throwable ex) {
        log.add("AFTER_THROWING " + jp.getSignature().getName() + " - " + ex.getMessage());
    }

    /**
     * @After - runs after the method regardless of outcome (like finally).
     * Use for cleanup that must always happen.
     */
    @After("userServiceMethods()")
    public void logAfter(JoinPoint jp) {
        log.add("AFTER " + jp.getSignature().getName());
    }

    /**
     * @Around - wraps the entire method invocation.
     * {@code pjp.proceed()} delegates to the actual method.
     * Can modify args, modify the return value, or suppress the exception.
     *
     * <p>@Around is the most powerful advice - use it for timing, transactions,
     * retries, or security checks where you need full control.
     */
    @Around("execution(* com.javatraining.springcore.service.ReportService.*(..))")
    public Object timeAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        log.add("AROUND_BEFORE " + pjp.getSignature().getName());
        try {
            Object result = pjp.proceed();
            long elapsed = System.nanoTime() - start;
            log.add("AROUND_AFTER " + pjp.getSignature().getName() + " [" + elapsed + "ns]");
            return result;
        } catch (Throwable t) {
            log.add("AROUND_THROW " + pjp.getSignature().getName());
            throw t;
        }
    }

    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }

    public void clearLog() {
        log.clear();
    }
}
