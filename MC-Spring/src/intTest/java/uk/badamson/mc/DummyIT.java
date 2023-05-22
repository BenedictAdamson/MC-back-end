package uk.badamson.mc;

import org.junit.jupiter.api.Test;

public class DummyIT {

    @Test
    public void good() {

    }
    @Test
    public void bad() {
        throw new AssertionError();
    }
}
