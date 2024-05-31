package org.soc.sonices;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScalerTest {

    @Test
    public void testScale() {
        Scaler s = new Scaler(10, 20, 100, 200);
        assertEquals(100, s.scale(10), Float.MIN_VALUE);
        assertEquals(200, s.scale(20), Float.MIN_VALUE);
        assertEquals(110, s.scale(11), Float.MIN_VALUE);
        assertEquals(115, s.scale(11.5f), Float.MIN_VALUE);
        assertEquals(100, s.scale(0), Float.MIN_VALUE);
        assertEquals(200, s.scale(30), Float.MIN_VALUE);
    }
}
