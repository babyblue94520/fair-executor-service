package pers.clare.concurrent;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

class CustomExecutorServiceTests {

    @Nested
    @TestInstance(PER_CLASS)
    class Default extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>();
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class Fixed extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(Executors.newFixedThreadPool(keyCount / slowSpacing));
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class FixedBlocking extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(Executors.newFixedThreadPool(keyCount / slowSpacing - 1));
        }

        @Override
        protected boolean isBlocking() {
            return true;
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class Cached extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(Executors.newCachedThreadPool());
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class Scheduled extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(Executors.newScheduledThreadPool(keyCount / slowSpacing + 1));
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class ScheduledBlocking extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(Executors.newScheduledThreadPool(keyCount / slowSpacing - 1));
        }

        @Override
        protected boolean isBlocking() {
            return true;
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class WorkStealing extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(Executors.newWorkStealingPool(keyCount / slowSpacing + 1));
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class WorkStealingBlocking extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>(Executors.newWorkStealingPool(keyCount / slowSpacing - 1));
        }
        @Override
        protected boolean isBlocking() {
            return true;
        }
    }


    @Nested
    @TestInstance(PER_CLASS)
    class Throws extends AbstractIntegerFairExecutorServiceTest {
        @Override
        FairExecutorService<Integer> buildExecutorService() {
            return new FairExecutorService<>();
        }

        @Override
        protected void doSomething() {
            throw new RuntimeException("test");
        }
    }


}
