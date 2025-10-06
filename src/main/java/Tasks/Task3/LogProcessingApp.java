package Tasks.Task3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main application — sets up producers and consumers to run together.
 * You’ll see that high-priority logs get processed first,
 * but low-priority ones also eventually get picked up.
 */
public class LogProcessingApp {
    public static void main(String[] args) throws InterruptedException {
        LogProcessor processor = new LogProcessor();

        // 2 producers, 2 consumers
        Producer p1 = new Producer(processor, "Producer-1");
        Producer p2 = new Producer(processor, "Producer-2");
        Consumer c1 = new Consumer(processor, "Consumer-A");
        Consumer c2 = new Consumer(processor, "Consumer-B");

        p1.start();
        p2.start();
        c1.start();
        c2.start();

        // Let it run for a while, then stop gracefully
        Thread.sleep(7000);
        c1.interrupt();
        c2.interrupt();

        System.out.println("Main thread exiting...");
    }
}
