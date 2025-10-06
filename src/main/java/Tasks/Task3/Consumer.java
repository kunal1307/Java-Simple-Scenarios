package Tasks.Task3;

import java.util.concurrent.ThreadLocalRandom;

/** Simulates consumers that process logs based on their priority. */
class Consumer extends Thread {
    private final LogProcessor processor;
    private final String name;

    public Consumer(LogProcessor processor, String name) {
        this.processor = processor;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            while (true) {
                var task = processor.consumeLog();
                System.out.println("[Consumer " + name + "] processed: " + task);
                Thread.sleep(ThreadLocalRandom.current().nextInt(200, 500)); // simulate work
            }
        } catch (InterruptedException e) {
            System.out.println("[Consumer " + name + "] shutting down.");
        }
    }
}
