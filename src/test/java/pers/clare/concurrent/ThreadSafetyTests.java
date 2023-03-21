package pers.clare.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class ThreadSafetyTests {

    @Test
    void fair() {
        FairExecutorService<Integer> consumers = new FairExecutorService<>(2);
        for (int i = 0; i < 20; i++) {
            run((id, runnable) -> consumers.submit(id, runnable, null), consumers::size);
        }
        consumers.getExecutorService().shutdownNow();
    }

    @Test
    void normal() {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        ExecutorService consumers = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, queue);
        try{
            for (int i = 0; i < 20; i++) {
                run((id, runnable) -> consumers.submit(runnable, null), (id) -> queue.size());
            }
        }finally {
            consumers.shutdownNow();
        }
    }

    void run(BiConsumer<Integer,Runnable> consumer, Function<Integer, Integer> size) {
        int producerCount = 100;
        int userCount = 50;
        int count = 100;
        int totalCount = producerCount * count;
        int slow = 20;
        int slowSleepTime = 1;

        AtomicInteger appliedCount = new AtomicInteger(0);
        AtomicInteger executedCount = new AtomicInteger(0);

        ExecutorService producers = Executors.newFixedThreadPool(producerCount);

        Map<Integer, AtomicLong> totalTimeMap = new HashMap<>();
        Map<Integer, AtomicLong> countMap = new HashMap<>();
        for (int i = 0; i < userCount; i++) {
            totalTimeMap.put(i, new AtomicLong());
            countMap.put(i, new AtomicLong());
        }
        for (int i = 0; i < producerCount; i++) {
            producers.execute(() -> {
                int number;
                while ((number = appliedCount.incrementAndGet()) <= totalCount) {
                    int id = number % userCount;
                    long startTime = System.currentTimeMillis();
                    consumer.accept(id, () -> {
                        if (id % slow == 0) {
                            long stopTime = System.currentTimeMillis() + slowSleepTime;
                            while (System.currentTimeMillis() < stopTime) {
                            }
                        }
                        executedCount.incrementAndGet();
                        countMap.get(id).incrementAndGet();
                        totalTimeMap.get(id).getAndAdd(System.currentTimeMillis() - startTime);
                    });
                }
                appliedCount.decrementAndGet();
            });
        }
        long time = System.currentTimeMillis() + 3000;
        while (executedCount.get() < totalCount) {
            if (System.currentTimeMillis() > time) {
                time = System.currentTimeMillis() + 3000;
                for (Map.Entry<Integer, AtomicLong> entry : countMap.entrySet()) {
                    int id = entry.getKey();
                    long entryCount = entry.getValue().get();
                    if (entryCount < count) {
                        String type = id % slow == 0 ? "slow" : "    ";
                        System.out.printf("[ %s ] id: %d count: %d wait task: %d \n"
                                , type
                                , id
                                , entryCount
                                , size.apply(id)
                        );
                    }
                }
            }
        }

        producers.shutdownNow();
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Map.Entry<Integer, AtomicLong> entry : countMap.entrySet()) {
            int id = entry.getKey();
            long entryCount = entry.getValue().get();
            long entryTime = totalTimeMap.get(entry.getKey()).get();
            long average = entryCount == 0 ? 0 : entryTime / entryCount;
            String type = id % slow == 0 ? "slow" : "    ";
            System.out.printf("[ %s ] id: %d average: %d count: %d time: %d\n"
                    , type
                    , id
                    , average
                    , entryCount
                    , entryTime
            );
            if (average > max) {
                max = average;
            }
            if (average < min) {
                min = average;
            }
        }
        System.out.printf("applied: %d executed: %d min: %d max: %d\n", appliedCount.get(), executedCount.get(), min, max);
    }
}
