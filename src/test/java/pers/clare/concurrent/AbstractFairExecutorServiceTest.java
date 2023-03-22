package pers.clare.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
            timeMap.put(id, new AtomicLong());
        }
        for (int i = 0; i < max; i++) {
            final int id = toId(i);
            consumer.accept(toKey(id), () -> {
                timeMap.get(id).addAndGet(System.currentTimeMillis() - startTime);
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

        System.out.printf("key count: %d, task: %d, total task: %d\n", keyCount, taskCount, max);
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
        executorService.shutdown();
    }

    @Test
    void keySubmitRunnableResult() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test((key, runnable) -> executorService.submit(key, runnable, null));
        executorService.shutdown();
    }

    @Test
    void keySubmitCallable() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test((key, runnable) -> executorService.submit(key, () -> {
            runnable.run();
            return null;
        }));
        executorService.shutdown();
    }

    @Test
    void keyExecute() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test(executorService::execute);
        executorService.shutdown();
    }

    @Test
    void submitRunnable() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test2((key, runnable) -> executorService.submit(runnable));
        executorService.shutdown();
    }

    @Test
    void submitRunnableResult() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test2((key, runnable) -> executorService.submit(runnable, null));
        executorService.shutdown();
    }

    @Test
    void submitCallable() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test2((key, runnable) -> executorService.submit(() -> {
            runnable.run();
            return null;
        }));
        executorService.shutdown();
    }

    @Test
    void execute() {
        FairExecutorService<Key> executorService = buildExecutorService();
        test2((key, runnable) -> executorService.execute(runnable));
        executorService.shutdown();
    }

    @Test
    void clear() throws InterruptedException {
        FairExecutorService<Key> executorService = buildExecutorService();
        int max = 1000;
        int keyCount = 10;
        Map<Key, AtomicInteger> countMap = new ConcurrentHashMap<>();
        for (int i = 0; i < max; i++) {
            int id = i % keyCount;
            Key key = toKey(id);
            countMap.put(key, new AtomicInteger());
        }
        for (int i = 0; i < max; i++) {
            int id = i % keyCount;
            Key key = toKey(id);
            executorService.execute(key, () -> {
                countMap.get(key).incrementAndGet();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Thread.sleep(5);
        Map<Key, Integer> unexecutedMap = new ConcurrentHashMap<>();
        for (Map.Entry<Key, AtomicInteger> entry : countMap.entrySet()) {
            unexecutedMap.put(entry.getKey(), executorService.reset(entry.getKey()).length);
        }
        Thread.sleep(5);
        executorService.shutdown();
        if (executorService.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            for (Map.Entry<Key, AtomicInteger> entry : countMap.entrySet()) {
                int executed = entry.getValue().get();
                int unexecuted = Objects.requireNonNullElse(unexecutedMap.get(entry.getKey()), 0);
                int total = max / keyCount;
                System.out.printf("executed: %d, unexecuted: %d, total: %d\n", executed, unexecuted, total);
                assertEquals(executed + unexecuted, max / keyCount);
            }
        }
    }
}
