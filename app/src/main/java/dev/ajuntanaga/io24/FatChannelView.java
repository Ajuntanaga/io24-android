package dev.ajuntanaga.io24;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Full two-channel dynamics and EQ editor. */
final class FatChannelView implements ControllerView {
    private final View root;
    private final CommandSink sink;
    private final Spinner inputSelector;
    private final Switch highPassOn;
    private final ParameterSlider highPassFrequency;
    private final Switch gateOn;
    private final ParameterSlider gateThreshold;
    private final ParameterSlider gateRange;
    private final ParameterSlider gateAttack;
    private final ParameterSlider gateRelease;
    private final Spinner compressorModel;
    private final Switch compressorOn;
    private final ParameterSlider compressorThreshold;
    private final ParameterSlider compressorRatio;
    private final ParameterSlider compressorAttack;
    private final ParameterSlider compressorRelease;
    private final ParameterSlider compressorGain;
    private final DynamicsGraphView dynamicsGraph;
    private final Switch limiterOn;
    private final ParameterSlider limiterThreshold;
    private final Switch eqOn;
    private final Spinner eqBandSelector;
    private final Switch[] eqBandOn = new Switch[4];
    private final Switch[] eqShelf = new Switch[4];
    private final ParameterSlider[] eqFrequencies = new ParameterSlider[4];
    private final ParameterSlider[] eqGains = new ParameterSlider[4];
    private final ParameterSlider[] eqQ = new ParameterSlider[4];
    private final LinearLayout[] eqBandPanels = new LinearLayout[4];
    private final EqGraphView eqGraph;
    private final Switch eqFirst;
    private final Map<String, ParameterSlider> sliders = new LinkedHashMap<>();
    private Io24State lastState = Io24State.defaults();
    private int channel = 1;
    private boolean rendering;

    FatChannelView(Context context, CommandSink sink) {
        this.sink = sink;
        LinearLayout content = Ui.column(context);
        content.setPadding(Ui.dp(context, 16), Ui.dp(context, 12),
                Ui.dp(context, 16), Ui.dp(context, 24));
        content.addView(Ui.title(context, "Fat Channel"), Ui.match());
        content.addView(Ui.caption(context,
                "Gate, compression, limiting, and equalization for one input."),
                Ui.spaced(context));

        inputSelector = spinner(context, new String[] {"Input 1", "Input 2"});
        inputSelector.setContentDescription("Fat Channel input");
        inputSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                channel = position + 1;
                render(lastState);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        content.addView(inputSelector, Ui.spaced(context));

        LinearLayout filter = Ui.panel(context);
        filter.addView(Ui.section(context, "High-pass filter"), Ui.match());
        highPassOn = Ui.toggle(context, "On");
        highPassOn.setContentDescription("Channel high-pass power");
        highPassOn.setOnCheckedChangeListener((button, checked) -> {
            if (!rendering) {
                sink.dispatch(Io24Command.setHighPass(channel, checked));
            }
        });
        filter.addView(highPassOn, Ui.match());
        highPassFrequency = slider(
                context, filter, "hpf_hz", "Cutoff", 24, 1000, 244,
                ParameterSlider.Scale.LOG, Ui::hz);
        content.addView(filter, Ui.spaced(context));

        LinearLayout gate = Ui.panel(context);
        gate.addView(Ui.section(context, "Gate / Expander"), Ui.match());
        gateOn = toggle(context, "On", "gate_on");
        gate.addView(gateOn, Ui.match());
        gateThreshold = slider(context, gate, "gate_threshold", "Threshold",
                -84, 0, 168, ParameterSlider.Scale.LINEAR, Ui::db);
        gateRange = slider(context, gate, "gate_range", "Range",
                -84, 0, 168, ParameterSlider.Scale.LINEAR, Ui::db);
        gateAttack = slider(context, gate, "gate_attack", "Attack",
                0.00002f, 0.5f, 250, ParameterSlider.Scale.LOG,
                value -> millis(value));
        gateRelease = slider(context, gate, "gate_release", "Release",
                0.05f, 2.0f, 195, ParameterSlider.Scale.LOG,
                value -> millis(value));
        content.addView(gate, Ui.spaced(context));

        LinearLayout compressor = Ui.panel(context);
        compressor.addView(Ui.section(context, "Compressor"), Ui.match());
        compressorModel = spinner(
                context, new String[] {"Standard"});
        compressorModel.setContentDescription("Compressor model");
        compressorModel.setOnItemSelectedListener(parameterSpinner(
                "compressor_model"));
        compressor.addView(compressorModel, Ui.match());
        compressor.addView(Ui.caption(context,
                "Exact Standard model controls."), Ui.match());
        compressorOn = toggle(context, "On", "compressor_on");
        compressor.addView(compressorOn, Ui.match());
        dynamicsGraph = new DynamicsGraphView(context);
        compressor.addView(dynamicsGraph, Ui.spaced(context));
        compressorThreshold = slider(
                context, compressor, "compressor_threshold", "Threshold",
                -56, 0, 112, ParameterSlider.Scale.LINEAR, Ui::db);
        compressorRatio = slider(
                context, compressor, "compressor_ratio", "Ratio",
                1, 20, 190, ParameterSlider.Scale.LINEAR,
                value -> String.format(java.util.Locale.US, "%.1f:1", value));
        compressorAttack = slider(
                context, compressor, "compressor_attack", "Attack",
                0.0002f, 0.15f, 250, ParameterSlider.Scale.LOG,
                value -> millis(value));
        compressorRelease = slider(
                context, compressor, "compressor_release", "Release",
                0.0025f, 0.9f, 250, ParameterSlider.Scale.LOG,
                value -> millis(value));
        compressorGain = slider(
                context, compressor, "compressor_gain", "Makeup",
                0, 28, 112, ParameterSlider.Scale.LINEAR, Ui::db);
        content.addView(compressor, Ui.spaced(context));

        LinearLayout limiter = Ui.panel(context);
        limiter.addView(Ui.section(context, "Limiter"), Ui.match());
        limiterOn = toggle(context, "On", "limiter_on");
        limiter.addView(limiterOn, Ui.match());
        limiterThreshold = slider(
                context, limiter, "limiter_threshold", "Ceiling",
                -28, 0, 112, ParameterSlider.Scale.LINEAR, Ui::db);
        content.addView(limiter, Ui.spaced(context));

        LinearLayout equalizer = Ui.panel(context);
        LinearLayout eqHeader = Ui.row(context);
        eqHeader.addView(Ui.section(context, "Standard EQ"), Ui.weight(1));
        eqOn = eqToggle(context, "On", "eq_on");
        eqHeader.addView(eqOn);
        equalizer.addView(eqHeader, Ui.match());
        equalizer.addView(Ui.caption(context,
                "Four independent bands. Drag a graph node for frequency and gain."),
                Ui.match());
        eqGraph = new EqGraphView(context);
        equalizer.addView(eqGraph, Ui.spaced(context));

        String[] bandNames = {"Low", "Low mid", "High mid", "High"};
        String[] keys = {"low", "lowmid", "himid", "high"};
        eqBandSelector = spinner(context, bandNames);
        eqBandSelector.setContentDescription("Band");
        LinearLayout bandRow = Ui.row(context);
        bandRow.addView(Ui.label(context, "Band"));
        bandRow.addView(eqBandSelector, Ui.weight(1));
        equalizer.addView(bandRow, Ui.spaced(context));

        for (int index = 0; index < 4; index++) {
            LinearLayout band = Ui.column(context);
            band.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
            eqBandPanels[index] = band;
            eqBandOn[index] = eqToggle(
                    context, "Band on", "eq_" + keys[index] + "_on");
            band.addView(eqBandOn[index], Ui.match());
            if (index == 0 || index == 3) {
                eqShelf[index] = eqToggle(
                        context, "Peak / shelf",
                        "eq_" + keys[index] + "_shelf");
                band.addView(eqShelf[index], Ui.match());
                band.addView(Ui.caption(context,
                        "Off is parametric peak; On is "
                                + (index == 0 ? "low shelf." : "high shelf.")),
                        Ui.match());
            }
            eqFrequencies[index] = slider(
                    context, band, "eq_" + keys[index] + "_freq",
                    "Frequency", 36, 18000, 512,
                    ParameterSlider.Scale.LOG, Ui::hz);
            eqGains[index] = slider(
                    context, band, "eq_" + keys[index] + "_gain",
                    "Gain", -15, 15, 120,
                    ParameterSlider.Scale.LINEAR, Ui::db);
            eqQ[index] = slider(
                    context, band, "eq_" + keys[index] + "_q",
                    "Q", 0.1f, 10, 198,
                    ParameterSlider.Scale.LOG,
                    value -> String.format(java.util.Locale.US, "%.2f", value));
            equalizer.addView(band, Ui.match());
        }
        eqBandSelector.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        showEqBand(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
        eqGraph.setListener(new EqGraphView.Listener() {
            @Override
            public void selected(int band) {
                eqBandSelector.setSelection(band);
            }

            @Override
            public void changed(
                    int band,
                    float frequency,
                    float gainDb,
                    boolean committed) {
                eqFrequencies[band].setValue(frequency);
                eqGains[band].setValue(gainDb);
                updateGraphs();
                if (committed && !rendering) {
                    // Dispatch is synchronous in the emulator and may render
                    // after each write. Capture both parts of the gesture
                    // before the frequency write can refresh these sliders.
                    float committedFrequency = eqFrequencies[band].value();
                    float committedGain = eqGains[band].value();
                    sink.dispatchAll(List.of(
                            Io24Command.setProcessingParameter(
                                    channel, "eq_" + keys[band] + "_freq",
                                    committedFrequency),
                            Io24Command.setProcessingParameter(
                                    channel, "eq_" + keys[band] + "_gain",
                                    committedGain)),
                            "EQ band " + (band + 1) + " graph edit");
                }
            }
        });
        eqFirst = toggle(context, "EQ before compressor", "eq_first");
        equalizer.addView(eqFirst, Ui.spaced(context));
        content.addView(equalizer, Ui.spaced(context));
        root = Ui.scroll(context, content);
    }

    @Override
    public View view() {
        return root;
    }

    @Override
    public void render(Io24State state) {
        lastState = state;
        boolean enabled = state.connectionStatus()
                == Io24State.ConnectionStatus.CONNECTED && !state.busy();
        Map<String, Float> values = state.processing(channel);
        rendering = true;
        highPassOn.setChecked(state.input(channel).highPass());
        highPassFrequency.setValue(value(values, "hpf_hz", 80));
        gateOn.setChecked(on(values, "gate_on"));
        gateThreshold.setValue(value(values, "gate_threshold", -48));
        gateRange.setValue(value(values, "gate_range", -48));
        gateAttack.setValue(value(values, "gate_attack", 0.005f));
        gateRelease.setValue(value(values, "gate_release", 0.7f));
        compressorModel.setSelection(0, false);
        compressorOn.setChecked(on(values, "compressor_on"));
        compressorThreshold.setValue(
                value(values, "compressor_threshold", -18));
        compressorRatio.setValue(value(values, "compressor_ratio", 3));
        compressorAttack.setValue(value(values, "compressor_attack", 0.02f));
        compressorRelease.setValue(
                value(values, "compressor_release", 0.15f));
        compressorGain.setValue(value(values, "compressor_gain", 0));
        limiterOn.setChecked(on(values, "limiter_on"));
        limiterThreshold.setValue(value(values, "limiter_threshold", -1));
        eqOn.setChecked(on(values, "eq_on"));
        String[] keys = {"low", "lowmid", "himid", "high"};
        float[] frequencyDefaults = {130, 320, 1400, 5000};
        for (int index = 0; index < 4; index++) {
            eqBandOn[index].setChecked(onDefault(
                    values, "eq_" + keys[index] + "_on", true));
            if (eqShelf[index] != null) {
                eqShelf[index].setChecked(onDefault(
                        values, "eq_" + keys[index] + "_shelf", true));
            }
            eqFrequencies[index].setValue(value(
                    values, "eq_" + keys[index] + "_freq",
                    frequencyDefaults[index]));
            eqGains[index].setValue(value(
                    values, "eq_" + keys[index] + "_gain", 0));
            eqQ[index].setValue(value(
                    values, "eq_" + keys[index] + "_q", 0.6f));
        }
        eqFirst.setChecked(on(values, "eq_first"));
        rendering = false;
        updateGraphs();
        highPassOn.setEnabled(enabled);
        compressorModel.setEnabled(enabled);
        eqBandSelector.setEnabled(enabled);
        eqGraph.setEnabled(enabled);
        for (ParameterSlider slider : sliders.values()) {
            Ui.enabledTree(slider, enabled);
        }
        for (Switch toggle : new Switch[] {
                gateOn, compressorOn, limiterOn, eqOn, eqFirst}) {
            toggle.setEnabled(enabled);
        }
        for (int index = 0; index < 4; index++) {
            eqBandOn[index].setEnabled(enabled);
            if (eqShelf[index] != null) {
                eqShelf[index].setEnabled(enabled);
            }
        }
    }

    private ParameterSlider slider(
            Context context,
            LinearLayout parent,
            String key,
            String name,
            float min,
            float max,
            int steps,
            ParameterSlider.Scale scale,
            ParameterSlider.Formatter formatter) {
        ParameterSlider slider = new ParameterSlider(
                context, name, min, max, steps, scale, formatter);
        slider.setListener((value, committed) -> {
            updateGraphs();
            if (committed && !rendering) {
                sink.dispatch(Io24Command.setProcessingParameter(
                        channel, key, value));
            }
        });
        sliders.put(key, slider);
        parent.addView(slider, Ui.spaced(context));
        return slider;
    }

    private Switch toggle(Context context, String label, String key) {
        Switch result = Ui.toggle(context, label);
        result.setContentDescription(label + " " + key.replace('_', ' '));
        result.setOnCheckedChangeListener((button, checked) -> {
            if (!rendering) {
                sink.dispatch(Io24Command.setProcessingParameter(
                        channel, key, checked ? 1.0f : 0.0f));
            }
        });
        return result;
    }

    private Switch eqToggle(Context context, String label, String key) {
        Switch result = Ui.toggle(context, label);
        result.setContentDescription(label + " " + key.replace('_', ' '));
        result.setOnCheckedChangeListener((button, checked) -> {
            updateGraphs();
            if (!rendering) {
                sink.dispatch(Io24Command.setProcessingParameter(
                        channel, key, checked ? 1.0f : 0.0f));
            }
        });
        return result;
    }

    private AdapterView.OnItemSelectedListener parameterSpinner(String key) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                if (!rendering) {
                    sink.dispatch(Io24Command.setProcessingParameter(
                            channel, key, position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    private void updateGraphs() {
        dynamicsGraph.setDynamics(
                compressorThreshold.value(),
                compressorRatio.value(),
                compressorGain.value());
        float[] frequencies = new float[4];
        float[] gains = new float[4];
        float[] q = new float[4];
        boolean[] bandOn = new boolean[4];
        boolean[] shelf = new boolean[4];
        for (int index = 0; index < 4; index++) {
            frequencies[index] = eqFrequencies[index].value();
            gains[index] = eqGains[index].value();
            q[index] = eqQ[index].value();
            bandOn[index] = eqBandOn[index].isChecked();
            shelf[index] = eqShelf[index] != null
                    && eqShelf[index].isChecked();
        }
        eqGraph.setEq(
                frequencies,
                gains,
                q,
                bandOn,
                shelf,
                lastState.sampleRateHz(),
                eqOn.isChecked());
    }

    private void showEqBand(int selected) {
        int safe = Math.max(0, Math.min(3, selected));
        for (int index = 0; index < eqBandPanels.length; index++) {
            eqBandPanels[index].setVisibility(
                    index == safe ? View.VISIBLE : View.GONE);
        }
        eqGraph.setSelectedBand(safe);
    }

    private static Spinner spinner(Context context, String[] labels) {
        Spinner spinner = new Spinner(context);
        spinner.setMinimumHeight(Ui.dp(context, 48));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private static boolean on(Map<String, Float> values, String key) {
        return value(values, key, 0) >= 0.5f;
    }

    private static boolean onDefault(
            Map<String, Float> values,
            String key,
            boolean fallback) {
        return value(values, key, fallback ? 1.0f : 0.0f) >= 0.5f;
    }

    private static float value(
            Map<String, Float> values,
            String key,
            float fallback) {
        Float result = values.get(key);
        return result == null ? fallback : result;
    }

    private static String millis(float seconds) {
        return String.format(java.util.Locale.US, "%.1f ms", seconds * 1000.0f);
    }
}
