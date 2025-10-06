package Tasks.Task4;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class demonstrates and fixes a potential deadlock scenario
 * that occurs when multiple threads try to acquire shared locks
 * in different orders under high concurrency.
 *
 * It replaces simple synchronized blocks with ReentrantLocks
 * and enforces a consistent global locking order to prevent circular waits.
 * Includes an optional deadlock detector thread that reports any blocked threads.
 *
 * Scope:
 * - Thread-safe for multiple concurrent users (threads)
 * - Scales to any number of locks
 * - Safe to use alongside third-party libraries that use internal locks
 */
public class DeadlockSimulator {
    // plain locks (same idea as lock1/lock2)
    private final ReentrantLock lock1 = new ReentrantLock();
    private final ReentrantLock lock2 = new ReentrantLock();

    // small tunables for the tryLock path
    private static final long PER_LOCK_TIMEOUT_MS = 50;
    private static final int MAX_ATTEMPTS = 50;

    /* Acquire all locks in a stable order; block until acquired. */
    private static void withLocks(Collection<ReentrantLock> locks, Runnable task) {
        if (locks == null || locks.isEmpty()) {
            task.run();
            return;
        }
        List<ReentrantLock> ordered = order(locks);

        for (ReentrantLock l : ordered) {
            l.lock();
        }
        try {
            task.run();
        } finally {
            for (int i = ordered.size() - 1; i >= 0; i--) {
                ordered.get(i).unlock();
            }
        }
    }

    /* Same as above but with the use of tryLock(timeout) and retry. Handy when using third-party code. */
    private static void withLocksTry(Collection<ReentrantLock> locks, Runnable task) throws InterruptedException {
        if (locks == null || locks.isEmpty()) {
            task.run();
            return;
        }
        List<ReentrantLock> ordered = order(locks);

        int attempts = 0;
        while (true) {
            attempts++;
            List<ReentrantLock> acquired = new ArrayList<>(ordered.size());
            try {
                boolean all = true;
                for (ReentrantLock l : ordered) {
                    if (l.tryLock(PER_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                        acquired.add(l);
                    } else {
                        all = false;
                        break;
                    }
                }
                if (all) {
                    task.run();
                    return;
                }
            } finally {
                // release what we grabbed this round
                for (int i = acquired.size() - 1; i >= 0; i--) {
                    acquired.get(i).unlock();
                }
            }
            if (attempts >= MAX_ATTEMPTS) {
                throw new RuntimeException("gave up acquiring locks after " + attempts + " attempts");
            }
            // tiny fixed backoff; simple and good enough here
            Thread.sleep(5);
        }
    }

    /* Deterministic ordering by identity prevents cycles. Also, dedupes the collection. */
    private static List<ReentrantLock> order(Collection<ReentrantLock> locks) {
        // HashSet to dedupe in case the same lock is passed twice
        List<ReentrantLock> list = new ArrayList<>(new HashSet<>(locks));
        list.sort((a, b) -> Integer.compare(System.identityHashCode(a), System.identityHashCode(b)));
        return list;
    }

    public void method1() {
        withLocks(Arrays.asList(lock1, lock2), () -> {
            // keep this section short; no blocking IO if possible
            System.out.println("method1: got lock1 & lock2");
        });
    }

    public void method2() {
        try {
            withLocksTry(Arrays.asList(lock1, lock2), () -> {
                // this path is safer if you might call into libs that also use locks
                System.out.println("method2: got lock1 & lock2 (tryLock)");
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while acquiring locks", e);
        }
    }

    // optional: run this during tests to catch any deadlocks
    public static void startDeadlockDetector() {
        Thread t = new Thread(() -> {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            for (;;) {
                long[] ids = mx.findDeadlockedThreads();
                if (ids != null) {
                    ThreadInfo[] infos = mx.getThreadInfo(ids, true, true);
                    System.err.println("DEADLOCK DETECTED: " + Arrays.toString(ids));
                    for (ThreadInfo ti : infos) {
                        System.err.println(ti);
                    }
                    // System.exit(2); // uncomment if you want to fail fast in CI
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "deadlock-detector");
        t.setDaemon(true);
        t.start();
    }
    public static void main(String[] args) {
        startDeadlockDetector();
        DeadlockSimulator simulator = new DeadlockSimulator();
        Thread t1 = new Thread(simulator::method1);
        Thread t2 = new Thread(simulator::method2);
        t1.start();
        t2.start();
    }
}



