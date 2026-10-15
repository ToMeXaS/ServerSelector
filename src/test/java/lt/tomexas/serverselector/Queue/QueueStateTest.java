package lt.tomexas.serverselector.Queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueueStateTest {

    @Test
    void progressIsFullAtTheFront() {
        assertEquals(1.0, QueueState.queued("survival", 1, 4).progress(), 1e-9);
        assertEquals(0.25, QueueState.queued("survival", 4, 4).progress(), 1e-9);
    }

    @Test
    void progressIsZeroWithoutData() {
        assertEquals(0.0, QueueState.queued("survival", 0, 0).progress(), 1e-9);
        assertEquals(0.0, QueueState.queued("survival", 5, 4).progress(), 1e-9);
        assertEquals(0.0, QueueState.NOT_QUEUED.progress(), 1e-9);
    }
}
