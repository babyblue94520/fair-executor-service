package pers.clare.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is an ExecuteService with multiple queues and fair handling of tasks.
 *
 * @param <Key> Key type.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class FairExecutorService<Key> implements ExecutorService {

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

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public KeyQueue getQueue(Key key) {
        if (key == null) {
            return defaultKeyQueue;
        } else {
            return queueMap.computeIfAbsent(key, this::createQueue);
        }
    }

    /**
     * Replaced KeyQueue.
     * @param key Key.
     * @return Return unexecuted tasks.
     */
    public Runnable[] reset(Key key) {
        KeyQueue queue = queueMap.put(key, createQueue(key));
        if (queue == null) {
            return new Runnable[0];
        }else{
            return queue.clear();
        }
    }

    public int size(Key key) {
        KeyQueue queue = queueMap.get(key);
        if (queue != null) {
            return queue.size();
        }
        return 0;
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

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return submit((Key) null, task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit(null, task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit((Key) null, task);
    }

    /**
     * Tasks put in the default queue.
     *
     * @param command Task.
     */
    @Override
    public void execute(Runnable command) {
        execute(null, command);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        throw new RejectedExecutionException("invokeAll is not supported.");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        throw new RejectedExecutionException("invokeAll is not supported.");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
        throw new RejectedExecutionException("invokeAny is not supported.");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        throw new RejectedExecutionException("invokeAny is not supported.");
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<>(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<>(callable);
    }

    protected KeyQueue createQueue(Key key) {
        return new KeyQueue(new ConcurrentLinkedQueue<>());
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

        public Runnable[] clear() {
            try {
                lock.lock();
                Runnable[] tasks = queue.toArray(new Runnable[0]);
                queue.clear();
                return tasks;
            } finally {
                lock.unlock();
            }
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
