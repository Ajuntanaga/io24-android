package dev.ajuntanaga.io24;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutoGainTest {
    @Test public void silenceAndSteadyNoiseNeverRise() {
        for (float level : new float[]{0, .01f}) {
            AutoGain auto = new AutoGain();
            for (int n=0; n<100; n++) assertNull(auto.step(n*.1, level, 20));
        }
    }
    @Test public void sustainedClippingDropsGainWithinLimits() {
        AutoGain auto = new AutoGain();
        Float result = null;
        for (int n=0; n<=30; n++) {
            Float next = auto.step(n*.1, 1, 20);
            if (next != null) result = next;
        }
        assertNotNull(result);
        assertEquals(8f, result, .001f);
    }
    @Test public void speechRisesOnlyInGap() {
        AutoGain auto = new AutoGain();
        for (int n=0; n<30; n++) assertNull(auto.step(n*.1, n%5==0 ? .001f : .03f, 20));
        assertEquals(26f, auto.step(3, .001f, 20), .001f);
    }
    @Test public void missingReadingsDiscardWindow() {
        AutoGain auto = new AutoGain();
        for (int n=0; n<29; n++) assertNull(auto.step(n*.1, 1, 20));
        assertNull(auto.step(5, 1, 20));
        assertNull(auto.step(5.1, Float.NaN, 20));
    }
    @Test public void gainAtFloorCannotFall() {
        AutoGain auto = new AutoGain();
        for (int n=0; n<60; n++) assertNull(auto.step(n*.1, 1, 0));
    }
}
