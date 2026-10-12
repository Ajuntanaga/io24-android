package dev.ajuntanaga.io24;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public final class GraphMathTest {
    @Test
    public void everyVoiceFxParameterChangesItsGraphTrace() {
        Io24State defaults = Io24State.defaults();
        for (Io24State.VoiceFxModel model : Io24State.VoiceFxModel.values()) {
            Map<String, Float> base = defaults.voiceFx(model).parameters();
            float[] baseline = GraphMath.effectTrace(model, base, 320, 120);
            for (Map.Entry<String, Float> entry : base.entrySet()) {
                Map<String, Float> changed = new LinkedHashMap<>(base);
                changed.put(entry.getKey(), changedValue(model, entry));
                float[] trace = GraphMath.effectTrace(model, changed, 320, 120);
                assertFalse(
                        model + " " + entry.getKey() + " did not affect its graph",
                        same(baseline, trace));
            }
        }
    }

    @Test
    public void dynamicsAndEqParametersChangeTheirCurves() {
        float[] dynamics = GraphMath.dynamicsCurve(-18, 4, 3, 320, 120);
        assertFalse(same(dynamics,
                GraphMath.dynamicsCurve(-30, 4, 3, 320, 120)));
        assertFalse(same(dynamics,
                GraphMath.dynamicsCurve(-18, 10, 3, 320, 120)));
        assertFalse(same(dynamics,
                GraphMath.dynamicsCurve(-18, 4, 9, 320, 120)));

        float[] frequencies = {130, 320, 1400, 5000};
        float[] gains = {4, -3, 5, -4};
        float[] q = {0.6f, 0.8f, 1.4f, 0.6f};
        boolean[] on = {true, true, true, true};
        boolean[] shelf = {true, false, false, true};
        float[] eq = GraphMath.eqCurve(
                frequencies, gains, q, on, shelf, 48_000, 320, 120);

        for (int band = 0; band < 4; band++) {
            float[] changedFrequency = frequencies.clone();
            changedFrequency[band] *= 1.2f;
            assertFalse("Band " + band + " frequency did not move the curve",
                    same(eq, GraphMath.eqCurve(changedFrequency, gains, q, on,
                            shelf, 48_000, 320, 120)));

            float[] changedGain = gains.clone();
            changedGain[band] += 1.0f;
            assertFalse("Band " + band + " gain did not move the curve",
                    same(eq, GraphMath.eqCurve(frequencies, changedGain, q, on,
                            shelf, 48_000, 320, 120)));

            float[] changedQ = q.clone();
            changedQ[band] *= 1.35f;
            assertFalse("Band " + band + " Q did not move the curve",
                    same(eq, GraphMath.eqCurve(frequencies, gains, changedQ, on,
                            shelf, 48_000, 320, 120)));

            boolean[] changedPower = on.clone();
            changedPower[band] = false;
            assertFalse("Band " + band + " power did not move the curve",
                    same(eq, GraphMath.eqCurve(frequencies, gains, q,
                            changedPower, shelf, 48_000, 320, 120)));
        }

        for (int band : new int[] {0, 3}) {
            boolean[] changedShelf = shelf.clone();
            changedShelf[band] = false;
            assertFalse("Band " + band + " mode did not move the curve",
                    same(eq, GraphMath.eqCurve(frequencies, gains, q, on,
                            changedShelf, 48_000, 320, 120)));
        }
    }

    @Test
    public void standardEqCurveHasTheExpectedFlatAndLowShelfResponse() {
        float[] frequencies = {130, 320, 1400, 5000};
        float[] q = {0.6f, 0.6f, 0.6f, 0.6f};
        boolean[] on = {true, true, true, true};
        boolean[] shelf = {true, false, false, true};
        float[] flat = GraphMath.eqCurve(
                frequencies,
                new float[] {0, 0, 0, 0},
                q,
                on,
                shelf,
                48_000,
                320,
                120);
        for (float point : flat) {
            assertEquals(60.0f, point, 0.01f);
        }

        float[] lowBoost = GraphMath.eqCurve(
                frequencies,
                new float[] {6, 0, 0, 0},
                q,
                on,
                shelf,
                48_000,
                320,
                120);
        assertTrue("A low-shelf boost must rise on the low-frequency side",
                lowBoost[0] < flat[0] - 5.0f);
        assertEquals("A low shelf must return to unity at the high end",
                flat[flat.length - 1], lowBoost[lowBoost.length - 1], 0.1f);
    }

    private static float changedValue(
            Io24State.VoiceFxModel model,
            Map.Entry<String, Float> entry) {
        String name = entry.getKey();
        float value = entry.getValue();
        if (name.equals("voiced")) {
            return value < 0.5f ? 1.0f : 0.0f;
        }
        if (name.equals("detune")) {
            return value == 8.0f ? 0.0f : value + 1.0f;
        }
        if (name.equals("carrier_type")) {
            return (value + 1.0f) % 3.0f;
        }
        if (name.equals("carrier2")) {
            return value < 0.5f ? 1.0f : 0.0f;
        }
        if (name.equals("carrier_freq")) {
            return value + 25.0f;
        }
        if (name.contains("carrier") && name.contains("hz")) {
            return value + 17.0f;
        }
        if (name.equals("time_s")) {
            return value + 0.01f;
        }
        return value > 0.75f ? value - 0.2f : value + 0.2f;
    }

    private static boolean same(float[] left, float[] right) {
        if (left.length != right.length) {
            return false;
        }
        for (int index = 0; index < left.length; index++) {
            if (Float.floatToIntBits(left[index])
                    != Float.floatToIntBits(right[index])) {
                return false;
            }
        }
        return true;
    }
}
