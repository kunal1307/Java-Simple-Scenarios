package Tasks.Task5;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@TestPropertySource(properties = {
        // override to H2 for tests (no local Postgres required)
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.hikari.maximum-pool-size=5",
        "spring.datasource.hikari.minimum-idle=1"
})
class DataSourceSmokeTest {

    @Autowired
    DataSource dataSource;

    @Test
    void dataSource_isHikari_andWorks() throws Exception {
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);

        try (var conn = dataSource.getConnection()) {
            assertFalse(conn.isClosed());
            try (var st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS t(x INT)");
            }
        }
    }
}
