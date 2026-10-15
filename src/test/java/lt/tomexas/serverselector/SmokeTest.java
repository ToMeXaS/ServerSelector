package lt.tomexas.serverselector;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmokeTest {

    @Test
    void paperApiIsOnTheTestClasspath() {
        assertEquals(3.0, new Vector(1, 2, 0).add(new Vector(2, 0, 0)).getX());
    }
}
