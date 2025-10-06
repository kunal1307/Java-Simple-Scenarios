package Tasks.Task3;

import java.util.concurrent.ThreadLocalRandom;

/** Simulates producers adding logs of random priority. */
class Producer extends Thread {
    private final LogProcessor processor;
    private final String name;

    public Producer(LogProcessor processor, String name) {
        this.processor = processor;
        this.name = name;
    }

    @Override
    public void run() {
        for (int i = 0; i < 8; i++) {
            int priority = ThreadLocalRandom.current().nextInt(1, 6); // 1-5 priority
            processor.produceLog(name + " - log " + i, priority);
            try { Thread.sleep(ThreadLocalRandom.current().nextInt(100, 400)); } catch (InterruptedException ignored) {}
        }
    }
}
