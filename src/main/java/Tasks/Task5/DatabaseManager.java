package Tasks.Task5;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Thin wrapper around Spring's DataSource.
 * We time how long it takes to get a connection. If it's slow, we log it.
 * Slow acquire usually means: pool is exhausted (too small) or queries are slow.
 */

@Component
public class DatabaseManager {
    @Autowired
    private DataSource dataSource;

    // threshold for “slow to acquire a connection” (tweak as needed)
    private static final long ACQUIRE_WARN_MS = 200; // 200ms is a decent starting signal

    /** Get a pooled connection; log if the pool made us wait too long. */
    public Connection getConnection() throws SQLException {
        long start = System.nanoTime();
        Connection c = dataSource.getConnection();
        long tookMs = (System.nanoTime() - start) / 1_000_000L;

        if (tookMs > ACQUIRE_WARN_MS) {
            System.err.println("[DB] Slow connection acquire: " + tookMs + " ms");
        }
        return c;
    }

    /** Always close to return the connection to the pool. */
    public void closeConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();// return it to pool
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error closing connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
