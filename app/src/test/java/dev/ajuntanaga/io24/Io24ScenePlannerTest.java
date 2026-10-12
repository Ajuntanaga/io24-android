package dev.ajuntanaga.io24;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public final class Io24ScenePlannerTest {
    @Test
    public void scenesDoNotChangeTheCurrentAudioRateOrFrontPanelBlock() {
        Io24State target = Io24State.defaults().buildUpon()
                .setRememberedSampleRateHz(96_000)
                .setInputPresetSlot(1, 1)
                .setInputPresetSlot(2, 3)
                .build();

        List<Io24Command> commands = Io24ScenePlanner.commands(target);

        assertFalse(has(commands, Io24Command.Kind.SET_SAMPLE_RATE));
        assertFalse(has(commands, Io24Command.Kind.SET_PRESET_SLOT));
    }

    @Test
    public void aDelaySceneAlwaysCarriesSemanticModelIntent() {
        Io24State target = Io24State.defaults().buildUpon()
                .setRememberedSampleRateHz(96_000)
                .setSelectedVoiceFx(Io24State.VoiceFxModel.DELAY)
                .build();

        List<Io24Command> commands = Io24ScenePlanner.commands(target);

        assertTrue(commands.stream().anyMatch(command ->
                command.kind() == Io24Command.Kind.SET_VOICE_FX_MODEL
                        && command.model() == Io24State.VoiceFxModel.DELAY));
    }

    @Test
    public void aSceneUsesOnlyTheSharedChannelHighPassPowerCommand() {
        Io24State target = Io24State.defaults().buildUpon()
                .setInputHighPass(1, true)
                .setProcessingParameter(1, "hpf_hz", 406.0f)
                .build();

        List<Io24Command> commands = Io24ScenePlanner.commands(target);

        assertTrue(commands.stream().anyMatch(command ->
                command.kind() == Io24Command.Kind.SET_HIGH_PASS
                        && command.channel() == 1
                        && command.toggle()));
        assertFalse(commands.stream().anyMatch(command ->
                command.kind() == Io24Command.Kind.SET_PROCESSING_PARAMETER
                        && "hpf_on".equals(command.parameter())));
        assertTrue(commands.stream().anyMatch(command ->
                command.kind() == Io24Command.Kind.SET_PROCESSING_PARAMETER
                        && "hpf_hz".equals(command.parameter())
                        && command.value() == 406.0f));
        int cutoff = indexOf(commands, Io24Command.Kind.SET_PROCESSING_PARAMETER,
                "hpf_hz");
        int power = indexOf(commands, Io24Command.Kind.SET_HIGH_PASS, null);
        assertTrue("A scene must program HPF cutoff before enabling power",
                cutoff >= 0 && cutoff < power);
    }

    private static int indexOf(
            List<Io24Command> commands,
            Io24Command.Kind kind,
            String parameter) {
        for (int index = 0; index < commands.size(); index++) {
            Io24Command command = commands.get(index);
            if (command.kind() == kind
                    && (parameter == null
                    || parameter.equals(command.parameter()))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean has(
            List<Io24Command> commands,
            Io24Command.Kind kind) {
        return commands.stream().anyMatch(command -> command.kind() == kind);
    }
}
