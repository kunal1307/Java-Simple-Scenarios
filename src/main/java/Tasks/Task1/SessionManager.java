package Tasks.Task1;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * This class manages user sessions safely under concurrency.
 * It fixes race conditions in login/logout, adds a small expiry system
 * so old sessions don’t pile up in memory, and keeps responses friendly.
 *
 * Scope:
 * - Thread-safe within one JVM instance (good for demo / local backend)
 * - 30-minute default session lifetime
 */
public class SessionManager {

    /**
     * Represents one active session.
     * Each session has an ID and an expiry timestamp.
     * When "expiresAt" passes, the session is considered invalid.
     */
    private static final class Session {
        final String id;
        final long   expiresAt;
        Session(String id, long expiresAt) {
            this.id = id;
            this.expiresAt = expiresAt;
        }
    }

    // Thread-safe map: userId → Session
    // Using ConcurrentHashMap avoids synchronization problems when many users log in/out at once.
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // 30 minutes default TTL (tunable as per you requirements)
    private final long ttlSeconds;

    // Background ScheduledExecutorService to remove expired entries
    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "session-janitor");
                t.setDaemon(true); // do not block JVM exit
                return t;
            });

    // Default constructor – 30 min TTL
    public SessionManager() {
        this(30 * 60); // calls the next constructor
    }

    // Overloaded constructor – allows custom TTL
    public SessionManager(long ttlSeconds) {
        // run every minute
        this.ttlSeconds = ttlSeconds;
        scheduledExecutorService.scheduleAtFixedRate(this::cleanUpExpired, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Login semantics:
     * - If user already has a valid session, return that (no duplicate sessions).
     * - Else create a new session atomically.
     */
    public String login(String userId) {
        // Get current time (in seconds) to check session expiry
        long now = Instant.now().getEpochSecond();
        // Check if the user already has an active (non-expired) session
        Session existing = sessions.get(userId);
        if (existing != null && existing.expiresAt > now) {
            // User is already logged in — just reuse the existing session ID
            return "User already logged in. Session ID: " + existing.id;
        }

        // No valid session found → create a new one
        String newId = "SESSION_" + UUID.randomUUID();

        // Each session stores both the ID and its expiry timestamp (now + TTL)
        Session fresh = new Session(newId, now + ttlSeconds);

        // Atomically add or replace the old session
        // Using 'put' ensures that the new session replaces an expired one if present
        sessions.put(userId, fresh);

        // Return the new session info to the client
        return "Login successful. Session ID: " + newId;
    }

    /**
     * Logout semantics:
     * - Remove session if present (expired or not).
     */
    public String logout(String userId) {
        return (sessions.remove(userId) != null)
                ? "Logout successful."
                : "User not logged in.";
    }

    /**
     * Query semantics:
     * - If missing or expired, return friendly message and tidy up.
     */
    public String getSessionDetails(String userId) {
        long now = Instant.now().getEpochSecond();
        Session s = sessions.get(userId);
        if (s == null || s.expiresAt <= now) {
            sessions.remove(userId); // tidy up if it was expired
            return "Session not found for user " + userId;
        }
        return "Session ID for user " + userId + ": " + s.id;
    }

    /**
     * Periodic cleanup method.
     * Scans all sessions and removes any that are past their expiry time.
     * Called automatically by the ScheduledExecutorService thread once a minute.
     */
    private void cleanUpExpired() {
        long now = Instant.now().getEpochSecond();
        // removeIf is safe here; ConcurrentHashMap supports it internally
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt <= now);
    }

    public static void main(String[] args) throws Exception {
        // Use a short TTL for demo so we can see expiry without waiting 30 mins
        SessionManager sm = new SessionManager(5); // 5 seconds TTL for the demo

        System.out.println("=== Basic flow: user1..user4 ===");
        for (int i = 1; i <= 4; i++) {
            String user = "user" + i;
            System.out.println(user + " -> " + sm.login(user));
            System.out.println(user + " -> " + sm.getSessionDetails(user));
        }

        System.out.println("\n=== Duplicate login prevention (user1) ===");
        System.out.println("user1 -> " + sm.login("user1")); // should say already logged in with same ID

        System.out.println("\n=== Logout test (user2) ===");
        System.out.println("user2 -> " + sm.logout("user2"));
        System.out.println("user2 -> " + sm.getSessionDetails("user2")); // should say not found

        System.out.println("\n=== Concurrency test (many login attempts for user3) ===");
        var pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 20; i++) {
            pool.submit(() -> System.out.println("concurrent -> " + sm.login("user3")));
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);

        System.out.println("\n=== TTL expiry test (wait 6 seconds) ===");
        TimeUnit.SECONDS.sleep(6);
        System.out.println("user1 after TTL -> " + sm.getSessionDetails("user1")); // likely expired
        System.out.println("user3 after TTL -> " + sm.getSessionDetails("user3")); // likely expired

        System.out.println("\nDemo complete.");
    }
}
