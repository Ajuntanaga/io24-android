package dev.ajuntanaga.io24;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

public final class EqualizerModelsTest {
    @Test public void allIndependentInstructionInterpreterVectorsMatchExactly() throws Exception {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/native_eq_coefficients.tsv"), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                if (line.startsWith("#")) continue;
                String[] columns = line.split("\t"), args = columns[1].split(",");
                float[] actual;
                switch (columns[0]) {
                    case "vintage":
                        actual = NativeEq.vintage(args[0], Integer.parseInt(args[1]),
                                Float.parseFloat(args[2]), Float.parseFloat(args[3])); break;
                    case "passive_low":
                        actual = NativeEq.passiveLow(Integer.parseInt(args[0]), Float.parseFloat(args[1]),
                                Float.parseFloat(args[2]), Float.parseFloat(args[3])); break;
                    case "passive_high":
                        actual = NativeEq.passiveHigh(Integer.parseInt(args[0]), Float.parseFloat(args[1]),
                                Float.parseFloat(args[2]), Integer.parseInt(args[3]),
                                Float.parseFloat(args[4]), Float.parseFloat(args[5])); break;
                    default: throw new AssertionError(columns[0]);
                }
                ByteBuffer bytes = ByteBuffer.allocate(actual.length * 4).order(ByteOrder.LITTLE_ENDIAN);
                for (float f : actual) bytes.putFloat(f);
                assertEquals(columns[0] + " " + columns[1], columns[2], hex(bytes.array()));
                // The complete production packet must accept the recovered wide section.
                if (actual.length == 7) NativeDsp.wideEqBlob(0, actual);
                count++;
            }
        }
        assertEquals(808, count);
    }

    @Test public void allModelsRouteToInputTwoAndUseTheLinuxLiveLayout() {
        Io24State before = Io24State.defaults().buildUpon()
                .setProcessingParameter(2, "eq_on", 1).build();
        for (int model = 0; model < 3; model++) {
            Io24Command command = Io24Command.setProcessingParameter(2, "eq_model", model);
            Io24State after = Io24StateReducer.apply(before, command);
            Io24Protocol.Transaction tx = Io24Protocol.encodeTransaction(command, before, 254);
            assertEquals(4, tx.writes().size());
            for (int band = 0; band < 4; band++) {
                byte[] blob = NativeDsp.equalizer(after.processing(2), 48000).get(band);
                ByteBuffer b = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
                assertEquals(model > 0 && band == 0 ? 0x4c666466 : 0x42716466, b.getInt(0));
                assertEquals(band, b.getInt(12));
                assertArrayEquals(PaeFrame.buildBlockWrite(new int[] {254, 255, 1, 2}[band], NativeDsp.BLOCK_EQ, 1, blob),
                        tx.writes().get(band).frame());
            }
        }
    }

    @Test public void bypassHasIdentityResponseAndKeepsSavedModelSettings() {
        for (int model = 0; model < 3; model++) {
            Io24State state = configured(model).buildUpon().setProcessingParameter(1, "eq_on", 0).build();
            for (float[] band : NativeDsp.eqSections(state.processing(1), 48000)) {
                float[] identity = new float[band.length]; identity[0] = 1;
                assertArrayEquals(identity, band, 0f);
            }
            float[] curve = GraphMath.eqCoefficientCurve(NativeDsp.eqSections(state.processing(1), 48000), 48000, 300, 120);
            for (float y : curve) assertEquals(60f, y, 0.001f);
            assertEquals(7.5f, state.processing(1).get("eq_passive_low_boost"), 0f);
        }
    }

    @Test public void presetUsesFourRatesAndItsOwnVintageBandOrder() {
        int[] rates = {44100, 48000, 88200, 96000};
        for (int model = 0; model < 3; model++) {
            Io24State state = configured(model);
            byte[] eq = Arrays.copyOfRange(NativePreset.build(state, 1, 0), 568, 1000);
            for (int rate = 0; rate < rates.length; rate++) {
                float[][] live = NativeDsp.eqSections(state.processing(1), rates[rate]);
                int[] order = model == 2 ? new int[] {3, 2, 1} : new int[] {1, 2, 3};
                ByteBuffer bytes = ByteBuffer.wrap(eq).order(ByteOrder.LITTLE_ENDIAN);
                for (int band = 0; band < 3; band++) {
                    for (int coefficient = 0; coefficient < 5; coefficient++)
                        assertEquals(live[order[band]][coefficient], bytes.getFloat(rate * 69 + 4 + band * 20 + coefficient * 4), 0f);
                }
                for (int coefficient = 0; coefficient < 7; coefficient++)
                    assertEquals(coefficient < live[0].length ? live[0][coefficient] : 0,
                            bytes.getFloat(280 + rate * 37 + 4 + coefficient * 4), 0f);
            }
        }
    }

    @Test public void sceneReplayRetainsEachModelsSettings() {
        Io24State original = configured(2);
        Io24State decoded = PresetCodec.decode(PresetCodec.encode("EQ models", original)).state();
        assertEquals(original.processing(1), decoded.processing(1));
        Io24State replay = Io24State.defaults();
        for (Io24Command command : Io24ScenePlanner.commands(decoded)) {
            Io24Protocol.encodeTransaction(command, replay, 1);
            replay = Io24StateReducer.apply(replay, command);
        }
        for (Map.Entry<String, Float> entry : original.processing(1).entrySet())
            assertEquals(entry.getKey(), entry.getValue(), replay.processing(1).get(entry.getKey()));
    }

    @Test public void selectedModelsProduceDifferentFiniteResponses() {
        float[][] curves = new float[3][];
        for (int model = 0; model < 3; model++) {
            curves[model] = GraphMath.eqCoefficientCurve(NativeDsp.eqSections(configured(model).processing(1), 48000), 48000, 300, 120);
            for (float y : curves[model]) assertTrue(Float.isFinite(y));
        }
        assertFalse(Arrays.equals(curves[0], curves[1]));
        assertFalse(Arrays.equals(curves[1], curves[2]));
    }

    private static Io24State configured(int model) {
        return Io24State.defaults().buildUpon().setProcessingParameter(1, "eq_on", 1)
                .setProcessingParameter(1, "eq_model", model)
                .setProcessingParameter(1, "eq_low_gain", 5)
                .setProcessingParameter(1, "eq_passive_low_boost", 7.5f)
                .setProcessingParameter(1, "eq_passive_high_attenuation", 3)
                .setProcessingParameter(1, "eq_vintage_low_gain", -6)
                .setProcessingParameter(1, "eq_vintage_lowmid_gain", 4)
                .setProcessingParameter(1, "eq_vintage_himid_gain", 7)
                .setProcessingParameter(1, "eq_vintage_high_gain", -2).build();
    }
    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) out.append(String.format("%02x", b & 255));
        return out.toString();
    }
}
