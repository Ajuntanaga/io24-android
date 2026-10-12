package dev.ajuntanaga.io24;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PresetCodecTest {
    @Test
    public void completeSemanticStateRoundTripsDeterministically() {
        Io24State original = Io24State.defaults().buildUpon()
                .setInputGain(1, 19.5f)
                .setInputHighPass(1, true)
                .setInputPhantom(2, true)
                .setMixerSend(
                        Io24State.Source.PLAYBACK_3_4,
                        Io24State.Bus.MIX_A,
                        -8.5f,
                        false,
                        0.25f)
                .setProcessingParameter(1, "compressor_ratio", 7.5f)
                .setProcessingParameter(1, "eq_low_on", 1.0f)
                .setProcessingParameter(1, "eq_low_shelf", 0.0f)
                .setProcessingParameter(1, "eq_low_q", 2.75f)
                .setVoiceFxOn(Io24State.VoiceFxModel.FILTERS, true)
                .setVoiceFxParameter(
                        Io24State.VoiceFxModel.FILTERS, "mix", 0.73f)
                .setSelectedVoiceFx(Io24State.VoiceFxModel.FILTERS)
                .setVoiceFxInput(2)
                .setReverb(true, 0.8f, 0.4f, 180.0f, 0.04f)
                .setSampleRateHz(96_000)
                .build();

        String encoded = PresetCodec.encode("Lead \"scene\"", original);
        PresetCodec.Decoded decoded = PresetCodec.decode(encoded);

        assertEquals(encoded, PresetCodec.encode(decoded.name(), decoded.state()));
        assertEquals("Lead \"scene\"", decoded.name());
        assertEquals(19.5f, decoded.state().input(1).gainDb(), 0.0f);
        assertTrue(decoded.state().input(1).highPass());
        assertFalse(encoded.contains("processing.1.hpf_on"));
        assertTrue(decoded.state().input(2).phantom());
        assertEquals(-8.5f, decoded.state().send(
                Io24State.Source.PLAYBACK_3_4,
                Io24State.Bus.MIX_A).db(), 0.0f);
        assertEquals(7.5f,
                decoded.state().processing(1).get("compressor_ratio"), 0.0f);
        assertEquals(1.0f,
                decoded.state().processing(1).get("eq_low_on"), 0.0f);
        assertEquals(0.0f,
                decoded.state().processing(1).get("eq_low_shelf"), 0.0f);
        assertEquals(2.75f,
                decoded.state().processing(1).get("eq_low_q"), 0.0f);
        assertTrue(decoded.state().voiceFx(
                Io24State.VoiceFxModel.FILTERS).on());
        assertEquals(0.73f, decoded.state().voiceFx(
                Io24State.VoiceFxModel.FILTERS).parameter("mix"), 0.0f);
        assertEquals(96_000, decoded.state().sampleRateHz());
        assertTrue(original.sampleRateConfirmed());
        assertFalse(decoded.state().sampleRateConfirmed());
    }

    @Test
    public void malformedAndFutureSchemasAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PresetCodec.decode("not json"));
        assertThrows(IllegalArgumentException.class,
                () -> PresetCodec.decode("{\"schema\":99,\"name\":\"x\"}"));
    }
}
