package pers.clare.concurrent;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import pers.clare.concurrent.vo.KeyBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

public class CustomKeyTests {

    @Nested
    @TestInstance(PER_CLASS)
    class Object extends AbstractFairExecutorServiceTest<Object> {
        private final Map<Integer, Object> keyMap = new ConcurrentHashMap<>();

        @Override
        FairExecutorService<Object> buildExecutorService() {
            return new FairExecutorService<>();
        }

        @Override
        protected Object toKey(int id) {
            return keyMap.computeIfAbsent(id, (index) -> new Object());
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class Bean extends AbstractFairExecutorServiceTest<KeyBean> {
        private final Map<Integer, KeyBean> keyMap = new ConcurrentHashMap<>();

        @Override
        FairExecutorService<KeyBean> buildExecutorService() {
            return new FairExecutorService<>();
        }

        @Override
        protected KeyBean toKey(int id) {
            return new KeyBean(id, "test");
        }

    }
}
