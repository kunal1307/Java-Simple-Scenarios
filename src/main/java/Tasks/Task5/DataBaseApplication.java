package Tasks.Task5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Small Spring Boot app to demonstrate HikariCP with custom monitoring.
 * - @EnableScheduling lets our monitor run a tiny job every few seconds.
 */
@SpringBootApplication
@EnableScheduling
public class DataBaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataBaseApplication.class, args);
    }
}

