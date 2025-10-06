package Tasks.Task3;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * use f thread-safe PriorityBlockingQueue
 * instead of a simple LinkedList.
 *
 * This lets us handle multiple producers and consumers safely,
 * and ensures high-priority logs are processed first.
 */
public class LogProcessor {
    // PriorityBlockingQueue automatically orders tasks by priority
    private final PriorityBlockingQueue<LogTask> logQueue = new PriorityBlockingQueue<>();


    /** Producer adds a log with a given priority (higher = more important). */
    public void produceLog(String log, int priority) {
        logQueue.put(new LogTask(log, priority));
        System.out.println("[Producer] Added: " + log + " (priority " + priority + ")");
    }

    /** Consumer takes logs from the queue and processes them.
     * Helper for tests or demos — returns a string form of the consumed log.*/
    public String consumeLog() throws InterruptedException {
        LogTask task = logQueue.take(); // waits if empty or safely block until something available
        return task.toString();
    }

    /** A simple wrapper class that adds priority support. */
    private static class LogTask implements Comparable<LogTask> {
        final String message;
        final int priority;

        LogTask(String message, int priority) {
            this.message = message;
            this.priority = priority;
        }

        // Higher number = higher priority
        @Override
        public int compareTo(LogTask other) {
            return Integer.compare(other.priority, this.priority);
        }

        @Override
        public String toString() {
            return message + " (priority " + priority + ")";
        }
    }
}
