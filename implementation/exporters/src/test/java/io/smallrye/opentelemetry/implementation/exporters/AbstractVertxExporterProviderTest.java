package io.smallrye.opentelemetry.implementation.exporters;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.vertx.core.Vertx;

/**
 * Checks exporter providers share cached Vert.x instances and honor a supplied instance during lookup.
 */
class AbstractVertxExporterProviderTest {
    private final AbstractVertxExporterProvider provider = new TestExporterProvider();
    private Vertx suppliedVertx;

    /**
     * Clears the process-wide cache so each test starts with a cold lookup.
     *
     * @throws ReflectiveOperationException if the cache cannot be reset
     */
    @BeforeEach
    void resetVertxCache() throws ReflectiveOperationException {
        clearCachedVertx();
    }

    /**
     * Closes Vert.x instances created by a test and clears the shared cache.
     *
     * @throws Exception if an instance cannot be closed or the cache cannot be cleared
     */
    @AfterEach
    void closeVertxInstances() throws Exception {
        Vertx cached = clearCachedVertx();
        try {
            if (cached != null) {
                cached.close().toCompletionStage().toCompletableFuture().join();
            }
            if (suppliedVertx != null && suppliedVertx != cached) {
                suppliedVertx.close().toCompletionStage().toCompletableFuture().join();
            }
        } finally {
            suppliedVertx = null;
        }
    }

    /**
     * Verifies concurrent requests all receive the same cached Vert.x instance.
     *
     * @throws Exception if a concurrent request fails or times out
     */
    @Test
    void sharesOneLocallyCreatedVertxInstanceAcrossConcurrentCalls() throws Exception {
        int threadCount = 32;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Vertx>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return provider.getVertx(new TestConfigProperties());
                }));
            }
            start.countDown();

            Vertx created = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<Vertx> future : futures) {
                assertSame(created, future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            stopExecutor(executor, start);
        }
    }

    /**
     * Verifies a server-provided instance wins when it is registered after lookup has started.
     *
     * @throws Exception if the lookup fails, times out, or is interrupted
     */
    @Test
    void usesVertxSuppliedWhileLookupIsInProgress() throws Exception {
        suppliedVertx = Vertx.vertx();
        CountDownLatch configLookupStarted = new CountDownLatch(1);
        CountDownLatch continueLookup = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Vertx> lookup = executor.submit(
                    () -> provider.getVertx(new TestConfigProperties(configLookupStarted, continueLookup)));
            assertTrue(configLookupStarted.await(10, TimeUnit.SECONDS), "getVertx should reach config lookup");

            AbstractVertxExporterProvider.withVertx(suppliedVertx);
            continueLookup.countDown();

            assertSame(suppliedVertx, lookup.get(10, TimeUnit.SECONDS));
        } finally {
            stopExecutor(executor, continueLookup);
        }
    }

    /**
     * Clears the private process-wide cache so each test can exercise a cold lookup.
     *
     * @throws ReflectiveOperationException if the cache field cannot be reset
     */
    private static Vertx clearCachedVertx() throws ReflectiveOperationException {
        Field vertxField = AbstractVertxExporterProvider.class.getDeclaredField("vertx");
        vertxField.setAccessible(true);
        synchronized (AbstractVertxExporterProvider.class) {
            Vertx cached = (Vertx) vertxField.get(null);
            vertxField.set(null, null);
            return cached;
        }
    }

    /**
     * Releases the latch and waits for the executor to finish before test cleanup.
     *
     * @param executor the test executor
     * @param release the latch blocking the test task
     * @throws InterruptedException if waiting for termination is interrupted
     */
    private static void stopExecutor(ExecutorService executor, CountDownLatch release) throws InterruptedException {
        release.countDown();
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "test executor should terminate");
    }

    /**
     * Supplies unset configuration values and can pause string lookup for race tests.
     */
    private static final class TestConfigProperties implements ConfigProperties {
        private final CountDownLatch lookupStarted;
        private final CountDownLatch continueLookup;

        /** Creates an unblocked configuration stub for the cold-cache test. */
        private TestConfigProperties() {
            this(null, null);
        }

        /** Creates a configuration stub that pauses string lookup on the supplied latches. */
        private TestConfigProperties(CountDownLatch lookupStarted, CountDownLatch continueLookup) {
            this.lookupStarted = lookupStarted;
            this.continueLookup = continueLookup;
        }

        /** Returns no configured string value, optionally blocking to coordinate the race. */
        @Override
        public String getString(String key) {
            if (lookupStarted != null) {
                lookupStarted.countDown();
                try {
                    if (!continueLookup.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to continue Vertx lookup");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to continue Vertx lookup", e);
                }
            }
            return null;
        }

        /** Returns no configured boolean value. */
        @Override
        public Boolean getBoolean(String key) {
            return null;
        }

        /** Returns no configured integer value. */
        @Override
        public Integer getInt(String key) {
            return null;
        }

        /** Returns no configured long value. */
        @Override
        public Long getLong(String key) {
            return null;
        }

        /** Returns no configured double value. */
        @Override
        public Double getDouble(String key) {
            return null;
        }

        /** Returns no configured duration. */
        @Override
        public Duration getDuration(String key) {
            return null;
        }

        /** Returns no configured list. */
        @Override
        public List<String> getList(String key) {
            return null;
        }

        /** Returns no configured map. */
        @Override
        public Map<String, String> getMap(String key) {
            return null;
        }
    }

    /**
     * Exposes the base exporter provider for the package-level cache tests.
     */
    private static final class TestExporterProvider extends AbstractVertxExporterProvider {
        /** Creates a provider configured for trace exporters. */
        private TestExporterProvider() {
            super("traces", "test");
        }
    }
}
