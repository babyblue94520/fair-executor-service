package pers.clare.concurrent;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is an ExecuteService with multiple queues and fair handling of tasks.
 *
 * @param <Key> Key type.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class FairExecutorService<Key> {

    private final ConcurrentMap<Key, KeyQueue> queueMap = new ConcurrentHashMap<>();

    private final ExecutorService executorService;

    /**
     * Tasks that each queue can execute concurrently.
     */
    private final int concurrent;

    private final KeyQueue defaultKeyQueue = this.createQueue(null);

    public FairExecutorService() {
        this(null);
    }

    public FairExecutorService(int concurrent) {
        this(concurrent, null);
    }

    public FairExecutorService(ExecutorService executorService) {
        this(0, executorService);
    }

    public FairExecutorService(int concurrent, ExecutorService executorService) {
        this.concurrent = concurrent > 0 ? concurrent : 1;
        this.executorService = Objects.requireNonNullElseGet(executorService, () -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    public Future<?> submit(Key key, Runnable task) {
        return submit(key, task, null);
    }

    public <T> Future<T> submit(Key key, Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> future = newTaskFor(task, result);
        execute(key, future);
        return future;
    }

    public <T> Future<T> submit(Key key, Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> future = newTaskFor(task);
        execute(key, future);
        return future;
    }

    /**
     * @param key     Create a separate queue by key.
     * @param command Task.
     */
    public void execute(Key key, Runnable command) {
        getQueue(key).put(command);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return submit((Key) null, task);
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return submit(null, task, result);
    }

    public Future<?> submit(Runnable task) {
        return submit((Key) null, task);
    }

    /**
     * Tasks put in the default queue.
     *
     * @param command Task.
     */
    public void execute(Runnable command) {
        execute(null, command);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<>(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<>(callable);
    }

    public KeyQueue getQueue(Key key) {
        if (key == null) {
            return defaultKeyQueue;
        } else {
            return queueMap.computeIfAbsent(key, this::createQueue);
        }
    }

    protected KeyQueue createQueue(Key key) {
        return new KeyQueue(new ConcurrentLinkedQueue<>());
    }

    public int size(Key key) {
        KeyQueue queue = queueMap.get(key);
        if (queue != null) {
            return queue.size();
        }
        return 0;
    }

    class KeyQueue {
        private final Queue<Runnable> queue;
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicInteger current = new AtomicInteger();

        KeyQueue(Queue<Runnable> queue) {
            this.queue = queue;
        }

        public int size() {
            return queue.size();
        }

        void put(Runnable command) {
            boolean run;
            try {
                lock.lock();
                queue.add(command);
                run = hold();
            } finally {
                lock.unlock();
            }
            if (run) doRun();
        }

        private void doRun() {
            try {
                if (executorService.isShutdown()) return;
                Runnable command;
                try {
                    lock.lock();
                    command = queue.poll();
                    if (command == null) {
                        release();
                        return;
                    }
                } finally {
                    lock.unlock();
                }
                executorService.execute(() -> {
                    try {
                        command.run();
                    } finally {
                        doRun();
                    }
                });
            } catch (Exception e) {
                release();
                throw e;
            }
        }

        private boolean hold() {
            if (current.incrementAndGet() <= concurrent) {
                return true;
            } else {
                current.decrementAndGet();
                return false;
            }
        }

        private void release() {
            current.decrementAndGet();
        }
    }
}
