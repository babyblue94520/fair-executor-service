package pers.clare.concurrent;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

class ConcurrentTests {

    @Nested
    @TestInstance(PER_CLASS)
    class Concurrent2 extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(2);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class Concurrent3 extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(3);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class Concurrent5 extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(5);
        }
    }
}
