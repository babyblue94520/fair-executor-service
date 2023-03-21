package pers.clare.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SuppressWarnings({"StatementWithEmptyBody"})
public abstract class AbstractFairExecutorServiceTest<Key> {

    abstract FairExecutorService<Key> buildExecutorService();

    protected final int slowSpacing;

    protected final int keyCount;

    protected final int taskCount;
    protected final long slowRunTime;

    protected AbstractFairExecutorServiceTest() {
        this(5, 20, 100, 2);
    }

    public AbstractFairExecutorServiceTest(int slowSpacing, int keyCount, int taskCount, long slowRunTime) {
        this.slowSpacing = slowSpacing;
        this.keyCount = keyCount;
        this.taskCount = taskCount;
        this.slowRunTime = slowRunTime;
    }

    abstract protected Key toKey(int id);

    void test(BiConsumer<Key, Runnable> consumer) {
        test(consumer, false);
    }

    void test2(BiConsumer<Key, Runnable> consumer) {
        test(consumer, true);
    }

    void test(BiConsumer<Key, Runnable> consumer, boolean nullKey) {
        AtomicInteger count = new AtomicInteger();
        Map<Integer, AtomicLong> timeMap = new ConcurrentHashMap<>();
        long startTime = System.currentTimeMillis();
        int max = keyCount * taskCount;
        for (int i = 0; i < max; i++) {
            final int id = toId(i);
            consumer.accept(toKey(id), () -> {
                timeMap.computeIfAbsent(id, (index) -> new AtomicLong()).addAndGet(System.currentTimeMillis() - startTime);
                if (isSlow(id)) {
                    long waitingTime = System.currentTimeMillis() + slowRunTime;
                    while (System.currentTimeMillis() < waitingTime) {
                    }
                }
                count.incrementAndGet();
                doSomething();
            });
        }

        long waitingTime = System.currentTimeMillis() + 10000;
        while (waitingTime > System.currentTimeMillis() && count.get() < max) {
        }

        assertEquals(max, count.get());

        System.out.printf("key count: %d, task: %d, total task: %d\n", keyCount, taskCount , max);
        System.out.printf("slow key count: %d, run time: %d ms\n", keyCount / slowSpacing, slowRunTime);
        System.out.printf("nullKey: %b, blocking: %b\n", nullKey, isBlocking());
        long slowMaximumTime = 0;
        long fastMinimumTime = Long.MAX_VALUE;
        for (Map.Entry<Integer, AtomicLong> entry : timeMap.entrySet()) {
            int id = entry.getKey();
            long value = entry.getValue().get();
            boolean slow = isSlow(id);
            System.out.printf("[%s] key: %s total waiting time: %d \n", slow ? "slow" : "    ", toKey(id), value);
            if (slow) {
                if (value > slowMaximumTime) {
                    slowMaximumTime = value;
                }
            } else {
                if (value < fastMinimumTime) {
                    fastMinimumTime = value;
                }
            }
        }
        if (nullKey || isBlocking()) {
//            assertTrue(slowMaximumTime < fastMinimumTime );
        } else {
            assertTrue(fastMinimumTime < slowMaximumTime);
        }
    }

    protected void doSomething() {

    }

    protected int toId(int index) {
        return index % keyCount;
    }

    protected boolean isSlow(int id) {
        return id % slowSpacing == 0;
    }

    protected boolean isBlocking() {
        return false;
    }

    @Test
    void keySubmitRunnable() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test(executorService::submit);
        executorService.getExecutorService().shutdown();
    }

    @Test
    void keySubmitRunnableResult() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test((key, runnable) -> executorService.submit(key, runnable, null));
        executorService.getExecutorService().shutdown();
    }

    @Test
    void keySubmitCallable() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test((key, runnable) -> executorService.submit(key, () -> {
            runnable.run();
            return null;
        }));
        executorService.getExecutorService().shutdown();
    }

    @Test
    void keyExecute() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test(executorService::execute);
        executorService.getExecutorService().shutdown();
    }

    @Test
    void submitRunnable() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test2((key, runnable) -> executorService.submit(runnable));
        executorService.getExecutorService().shutdown();
    }

    @Test
    void submitRunnableResult() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test2((key, runnable) -> executorService.submit(runnable, null));
        executorService.getExecutorService().shutdown();
    }

    @Test
    void submitCallable() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test2((key, runnable) -> executorService.submit(() -> {
            runnable.run();
            return null;
        }));
        executorService.getExecutorService().shutdown();
    }

    @Test
    void execute() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test2((key, runnable) -> executorService.execute(runnable));
        executorService.getExecutorService().shutdown();
    }
}
