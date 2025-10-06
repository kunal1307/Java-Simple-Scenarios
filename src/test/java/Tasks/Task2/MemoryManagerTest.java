package Tasks.Task2;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class MemoryManagerTest {

    @Test
    void add_and_remove_sessionData_basic() {
        String id = "U-1";
        assertNull(MemoryManager.getSessionData(id)); // not present yet

        MemoryManager.addSessionData(id);
        assertNotNull(MemoryManager.getSessionData(id)); // added

        MemoryManager.removeSessionData(id);
        assertNull(MemoryManager.getSessionData(id)); // removed
    }

    @Test
    void concurrent_adds_doNotThrow() throws InterruptedException {
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final String id = "U-" + i;
            pool.submit(() -> {
                try {
                    MemoryManager.addSessionData(id);
                    assertNotNull(MemoryManager.getSessionData(id));
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(3, TimeUnit.SECONDS);
        pool.shutdownNow();
    }
}

