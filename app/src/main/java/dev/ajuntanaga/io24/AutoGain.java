package dev.ajuntanaga.io24;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Foreground meter-driven preamp control, adapted from the Linux host. */
final class AutoGain {
    private final List<double[]> samples = new ArrayList<>();
    private double lastChange = -100;
    void reset() { samples.clear(); }

    Float step(double now, float amplitude, float gain) {
        if (!Float.isFinite(amplitude) || amplitude < 0 || !Float.isFinite(gain)) {
            reset();
            return null;
        }
        if (now - lastChange < .5) return null;
        double db = 20 * Math.log10(Math.max(1e-12, amplitude));
        if (!samples.isEmpty() && now - samples.get(samples.size()-1)[0] > .35) reset();
        samples.add(new double[]{now, db});
        while (!samples.isEmpty() && samples.get(0)[0] < now - 3) samples.remove(0);
        if (samples.size() < 24 || now - samples.get(0)[0] < 2.9
                || now - lastChange < 3) return null;
        List<Double> sorted = new ArrayList<>();
        for (double[] sample : samples) sorted.add(sample[1]);
        Collections.sort(sorted);
        double loud = sorted.get((int)Math.ceil(sorted.size() * .95) - 1);
        double error = -12 - loud;
        if (loud < -60 || Math.abs(error) <= 4) return null;
        if (loud < -.5 && db > loud - 10) return null;
        if (error > 0) {
            double quiet = sorted.get((int)Math.ceil(sorted.size() * .1) - 1);
            if (loud - quiet < 10) return null;
            error = Math.min(error, -3 - sorted.get(sorted.size()-1));
            if (error <= 4) return null;
        }
        double delta = loud >= -.5 ? Math.max(-12, Math.min(0, error))
                : Math.max(-9, Math.min(6, error));
        float next = Math.round(Math.max(0, Math.min(60, gain + delta)) * 10) / 10f;
        if (next == gain) return null;
        lastChange = now;
        reset();
        return next;
    }
}
