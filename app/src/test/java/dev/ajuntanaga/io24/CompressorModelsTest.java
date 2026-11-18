package dev.ajuntanaga.io24;

import static org.junit.Assert.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

/** Independent vectors from Linux io24_dsp cpxt_tube/cpxt_fet and native Stat layout. */
public final class CompressorModelsTest {
    @Test public void tubeDefaultMatchesLinuxAtAllRatesAndInPreset() throws Exception {
        Io24State state = Io24State.defaults().buildUpon()
                .setProcessingParameter(1, "compressor_model", 1).build();
        assertEquals("aa99fc8ac5744a8e7cc420112488c476ceddb8d282d5d105dfe8962a42425767", digest(NativeDsp.compressor(state.processing(1), 44100).get(0)));
        assertEquals("74b9804f85d3db302bc9e884b552763a86a30acca0290b0b39473dd77de99524", digest(NativeDsp.compressor(state.processing(1), 48000).get(0)));
        assertEquals("c7297feb9727c65e9745483567f5b21b07ed23cbdc0a74d75e077ed17dffa516", digest(NativeDsp.compressor(state.processing(1), 88200).get(0)));
        assertEquals("d54f3b051b6526e653f526218a538bbcba677c0216c0ff12a89161867fee7232", digest(NativeDsp.compressor(state.processing(1), 96000).get(0)));
        assertEquals("9aeb482b7680c9e36bca6bf4ecc3ce904264c2d916bef8ec793ec91aa85b1cce", digest(NativeDsp.compressor(state.processing(1), 48000).get(1)));
        assertEquals("75de3894530d7d2c841139e583ec9cc21ed04010b2295fe4398ac23bd816aef0", digest(Arrays.copyOfRange(NativePreset.build(state, 1, 0), 340, 568)));
    }

    @Test public void tubeLimitMatchesLinuxAtAllRatesAndInPreset() throws Exception {
        Io24State state = Io24State.defaults().buildUpon()
                .setProcessingParameter(1, "compressor_model", 1)
                .setProcessingParameter(1, "compressor_tube_peak", 67.5f)
                .setProcessingParameter(1, "compressor_tube_gain", 53)
                .setProcessingParameter(1, "compressor_tube_limit_mode", 1)
                .setProcessingParameter(1, "compressor_on", 1).build();
        assertEquals("00edeb7cdc9d16986630fe5be6f2931a94cf11afe29952cf452ca2f87510ce72", digest(NativeDsp.compressor(state.processing(1), 44100).get(0)));
        assertEquals("753113acb950a92ac2712b7338a743137373cbda99d70309c0de3734702ef4a7", digest(NativeDsp.compressor(state.processing(1), 48000).get(0)));
        assertEquals("1be9763f00c3837b6d56d169dade8758a323793d20afffcfe786a17ac024d1bb", digest(NativeDsp.compressor(state.processing(1), 88200).get(0)));
        assertEquals("802bf95138cfe6477a2cf304ff84c565799cff820194b7ce33dbc419b6402193", digest(NativeDsp.compressor(state.processing(1), 96000).get(0)));
        assertEquals("d08b48cbc06791152c9aeb3f4657a9cc7f078e425897565827ea12baa2d5652b", digest(NativeDsp.compressor(state.processing(1), 48000).get(1)));
        assertEquals("69b6b46ce553623025c8310a23366a176e7ed35f1f7d59e4954b135a3a35affe", digest(Arrays.copyOfRange(NativePreset.build(state, 1, 0), 340, 568)));
    }

    @Test public void fetDefaultMatchesLinuxAtAllRatesAndInPreset() throws Exception {
        Io24State state = Io24State.defaults().buildUpon()
                .setProcessingParameter(1, "compressor_model", 2).build();
        assertEquals("8958a60a3c85b992593043dcaeb8fc145e40a49e74d551fbe0e7efc1ea491d50", digest(NativeDsp.compressor(state.processing(1), 44100).get(0)));
        assertEquals("8958a60a3c85b992593043dcaeb8fc145e40a49e74d551fbe0e7efc1ea491d50", digest(NativeDsp.compressor(state.processing(1), 48000).get(0)));
        assertEquals("8958a60a3c85b992593043dcaeb8fc145e40a49e74d551fbe0e7efc1ea491d50", digest(NativeDsp.compressor(state.processing(1), 88200).get(0)));
        assertEquals("8958a60a3c85b992593043dcaeb8fc145e40a49e74d551fbe0e7efc1ea491d50", digest(NativeDsp.compressor(state.processing(1), 96000).get(0)));
        assertEquals("e8e621d2b449f63f4ffc13a7cb9001c254fcf16c88112951a0a4df1cf2ea88e7", digest(NativeDsp.compressor(state.processing(1), 48000).get(1)));
        assertEquals("e0fb5d1f1ff4364100872c047bdc86df7a0474745e2f9a7e143d3f3e2dd534a2", digest(Arrays.copyOfRange(NativePreset.build(state, 1, 0), 340, 568)));
    }

    @Test public void fetAllMatchesLinuxAtAllRatesAndInPreset() throws Exception {
        Io24State state = Io24State.defaults().buildUpon()
                .setProcessingParameter(1, "compressor_model", 2)
                .setProcessingParameter(1, "compressor_fet_input", -31.25f)
                .setProcessingParameter(1, "compressor_fet_output", -7.75f)
                .setProcessingParameter(1, "compressor_fet_attack", 0.000021f)
                .setProcessingParameter(1, "compressor_fet_release", 0.05f)
                .setProcessingParameter(1, "compressor_fet_ratio_index", 4)
                .setProcessingParameter(1, "compressor_on", 1).build();
        assertEquals("2ee3d537a1e4412d2181f4e162de112685213deff7c0427d2b5b4f24ed233baf", digest(NativeDsp.compressor(state.processing(1), 44100).get(0)));
        assertEquals("2ee3d537a1e4412d2181f4e162de112685213deff7c0427d2b5b4f24ed233baf", digest(NativeDsp.compressor(state.processing(1), 48000).get(0)));
        assertEquals("2ee3d537a1e4412d2181f4e162de112685213deff7c0427d2b5b4f24ed233baf", digest(NativeDsp.compressor(state.processing(1), 88200).get(0)));
        assertEquals("2ee3d537a1e4412d2181f4e162de112685213deff7c0427d2b5b4f24ed233baf", digest(NativeDsp.compressor(state.processing(1), 96000).get(0)));
        assertEquals("ff593f05400d288009a421d31e01e08976fc565b27e88715b6064e7a43b40e2c", digest(NativeDsp.compressor(state.processing(1), 48000).get(1)));
        assertEquals("6a88a770b3b46b0034d91e7b1817efc0eda4b4cd8c901d7a0f7799ae9a2a52ba", digest(Arrays.copyOfRange(NativePreset.build(state, 1, 0), 340, 568)));
    }

    @Test public void everyFetRatioHasItsOwnLinuxVector() throws Exception {
        String[] expected = {
            "a4741787e8edf6448a6eef8bd5ae340cf9302cbbb660d97517045ebb32d22c29",
            "01434607aee5d7c7b3ff9691506ad16fffd0e85229003cf379ef63249299c09e",
            "38a4f48bc35a65f0630738a3bd287e6eaa99b474435839b6e45fe53b94281037",
            "7fca1f6dd10fe562e69eb7ca6548c0ae0a71af891261d6efb1fe75e18b5279e4",
            "6daf27fb92eb6da3e7c8fc64684948726104f1a32a1c601da155382255bce83b",
        };
        for (int i = 0; i < expected.length; i++) {
            Io24State state = Io24State.defaults().buildUpon()
                    .setProcessingParameter(1, "compressor_model", 2)
                    .setProcessingParameter(1, "compressor_on", 1)
                    .setProcessingParameter(1, "compressor_fet_ratio_index", i).build();
            assertEquals(expected[i], digest(NativeDsp.compressor(state.processing(1), 48000).get(0)));
        }
    }

    @Test public void modelSelectionWritesBothStatesToTheSelectedInput() {
        Io24State before = Io24State.defaults();
        for (int model = 0; model < 3; model++) {
            Io24Command command = Io24Command.setProcessingParameter(2, "compressor_model", model);
            Io24State after = Io24StateReducer.apply(before, command);
            Io24Protocol.Transaction tx = Io24Protocol.encodeTransaction(command, before, 254);
            assertEquals(2, tx.writes().size());
            for (int index = 0; index < 2; index++) {
                assertArrayEquals(PaeFrame.buildBlockWrite(254 + index, NativeDsp.BLOCK_COMPRESSOR, 1,
                        NativeDsp.compressor(after.processing(2), 48000).get(index)),
                        tx.writes().get(index).frame());
            }
        }
    }

    @Test public void scenePreservesAllModelSettingsAndSelectedModel() {
        Io24State original = Io24State.defaults().buildUpon()
                .setProcessingParameter(1, "compressor_model", 2)
                .setProcessingParameter(1, "compressor_threshold", -23)
                .setProcessingParameter(1, "compressor_tube_peak", 68)
                .setProcessingParameter(1, "compressor_tube_gain", 51)
                .setProcessingParameter(1, "compressor_tube_limit_mode", 1)
                .setProcessingParameter(1, "compressor_fet_input", -35)
                .setProcessingParameter(1, "compressor_fet_output", -7)
                .setProcessingParameter(1, "compressor_fet_attack", 0.000021f)
                .setProcessingParameter(1, "compressor_fet_release", 1.1f)
                .setProcessingParameter(1, "compressor_fet_ratio_index", 4).build();
        Io24State decoded = PresetCodec.decode(PresetCodec.encode("Models", original)).state();
        assertEquals(original.processing(1), decoded.processing(1));
        Io24State replay = Io24State.defaults();
        for (Io24Command command : Io24ScenePlanner.commands(decoded)) {
            Io24Protocol.encodeTransaction(command, replay, 1);
            replay = Io24StateReducer.apply(replay, command);
        }
        // Scene replay also materializes scalar defaults such as mute_sync.
        // Every saved processing value must survive those extra defaults.
        for (Map.Entry<String, Float> entry : original.processing(1).entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(), replay.processing(1).get(entry.getKey()));
        }
    }

    private static String digest(byte[] bytes) throws Exception {
        StringBuilder out = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) out.append(String.format("%02x", b & 255));
        return out.toString();
    }
}
