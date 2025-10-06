package Tasks.Task3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogProcessorTest {

    @Test
    void higherPriorityComesFirst() throws Exception {
        LogProcessor p = new LogProcessor();
        System.out.println(">>> Running LogProcessorTest.testLogPriorities");
        // add in mixed order (priority: higher = more important OR per your compareTo)
        p.produceLog("low", 1);
        p.produceLog("mid", 5);
        p.produceLog("high", 10);

        // consume in order; adjust assertions to your chosen priority semantics
        var t1 = p.consumeLog();
        var t2 = p.consumeLog();
        var t3 = p.consumeLog();

        // for our version: 10 > 5 > 1
        assertTrue(t1.toString().contains("high"));
        assertTrue(t2.toString().contains("mid"));
        assertTrue(t3.toString().contains("low"));
        System.out.println("LogProcessorTest passed!");
    }
}

