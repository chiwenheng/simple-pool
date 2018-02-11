package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.KeyAffinityExecutor.newSerializingExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phantomthief.pool.KeyAffinityExecutor;

/**
 * @author w.vela
 * Created on 2018-02-09.
 */
class KeyAffinityExecutorTest {

    private static final Logger logger = LoggerFactory.getLogger(KeyAffinityExecutorTest.class);
    private static final int LOOP = 1000;

    @Test
    void testClose() throws Exception {
        KeyAffinityExecutor<Integer> executor = newSerializingExecutor(10, "s-%d");
        executor.execute(1, () -> {});
        executor.close();
        assertThrows(RejectedExecutionException.class, () -> executor.execute(1, () -> {}));
    }

    @Test
    void test() throws Exception {
        Map<Integer, String> firstMapping = new ConcurrentHashMap<>();
        KeyAffinityExecutor<Integer> keyExecutor = newSerializingExecutor(50, 1000, "s-%d");
        for (int i = 0; i < 20; i++) {
            int j = i;
            keyExecutor.execute(j, () -> {
                firstMapping.put(j, currentThreadIdentity());
                sleepUninterruptibly(10, SECONDS);
            });
        }
        sleepUninterruptibly(1, SECONDS);
        AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < LOOP; i++) {
            int key = ThreadLocalRandom.current().nextInt(20);
            keyExecutor.execute(key, () -> {
                String firstV = firstMapping.get(key);
                assertEquals(firstV, currentThreadIdentity());
                counter.incrementAndGet();
            });
        }
        keyExecutor.close();
        logger.info("gathered threads:{}", firstMapping);
        assertEquals(LOOP, counter.get());
    }

    private String currentThreadIdentity() {
        Thread thread = Thread.currentThread();
        return thread.toString() + "/" + thread.hashCode();
    }
}