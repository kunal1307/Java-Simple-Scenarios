package Tasks.Task5;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Tiny scheduler that prints pool stats every 10s:
 * active / idle / total / threads waiting.
 * This is our "custom monitoring" to spot long waits or underuse.
 */
@Component
public class PoolMonitor {

    private final HikariDataSource hikari;
    private volatile boolean activated = false; // ensure we only warm once

    public PoolMonitor(DataSource dataSource) {
        // Spring may proxy DataSource; unwrap if needed.
        System.out.println("[DB] Injected DataSource type = " + dataSource.getClass().getName());
        this.hikari = (dataSource instanceof HikariDataSource) ? (HikariDataSource) dataSource : null;
        if (this.hikari == null) {
            System.err.println("[DB][WARN] DataSource is not HikariDataSource; pool stats will be skipped.");
        }
    }

    @Scheduled(initialDelay= 2_000, fixedRate = 10_000) //every 10 seconds
    public void logPool() {
        if (hikari == null) return;
        HikariPoolMXBean mx = hikari.getHikariPoolMXBean();
        // If the pool/bean isn't ready yet, do a one-time warm-up by borrowing a connection.
        if (mx == null) {
            if (!activated) {
                try (Connection ignored = hikari.getConnection()) {
                    System.out.println("[DB] PoolMonitor warm-up OK (opened + closed one connection).");
                    activated = true;
                } catch (Exception e) {
                    System.err.println("[DB] PoolMonitor warm-up failed: " + e.getMessage());
                }
            } else {
                // Happens if the pool hasn't started yet or failed to initialize
                System.out.println("[DB] Hikari MXBean still not available (pool not fully ready).");
            }
            return;
        }
        int active = mx.getActiveConnections();
        int idle   = mx.getIdleConnections();
        int total  = mx.getTotalConnections();
        int await  = mx.getThreadsAwaitingConnection();

        System.out.printf("[DB] active=%d idle=%d total=%d awaiting=%d%n",
                active, idle, total, await);

        // Simple “alerts”
        if (await > 0) {
            System.err.println("[DB][ALERT] Threads are waiting for connections. Consider tuning pool size or slow queries.");
        }
        // If the pool is almost never busy, consider shrinking it
        if (active < 2 && total > 10) {
            System.out.println("[DB][INFO] Pool may be larger than needed right now.");
        }
    }
}

