package com.javatraining.springcore;

import com.javatraining.springcore.scope.PrototypeTask;
import com.javatraining.springcore.scope.SingletonCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies singleton vs prototype scoping behaviour.
 *
 * <p>ObjectProvider is the correct way to retrieve prototype beans inside
 * a singleton bean — direct @Autowired in a singleton only injects once,
 * defeating the purpose of prototype scope.
 */
@SpringBootTest
class BeanScopeTest {

    @Autowired
    SingletonCounter counterA;

    @Autowired
    SingletonCounter counterB;

    @Autowired
    ApplicationContext context;

    @Autowired
    ObjectProvider<PrototypeTask> taskProvider;

    @BeforeEach
    void reset() {
        PrototypeTask.resetCounter();
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    @Test
    void singleton_two_injections_are_same_instance() {
        assertThat(counterA).isSameAs(counterB);
    }

    @Test
    void singleton_state_is_shared() {
        int before = counterA.getCount();
        counterA.increment();
        // counterB IS counterA — same object
        assertThat(counterB.getCount()).isEqualTo(before + 1);
    }

    // ── Prototype ─────────────────────────────────────────────────────────────

    @Test
    void prototype_each_getBean_returns_new_instance() {
        PrototypeTask t1 = context.getBean(PrototypeTask.class);
        PrototypeTask t2 = context.getBean(PrototypeTask.class);
        assertThat(t1).isNotSameAs(t2);
        assertThat(t1.getInstanceId()).isNotEqualTo(t2.getInstanceId());
    }

    @Test
    void prototype_state_is_isolated() {
        PrototypeTask t1 = taskProvider.getObject();
        PrototypeTask t2 = taskProvider.getObject();
        t1.execute();
        // t2 is a separate instance — its state is unaffected
        assertThat(t1.getStatus()).isEqualTo("DONE");
        assertThat(t2.getStatus()).isEqualTo("NEW");
    }

    @Test
    void prototype_object_provider_returns_new_instance_each_time() {
        PrototypeTask first  = taskProvider.getObject();
        PrototypeTask second = taskProvider.getObject();
        assertThat(first).isNotSameAs(second);
    }
}
