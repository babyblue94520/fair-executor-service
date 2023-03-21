package pers.clare.concurrent;

public abstract class AbstractIntegerFairExecutorServiceTest extends AbstractFairExecutorServiceTest<Integer> {
    @Override
    protected Integer toKey(int index) {
        return index;
    }
}
