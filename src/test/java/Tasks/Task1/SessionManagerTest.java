package Tasks.Task1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @Test
    void login_thenGetDetails_returnsSameSession() {
        // short TTL just for the test (if your ctor supports it; if not, use default)
        SessionManager sm = new SessionManager(300); // 5 min; adjust to your ctor

        String r1 = sm.login("user1");
        assertTrue(r1.contains("Login successful"));

        String d1 = sm.getSessionDetails("user1");
        assertTrue(d1.contains("Session ID"));

        // second login should say already logged in (and keep same session id visible)
        String r2 = sm.login("user1");
        assertTrue(r2.contains("already logged in"));
    }

    @Test
    void logout_thenGetDetails_returnsNotFound() {
        SessionManager sm = new SessionManager(300);

        sm.login("user2");
        assertEquals("Logout successful.", sm.logout("user2"));

        String d2 = sm.getSessionDetails("user2");
        assertTrue(d2.contains("Session not found"));
    }
}

