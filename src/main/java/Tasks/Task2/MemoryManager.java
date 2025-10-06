package Tasks.Task2;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Task 2 – Memory Management Fix
 * <p>
 * This version of MemoryManager fixes the memory leak from the original code.
 * It uses a ConcurrentHashMap to safely store session data from multiple threads,
 * and a simple cleanup mechanism that removes old sessions after a period of inactivity.
 * <p>
 * Key idea:
 * - Each session holds a 10 MB byte array (simulating user session data).
 * - When a session is idle for more than 5 minutes, it’s automatically removed.
 * - This ensures memory is reclaimed even if removeSessionData() is never called.
 */
public class MemoryManager {

    // Time after which idle sessions are cleaned up (5 minutes here)
    private static final long IDLE_TTL_MS = 5 * 60_000; // 5 minutes
    // Each session will allocate a 10 MB data block (for simulation)
    private static final int BLOB_BYTES = 10 * 1024 * 1024;

    // Thread-safe map to store sessionId -> session data
    // ConcurrentHashMap allows multiple threads to read/write safely
    private static final ConcurrentHashMap<String, SessionEntry> cache = new ConcurrentHashMap<>();

    /**
     * Each SessionEntry represents one active session.
     * It stores the 10 MB data and the last time this session was accessed.
     */
    private static class SessionEntry {
        final byte[] data = new byte[BLOB_BYTES];     // Simulated session data
        volatile long lastAccess = System.currentTimeMillis();    // Tracks when last used
    }


    // Static initializer: runs once when the class is loaded.
    // Creates a background "cleaner" thread that removes expired sessions every minute.
    static {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "memory-cleaner");
            t.setDaemon(true); // allows JVM to exit once main thread finishes
            return t;
        });
        cleaner.scheduleAtFixedRate(MemoryManager::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Called when a new session starts or logs in.
     * Allocates memory (10 MB per session) and adds it to the cache.
     * in short Add or refresh session data
     */
    public static void addSessionData(String id) {
        cache.put(id, new SessionEntry());
    }

    /**
     * Called when a session ends or user logs out.
     * Removes session data immediately to free memory.
     */
    public static void removeSessionData(String id) {
        cache.remove(id);
    }

    /**
     * Fetch session data by ID. If found, update its last access time.
     * This keeps active sessions from being deleted too early.
     */
    public static byte[] getSessionData(String id) {
        SessionEntry e = cache.get(id);
        if (e != null) {
            // Refresh last access time so active sessions are not evicted
            e.lastAccess = System.currentTimeMillis();
            return e.data;
        }
        return null; // No session found for this ID
    }


    /**
     * Internal/Background cleanup method.
     * Runs periodically in the background and removes sessions that
     * have been idle for longer than the TTL (time-to-live).
     */
    private static void cleanup() {
        long now = System.currentTimeMillis();
        // Iterate through the cache and remove stale sessions
        for (Map.Entry<String, SessionEntry> e : cache.entrySet()) {
            // If session hasn't been used for more than TTL, remove it
            if (now - e.getValue().lastAccess > IDLE_TTL_MS) {
                cache.remove(e.getKey());
                System.out.println("[Cleaner] Removed expired session: " + e.getKey());
            }
        }
        System.out.println("[Cleaner] Active sessions: " + cache.size());
    }
}

