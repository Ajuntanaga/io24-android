package dev.ajuntanaga.io24;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Pure-Java builders for the proven native io24 DSP records. */
final class NativeDsp {
    static final int BLOCK_MIXER = 100;
    static final int BLOCK_VOICE_FX = 201;
    static final int BLOCK_REVERB = 202;
    static final int BLOCK_FILTER = fourCc("filt");
    static final int BLOCK_GATE = fourCc("gate");
    static final int BLOCK_COMPRESSOR = fourCc("comp");
    static final int BLOCK_EQ = fourCc("eq  ");
    static final int BLOCK_LIMITER = fourCc("lim ");
    static final int BLOCK_ORDER = fourCc("opt ");

    private static final int TAG_PARA = 0x50617261;
    private static final int TAG_PARI = 0x50617269;
    private static final int TAG_VOFX = 0x566f4678;
    private static final int TAG_GODV = 0x676f6476;
    private static final int TAG_MAY4 = 0x6d617934;
    private static final int TAG_BOTA = 0x626f7461;
    private static final int TAG_BOTB = 0x626f7462;
    private static final int TAG_BOTC = 0x626f7463;
    private static final int TAG_VECH = 0x76656368;
    private static final int TAG_INIA = 0x696e6961;
    private static final int TAG_BQDF = 0x42716466;
    private static final int TAG_MBDF = 0x4d426466;
    private static final int TAG_VRVB = 0x76727662;
    private static final int TAG_CPXT = 0x63707874;
    private static final int TAG_GATE = 0x67617465;

    private static final float[] IDENTITY = {1, 0, 0, 0, 0};
    private static final float PI_F = Float.intBitsToFloat(0x40490fdb);
    private static final float TWO_PI_F = Float.intBitsToFloat(0x40c90fdb);
    private static final float[] TRANSFORMER_FREQUENCIES = {600, 550};
    private static final float[] NATIVE_RATES = {44_100, 48_000, 88_200, 96_000};
    private static final float[] VOCODER_HP = {
            85, 141, 230, 378, 622, 1024, 1680, 2730, 4450, 7250
    };
    private static final float[] VOCODER_LP = {
            141, 230, 378, 622, 1024, 1690, 2750, 4482, 7300, 11900
    };

    private NativeDsp() {
    }

    static byte[] mixerLevel(int sourceId, float db) {
        ByteBuffer blob = blob(TAG_PARA, 0x14, 0);
        blob.putInt(sourceId);
        blob.putFloat(db);
        return blob.array();
    }

    static byte[] processingOrder(boolean eqFirst) {
        ByteBuffer blob = blob(TAG_PARI, 0x14, 0);
        blob.putInt(0);
        blob.putInt(eqFirst ? 1 : 0);
        return blob.array();
    }

    static byte[] voiceFxSelector(Io24State.VoiceFxModel model) {
        ByteBuffer blob = blob(TAG_VOFX, 0x10, 0);
        blob.putInt(model.ordinal());
        return blob.array();
    }

    static List<byte[]> voiceFxMaterialization(
            Io24State.VoiceFxModel model,
            Io24State.VoiceFxState state,
            int sampleRateHz) {
        switch (model) {
            case TRANSFORMER:
                return transformerBlobs(state, sampleRateHz, true);
            case DETUNER:
                return Arrays.asList(detuner(state), detunerBiquad(sampleRateHz));
            case VOCODER:
                return Arrays.asList(vocoderFilters(sampleRateHz), vocoder(state));
            case RING_MOD:
                return List.of(ringMod(state));
            case FILTERS:
                return List.of(filters(state));
            case DELAY:
                if (sampleRateHz > Io24State.DELAY_NATIVE_MAX_RATE_HZ) {
                    throw new IllegalArgumentException(
                            "Delay cannot run safely on the device above 48 kHz");
                }
                return List.of(delay(state));
            default:
                throw new IllegalArgumentException("Unknown Voice FX model");
        }
    }

    static List<byte[]> voiceFxEdit(
            Io24State.VoiceFxModel model,
            Io24State.VoiceFxState state,
            String changedParameter,
            int sampleRateHz) {
        if (model == Io24State.VoiceFxModel.DELAY
                && sampleRateHz > Io24State.DELAY_NATIVE_MAX_RATE_HZ) {
            throw new IllegalArgumentException(
                    "Delay cannot run safely on the device above 48 kHz");
        }
        switch (model) {
            case TRANSFORMER:
                return transformerBlobs(
                        state, sampleRateHz, "lows".equals(changedParameter));
            case DETUNER:
                return List.of(detuner(state));
            case VOCODER:
                return List.of(vocoder(state));
            case RING_MOD:
                return List.of(ringMod(state));
            case FILTERS:
                return List.of(filters(state));
            case DELAY:
                return List.of(delay(state));
            default:
                throw new IllegalArgumentException("Unknown Voice FX model");
        }
    }

    private static List<byte[]> transformerBlobs(
            Io24State.VoiceFxState state,
            int sampleRateHz,
            boolean refreshTone) {
        List<byte[]> result = new ArrayList<>();
        float lows = state.parameter("lows");
        if (refreshTone) {
            float gainDb = f32(lows * 12.0f);
            for (int band = 0; band < TRANSFORMER_FREQUENCIES.length; band++) {
                result.add(biquadBlob(
                        band,
                        lowShelfFx(
                                TRANSFORMER_FREQUENCIES[band],
                                gainDb,
                                sampleRateHz,
                                0.7f)));
            }
            for (int table = 0; table < TRANSFORMER_FREQUENCIES.length; table++) {
                float[][] coefficients = new float[NATIVE_RATES.length][];
                for (int index = 0; index < NATIVE_RATES.length; index++) {
                    coefficients[index] = lowShelfFx(
                            TRANSFORMER_FREQUENCIES[table],
                            gainDb,
                            NATIVE_RATES[index],
                            0.7f);
                }
                result.add(multiBiquadBlob(table, coefficients));
            }
        }
        ByteBuffer stateBlob = blob(TAG_GODV, 0x20, 0);
        stateBlob.putInt(state.on() ? 1 : 0);
        stateBlob.putFloat(clamp01(lows));
        stateBlob.putFloat(clamp01(state.parameter("width")));
        stateBlob.putFloat(clamp01(lows));
        stateBlob.putFloat(clamp01(state.parameter("mix")));
        result.add(stateBlob.array());
        return result;
    }

    private static byte[] detuner(Io24State.VoiceFxState state) {
        int index = Math.max(0, Math.min(16, Math.round(state.parameter("detune"))));
        ByteBuffer result = blob(TAG_MAY4, 0x24, 0);
        result.putInt(state.on() ? 1 : 0);
        result.putInt(index - 8);
        result.putInt(0).putInt(0).putInt(0);
        result.putFloat(clamp01(state.parameter("mix")));
        return result.array();
    }

    private static byte[] detunerBiquad(int sampleRateHz) {
        return biquadBlob(0, lowPass(6000, sampleRateHz, 0.7f));
    }

    private static byte[] vocoderFilters(int sampleRateHz) {
        ByteBuffer result = blob(TAG_INIA, 0x1c4, 0);
        for (float frequency : VOCODER_HP) {
            putFloats(result, highPass(frequency, sampleRateHz, 2.0f));
        }
        for (float frequency : VOCODER_LP) {
            putFloats(result, lowPass(frequency, sampleRateHz, 2.0f));
        }
        putFloats(result, lowPass(600, sampleRateHz, 0.7f));
        putFloats(result, highPass(2500, sampleRateHz, 0.7f));
        return result.array();
    }

    private static byte[] vocoder(Io24State.VoiceFxState state) {
        ByteBuffer result = blob(TAG_BOTA, 0x28, 0);
        result.putInt(state.on() ? 1 : 0);
        result.putInt(Math.max(0, Math.min(2,
                Math.round(state.parameter("carrier_type")))));
        result.putFloat(state.parameter("carrier_freq"));
        result.putFloat(clamp01(state.parameter("vol")));
        result.putInt(1).putInt(1);
        result.putFloat(clamp01(state.parameter("mix")));
        return result.array();
    }

    private static byte[] ringMod(Io24State.VoiceFxState state) {
        ByteBuffer result = blob(TAG_BOTB, 0x28, 0);
        result.putInt(state.on() ? 1 : 0);
        result.putFloat(state.parameter("carrier_hz"));
        result.putFloat(clamp01(state.parameter("dist")));
        result.putFloat(clamp01(state.parameter("vol")));
        result.putInt(state.parameter("carrier2") >= 0.5f ? 1 : 0);
        result.putFloat(state.parameter("carrier2_hz"));
        result.putFloat(clamp01(state.parameter("mix")));
        return result.array();
    }

    private static byte[] filters(Io24State.VoiceFxState state) {
        float pitch = clamp01(state.parameter("pitch"));
        int samples = (int) f32(f32(pitch * f32(1300.0f)) + f32(250.5f));
        float feedback = f32(f32(clamp01(state.parameter("regeneration"))
                * f32(0.35f)) + f32(0.5f));
        float damping = f32(f32(clamp01(state.parameter("damping"))
                * f32(0.6f)) + f32(0.3f));
        ByteBuffer result = blob(TAG_BOTC, 0x28, 0);
        result.putInt(state.on() ? 1 : 0);
        result.putInt(samples);
        result.putFloat(feedback);
        result.putFloat(damping);
        result.putFloat(clamp01(state.parameter("distortion")));
        result.putFloat(clamp01(state.parameter("volume")));
        result.putFloat(clamp01(state.parameter("mix")));
        return result.array();
    }

    private static byte[] delay(Io24State.VoiceFxState state) {
        ByteBuffer result = blob(TAG_VECH, 0x1c, 0);
        result.putInt(state.on() ? 1 : 0);
        result.putFloat(clamp01(state.parameter("mix")));
        result.putFloat(f32(clamp01(state.parameter("feedback")) * 0.5f));
        result.putFloat(state.parameter("time_s"));
        return result.array();
    }

    static byte[] reverb(Io24State.ReverbState state, int sampleRateHz) {
        float hp = Math.max(0, Math.min(500, state.highPassHz()));
        float preDelay = Math.max(0.0001f, Math.min(0.25f,
                state.preDelaySeconds()));
        float[] coefficients = hp > 0.1f
                ? highPassRbj(hp, sampleRateHz, 0.7f)
                : IDENTITY;
        ByteBuffer result = blob(TAG_VRVB, 0x38, 0);
        result.putInt(state.on() ? 1 : 0);
        // The firmware's On word alone does not reliably silence the shared
        // return. Match the physically verified Host path by also sending a
        // zero wet return while Off. Io24State still retains the user's mix
        // value, so turning the reverb back on restores it.
        result.putFloat(state.on() ? clamp01(state.mix()) : 0.0f);
        result.putInt(preDelay > 0.0001f ? 1 : 0);
        result.putFloat(preDelay);
        result.putFloat(clamp01(state.size()));
        result.putFloat(hp);
        putFloats(result, coefficients);
        return result.array();
    }

    static byte[] highPassFilter(boolean on, float frequency, int sampleRateHz) {
        float cutoff = Math.max(24, Math.min(1000, frequency));
        float[] coefficients = on && cutoff > 24.001f
                ? highPassRbj(cutoff, sampleRateHz, 0.70710678f)
                : IDENTITY;
        return biquadBlob(0, coefficients);
    }

    static List<byte[]> gate(Map<String, Float> values, int sampleRateHz) {
        List<byte[]> result = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            result.add(gateBlob(
                    index,
                    bool(values, "gate_on", false),
                    bool(values, "gate_expander", true),
                    value(values, "gate_threshold", -48),
                    value(values, "gate_range", -48),
                    value(values, "gate_attack", 0.005f),
                    value(values, "gate_release", 0.7f),
                    sampleRateHz));
        }
        return result;
    }

    private static byte[] gateBlob(
            int index,
            boolean on,
            boolean expander,
            float thresholdDb,
            float rangeDb,
            float attackSeconds,
            float releaseSeconds,
            int sampleRateHz) {
        float attack = Math.max(0.00002f, attackSeconds);
        float release = Math.max(0.0001f, releaseSeconds);
        float rate = f32(f32(sampleRateHz) * 0.25f);
        float range = dbToLinear(rangeDb);
        float threshold = dbToInverseLinear(thresholdDb);
        float attackCoefficient = f32(Math.exp(f32(
                f32(f32(-TWO_PI_F / attack) / rate))));
        float kRelease = f32((expander ? 1.0f : 0.5f) * release);
        float releaseCoefficient = f32(Math.exp(f32(
                f32(f32(-TWO_PI_F / kRelease) / rate))));
        int hold = Math.max(1, (int) Math.ceil(f32(
                f32(kRelease * sampleRateHz) * 0.5f)));
        ByteBuffer result = blob(TAG_GATE, 0x48, index);
        putFloats(result, IDENTITY);
        result.putFloat(range).putFloat(threshold);
        result.putFloat(attack).putFloat(release);
        result.putFloat(attackCoefficient).putFloat(releaseCoefficient);
        result.putInt(hold);
        result.putInt(on ? 1 : 0);
        result.putInt(expander ? 1 : 0);
        result.putInt(0); // key listen
        return result.array();
    }

    /** All three host models compile to the same two CPXT instances.
     * Ported from Linux io24_dsp.py cpxt_tube/cpxt_fet, preserving float order.
     */
    static List<byte[]> compressor(Map<String, Float> values, int sampleRateHz) {
        int model = Math.round(value(values, "compressor_model", 0));
        if (model == 0) {
            return standardCompressor(values);
        }
        if (model != 1 && model != 2) {
            throw new IllegalArgumentException("Unknown compressor model " + model);
        }
        float[] sidechain = IDENTITY;
        float attack;
        float release;
        float slope;
        float knee;
        float threshold;
        float makeup;
        if (model == 1) {
            float peak = value(values, "compressor_tube_peak", 0);
            float k = bool(values, "compressor_tube_limit_mode", false)
                    ? 0.0833333358168602f : 0.0555555559694767f;
            threshold = 1.0f - peak * 0.5f;
            slope = peak > 0 ? 1.0f - 1.0f / (peak * k + 2.5f) : 1.0f;
            attack = peak * 0.0005000000237487257f + 0.019999999552965164f;
            release = 0.3799999952316284f;
            knee = 3.0f;
            makeup = dbToLinear((value(values, "compressor_tube_gain", 40) - 40) * 0.5f);
            // Designer type 11 uses w0, not sin(w0), for alpha.
            double w0 = 2.0 * 15000.0 * (Math.PI / sampleRateHz);
            double alpha = 0.5 * w0 / 0.5;
            double a = Math.pow(10, 7.0 * 0.025);
            double norm = a / (a + alpha);
            double c2 = 2.0 * Math.cos(w0);
            sidechain = new float[] {
                    f32((1.0 + a * alpha) * norm) * 0.5f,
                    f32(c2 * norm),
                    f32(-(c2 * norm)) * 0.5f,
                    f32(-((1.0 - alpha / a) * norm)),
                    f32((1.0 - a * alpha) * norm) * 0.5f};
        } else {
            int ratio = Math.max(0, Math.min(4, Math.round(
                    value(values, "compressor_fet_ratio_index", 0))));
            float[] ratios = {6.5f, 9.899999618530273f, 14, 22, 20};
            float[] thresholdOffsets = {3.75f, 2, 1.100000023841858f, 0, 2};
            float[] gainOffsets = {-3, -1.7000000476837158f,
                    -1.2000000476837158f, 0, -1.7000000476837158f};
            attack = value(values, "compressor_fet_attack", 0.0001f)
                    * (ratio == 4 ? 5.0f : 1.0f);
            release = value(values, "compressor_fet_release", 0.25f) * 10.0f;
            slope = 1.0f - 1.0f / ratios[ratio];
            knee = 2.5f;
            float input = -40.0f - value(values, "compressor_fet_input", -43);
            threshold = (input * input) * 0.007860999554395676f
                    + input * 1.5199999809265137f;
            threshold = threshold + 6.130000114440918f - thresholdOffsets[ratio];
            float output = value(values, "compressor_fet_output", 0);
            float gain = (output * 0.005727000068873167f + 1.434999942779541f) * output;
            gain = gain + 6.591000080108643f;
            gain = ((gain - threshold) + 4.0f) + gainOffsets[ratio];
            makeup = dbToLinear(gain);
        }
        List<byte[]> result = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            ByteBuffer blob = blob(TAG_CPXT, 0x40, index);
            putFloats(blob, sidechain);
            blob.putFloat(attack).putFloat(release).putFloat(slope).putFloat(knee);
            blob.putFloat(threshold).putFloat(makeup);
            blob.putInt(bool(values, "compressor_on", false) ? 1 : 0).putInt(0);
            result.add(blob.array());
        }
        return result;
    }

    static List<byte[]> standardCompressor(Map<String, Float> values) {
        boolean on = bool(values, "compressor_on", false);
        List<byte[]> result = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            result.add(compressorBlob(
                    index,
                    on,
                    value(values, "compressor_threshold", -18),
                    value(values, "compressor_ratio", 3),
                    value(values, "compressor_attack", 0.02f),
                    value(values, "compressor_release", 0.15f),
                    value(values, "compressor_gain", 0)));
        }
        return result;
    }

    private static byte[] compressorBlob(
            int index,
            boolean on,
            float thresholdDb,
            float ratio,
            float attack,
            float release,
            float gainDb) {
        float safeRatio = Math.max(1.0f, ratio);
        float slope = f32(1.0f - f32(1.0f / safeRatio));
        ByteBuffer result = blob(TAG_CPXT, 0x40, index);
        putFloats(result, IDENTITY);
        result.putFloat(Math.max(0.0002f, attack));
        result.putFloat(Math.max(0.0025f, release));
        result.putFloat(slope);
        result.putFloat(0.01f);
        result.putFloat(thresholdDb);
        result.putFloat(dbToLinear(gainDb));
        result.putInt(on ? 1 : 0);
        result.putInt(0);
        return result.array();
    }

    static List<byte[]> limiter(Map<String, Float> values, int sampleRateHz) {
        boolean on = bool(values, "limiter_on", false);
        float threshold = value(values, "limiter_threshold", -1);
        List<byte[]> result = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            ByteBuffer blob = blob(BLOCK_LIMITER, 0x18, index);
            blob.putInt(on ? 1 : 0);
            blob.putFloat(on
                    ? dbToInverseLinear(threshold)
                    : Float.intBitsToFloat(0x3faab0d5));
            blob.putFloat(on
                    ? f32(Math.exp(f32(-TWO_PI_F
                            / f32(value(values, "limiter_release", 0.4f) * sampleRateHz))))
                    : 0.0f);
            result.add(blob.array());
        }
        return result;
    }

    /** Native Stat stores the chosen limiter values even while bypassed. */
    static byte[] limiterPresetState(
            Map<String, Float> values,
            int sampleRateHz) {
        ByteBuffer result = ByteBuffer.allocate(12)
                .order(ByteOrder.LITTLE_ENDIAN);
        result.putInt(bool(values, "limiter_on", false) ? 1 : 0);
        result.putFloat(dbToInverseLinear(
                value(values, "limiter_threshold", -1)));
        result.putFloat(f32(Math.exp(f32(-TWO_PI_F
                / f32(value(values, "limiter_release", 0.4f) * sampleRateHz)))));
        return result.array();
    }

    /** Live order: wide section, then three biquads. Vintage reverses the
     * low-mid/high order relative to the native preset table. */
    static float[][] eqSections(Map<String, Float> values, int sampleRateHz) {
        if (sampleRateHz <= 0 || sampleRateHz > 176400) {
            throw new IllegalArgumentException("Unsupported EQ sample rate");
        }
        int model = Math.round(value(values, "eq_model", 0));
        if (model == 0) {
            List<byte[]> blobs = standardEq(values, sampleRateHz);
            float[][] result = new float[4][5];
            for (int band = 0; band < 4; band++) {
                ByteBuffer bytes = ByteBuffer.wrap(blobs.get(band)).order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < 5; i++) result[band][i] = bytes.getFloat(16 + 4 * i);
            }
            return result;
        }
        if (model != 1 && model != 2) throw new IllegalArgumentException("Unknown EQ model");
        if (!bool(values, "eq_on", false)) {
            return new float[][] { {1, 0, 0, 0, 0, 0, 0},
                    IDENTITY.clone(), IDENTITY.clone(), IDENTITY.clone() };
        }
        if (model == 1) {
            String p = "eq_passive_";
            return new float[][] {
                NativeEq.passiveHigh(Math.round(value(values, p + "high_frequency_index", 4)),
                        value(values, p + "high_boost", 1.5f),
                        value(values, p + "high_attenuation", 1.5f),
                        Math.round(value(values, p + "attenuation_frequency_index", 2)),
                        value(values, p + "bandwidth", 10), sampleRateHz),
                NativeEq.passiveLow(Math.round(value(values, p + "low_frequency_index", 0)),
                        value(values, p + "low_boost", 0),
                        value(values, p + "low_attenuation", 0), sampleRateHz),
                IDENTITY.clone(), IDENTITY.clone()
            };
        }
        String p = "eq_vintage_";
        return new float[][] {
            NativeEq.vintage("low", Math.round(value(values, p + "low_frequency_index", 1)),
                    value(values, p + "low_gain", 0), sampleRateHz),
            NativeEq.vintage("high", 0, value(values, p + "high_gain", 0), sampleRateHz),
            NativeEq.vintage("hi-mid", Math.round(value(values, p + "himid_frequency_index", 2)),
                    value(values, p + "himid_gain", 0), sampleRateHz),
            NativeEq.vintage("low-mid", Math.round(value(values, p + "lowmid_frequency_index", 2)),
                    value(values, p + "lowmid_gain", 0), sampleRateHz)
        };
    }

    static List<byte[]> equalizer(Map<String, Float> values, int sampleRateHz) {
        float[][] sections = eqSections(values, sampleRateHz);
        List<byte[]> result = new ArrayList<>();
        for (int band = 0; band < sections.length; band++) {
            result.add(sections[band].length == 7
                    ? wideEqBlob(band, sections[band]) : biquadBlob(band, sections[band]));
        }
        return result;
    }

    static byte[] wideEqBlob(int band, float[] coefficients) {
        if (coefficients.length != 7) throw new IllegalArgumentException("Wide EQ requires seven coefficients");
        for (float coefficient : coefficients) {
            if (!Float.isFinite(coefficient)) throw new IllegalArgumentException("Non-finite EQ coefficient");
        }
        double[] polynomial = {1, -coefficients[1], -coefficients[3], -coefficients[5]};
        while (polynomial.length > 1) {
            int degree = polynomial.length - 1;
            double first = polynomial[0], last = polynomial[degree];
            if (Math.abs(last) >= Math.abs(first)) throw new IllegalArgumentException("Unstable wide EQ");
            double[] next = new double[degree];
            for (int i = 0; i < degree; i++) next[i] = first * polynomial[i] - last * polynomial[degree - i];
            polynomial = next;
        }
        ByteBuffer result = blob(0x4c666466, 0x2c, 0);
        result.putInt(band);
        putFloats(result, coefficients);
        return result.array();
    }

    static List<byte[]> standardEq(Map<String, Float> values, int sampleRateHz) {
        String[] names = {"low", "lowmid", "himid", "high"};
        float[] defaults = {130, 320, 1400, 5000};
        boolean allOn = bool(values, "eq_on", false);
        List<byte[]> result = new ArrayList<>();
        for (int band = 0; band < names.length; band++) {
            String prefix = "eq_" + names[band] + "_";
            float frequency = value(values, prefix + "freq", defaults[band]);
            float gain = value(values, prefix + "gain", 0);
            float q = value(values, prefix + "q", 0.6f);
            boolean bandOn = bool(values, prefix + "on", true);
            boolean shelf = (band == 0 || band == 3)
                    && bool(values, prefix + "shelf", true);
            float[] coefficients = standardEqBandCoefficients(
                    band, frequency, gain, q, allOn && bandOn,
                    shelf, sampleRateHz);
            result.add(biquadBlob(band, coefficients));
        }
        return result;
    }

    /** Exact Standard-EQ designer path shared by wire records and UI response. */
    static float[] standardEqBandCoefficients(
            int band,
            float frequency,
            float gainDb,
            float q,
            boolean on,
            boolean shelf,
            int sampleRateHz) {
        if (band < 0 || band > 3) {
            throw new IllegalArgumentException("Standard EQ band must be 0..3");
        }
        if (sampleRateHz <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive");
        }
        if (!on) {
            return IDENTITY.clone();
        }
        float safeFrequency = Math.max(36.0f, Math.min(
                Math.min(18_000.0f, sampleRateHz * 0.45f), frequency));
        float safeGain = Math.max(-15.0f, Math.min(15.0f, gainDb));
        float safeQ = Math.max(0.1f, Math.min(10.0f, q));
        if (shelf && band == 0) {
            return lowShelfStandard(
                    safeFrequency, safeGain, sampleRateHz, safeQ);
        }
        if (shelf && band == 3) {
            return highShelfStandard(
                    safeFrequency, safeGain, sampleRateHz, safeQ);
        }
        return peakingStandard(
                safeFrequency, safeGain, sampleRateHz, safeQ);
    }

    static byte[] biquadBlob(int band, float[] coefficients) {
        if (coefficients.length != 5) {
            throw new IllegalArgumentException("Biquad requires five coefficients");
        }
        ByteBuffer result = blob(TAG_BQDF, 0x24, 0);
        result.putInt(band);
        putFloats(result, coefficients);
        return result.array();
    }

    private static byte[] multiBiquadBlob(int table, float[][] coefficients) {
        ByteBuffer result = blob(TAG_MBDF, 0x1f4, 0);
        result.putInt(table);
        for (int index = 0; index < coefficients.length; index++) {
            putFloats(result, coefficients[index]);
            result.putFloat(NATIVE_RATES[index]);
        }
        result.position(0x1f0);
        result.putInt(coefficients.length);
        return result.array();
    }

    private static float[] lowPass(float frequency, float sampleRate, float q) {
        float k = f32(Math.tan(f32(f32(frequency)
                * f32(PI_F / f32(sampleRate)))));
        float kk = f32(k * k);
        float n = f32(f32(f32(k / f32(q)) + kk) + 1.0f);
        float b0 = f32(kk / n);
        return new float[] {
                b0,
                f32(f32(f32(1.0f - kk) + f32(1.0f - kk)) / n),
                f32(b0 + b0),
                f32(f32(f32(f32(k / f32(q)) - 1.0f) - kk) / n),
                b0
        };
    }

    private static float[] highPass(float frequency, float sampleRate, float q) {
        float k = f32(Math.tan(f32(f32(frequency)
                * f32(PI_F / f32(sampleRate)))));
        float kk = f32(k * k);
        float n = f32(f32(f32(k / f32(q)) + kk) + 1.0f);
        float b0 = f32(1.0f / n);
        return new float[] {
                b0,
                f32(f32(f32(1.0f - kk) + f32(1.0f - kk)) / n),
                f32(-2.0f / n),
                f32(f32(f32(f32(k / f32(q)) - 1.0f) - kk) / n),
                b0
        };
    }

    private static float[] highPassRbj(
            float frequency, float sampleRate, float q) {
        double k = Math.tan(Math.PI * frequency / sampleRate);
        double n = 1.0 + k / q + k * k;
        return new float[] {
                f32(1.0 / n),
                f32(2.0 * (1.0 - k * k) / n),
                f32(-2.0 / n),
                f32((k / q - 1.0 - k * k) / n),
                f32(1.0 / n)
        };
    }

    private static float[] lowShelfFx(
            float frequency, float gainDb, float sampleRate, float q) {
        float a = f32(Math.pow(10.0, f32(f32(gainDb) * f32(0.025f))));
        float w = f32(f32(f32(frequency) + f32(frequency))
                * f32(PI_F / f32(sampleRate)));
        double sine = Math.sin(w);
        double cosine = Math.cos(w);
        float beta = f32(f32(Math.sqrt(a) / f32(q)) * sine);
        double a0 = (a + 1.0) + (a - 1.0) * cosine + beta;
        return new float[] {
                f32(a * ((a + 1.0) - (a - 1.0) * cosine + beta) / a0),
                f32(2.0 * ((a - 1.0) + (a + 1.0) * cosine) / a0),
                f32(2.0 * a * ((a - 1.0) - (a + 1.0) * cosine) / a0),
                f32(-((a + 1.0) + (a - 1.0) * cosine - beta) / a0),
                f32(a * ((a + 1.0) - (a - 1.0) * cosine - beta) / a0)
        };
    }

    private static float[] peakingStandard(
            float frequency, float gainDb, float sampleRate, float q) {
        float f = f32(frequency);
        float gain = f32(gainDb);
        float quality = f32(q);
        double gainPower = Math.pow(10.0, gain * 0.05);
        double k = Math.tan(f * (Math.PI / sampleRate));
        double k2 = k * k;
        double alphaNumerator = Math.sqrt(gainPower) * k / quality;
        double alphaDenominator = alphaNumerator / gainPower;
        double denominator = alphaDenominator + k2 + 1.0;
        return new float[] {
                f32((k2 + alphaNumerator + 1.0) / denominator),
                f32((2.0 - 2.0 * k2) / denominator),
                f32((2.0 * k2 - 2.0) / denominator),
                f32((alphaDenominator - 1.0 - k2) / denominator),
                f32((k2 - alphaNumerator + 1.0) / denominator)
        };
    }

    private static float[] lowShelfStandard(
            float frequency, float gainDb, float sampleRate, float q) {
        return shelfStandard(frequency, gainDb, sampleRate, q, false);
    }

    private static float[] highShelfStandard(
            float frequency, float gainDb, float sampleRate, float q) {
        return shelfStandard(frequency, gainDb, sampleRate, q, true);
    }

    private static float[] shelfStandard(
            float frequency,
            float gainDb,
            float sampleRate,
            float q,
            boolean high) {
        double f = f32(frequency);
        double gain = f32(gainDb);
        double quality = f32(q);
        double a = Math.pow(10.0, gain * 0.025);
        double w = 2.0 * f * Math.PI / sampleRate;
        double cosine = Math.cos(w);
        double beta = Math.sqrt(a) / quality * Math.sin(w);
        double a0;
        double b0;
        double na1;
        double b1;
        double na2;
        double b2;
        if (high) {
            a0 = (a + 1.0) - (a - 1.0) * cosine + beta;
            b0 = a * ((a + 1.0) + (a - 1.0) * cosine + beta) / a0;
            na1 = -2.0 * ((a - 1.0) - (a + 1.0) * cosine) / a0;
            b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosine) / a0;
            na2 = -((a + 1.0) - (a - 1.0) * cosine - beta) / a0;
            b2 = a * ((a + 1.0) + (a - 1.0) * cosine - beta) / a0;
        } else {
            a0 = (a + 1.0) + (a - 1.0) * cosine + beta;
            b0 = a * ((a + 1.0) - (a - 1.0) * cosine + beta) / a0;
            na1 = 2.0 * ((a - 1.0) + (a + 1.0) * cosine) / a0;
            b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosine) / a0;
            na2 = -((a + 1.0) + (a - 1.0) * cosine - beta) / a0;
            b2 = a * ((a + 1.0) - (a - 1.0) * cosine - beta) / a0;
        }
        return new float[] {
                f32(b0), f32(na1), f32(b1), f32(na2), f32(b2)
        };
    }

    private static float dbToLinear(float db) {
        float rounded = f32(db);
        if (rounded < -144.0f) {
            return Float.intBitsToFloat(0x33877f3f);
        }
        return f32(Math.pow(10.0, f32(rounded * f32(0.05f))));
    }

    private static float dbToInverseLinear(float db) {
        float rounded = f32(db);
        if (rounded > 144.0f) {
            return Float.intBitsToFloat(0x33877f3f);
        }
        return f32(Math.pow(10.0, f32(rounded * f32(-0.05f))));
    }

    private static ByteBuffer blob(int tag, int size, int index) {
        if (size < 12) {
            throw new IllegalArgumentException("Blob size is too small");
        }
        ByteBuffer result = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        result.putInt(tag).putInt(size).putInt(index);
        return result;
    }

    private static void putFloats(ByteBuffer target, float[] values) {
        for (float value : values) {
            target.putFloat(value);
        }
    }

    private static int fourCc(String value) {
        if (value.length() != 4) {
            throw new IllegalArgumentException("FourCC must have four characters");
        }
        return (value.charAt(0) << 24)
                | (value.charAt(1) << 16)
                | (value.charAt(2) << 8)
                | value.charAt(3);
    }

    private static float value(
            Map<String, Float> values,
            String name,
            float fallback) {
        Float result = values.get(name);
        return result == null ? fallback : result;
    }

    private static boolean bool(
            Map<String, Float> values,
            String name,
            boolean fallback) {
        Float result = values.get(name);
        return result == null ? fallback : result >= 0.5f;
    }

    private static float clamp01(float value) {
        return Math.max(0, Math.min(1, value));
    }

    private static float f32(double value) {
        return (float) value;
    }
}
