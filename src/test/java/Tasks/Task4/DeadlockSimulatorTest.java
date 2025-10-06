package Tasks.Task4;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadlockSimulatorTest {

    @Test
    void methods_complete_without_deadlock() throws Exception {
        DeadlockSimulator sim = new DeadlockSimulator(); // your ReentrantLock version or ordered synchronized

        int pairs = 200;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < pairs; i++) {
            futures.add(pool.submit(sim::method1));
            futures.add(pool.submit(sim::method2));
        }

        // each future should complete within 2 seconds
        for (Future<?> f : futures) {
            f.get(2, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        // if we reach here, everything returned in time (no deadlock observed)
        assertTrue(true);
        System.out.println("DeadlockSimulatorTest passed!");
    }
}

