package dev.ajuntanaga.io24;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
    private final Switch gateExpander;
    private final ParameterSlider gateThreshold;
    private final ParameterSlider gateRange;
    private final ParameterSlider gateAttack;
    private final ParameterSlider gateRelease;
    private final Spinner compressorModel;
    private final Switch compressorOn;
    private final LinearLayout[] compressorPanels = new LinearLayout[3];
    private final Switch tubeLimitMode;
    private final Spinner fetRatio;
    private final ParameterSlider compressorThreshold;
    private final ParameterSlider compressorRatio;
    private final ParameterSlider compressorAttack;
    private final ParameterSlider compressorRelease;
    private final ParameterSlider compressorGain;
    private final DynamicsGraphView dynamicsGraph;
    private final Switch limiterOn;
    private final ParameterSlider limiterThreshold;
    private final ParameterSlider limiterRelease;
    private final Switch eqOn;
    private final Spinner eqModel;
    private final LinearLayout[] eqPanels = new LinearLayout[3];
    private final Map<String, Spinner> eqChoices = new LinkedHashMap<>();
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
    private static final Map<String, Float> PROCESSING_DEFAULTS = Io24State.defaults().processing(1);
    private Io24State lastState = Io24State.defaults();
    private int channel = 1;
    private boolean rendering;
    private final LinearLayout[] modules = new LinearLayout[5];
    private final FatModuleNode[] moduleButtons = new FatModuleNode[5];
    private final LinearLayout moduleChain;
    private int selectedModule;

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

        String[] moduleNames = {"HPF", "Gate", "Compressor", "Limiter", "Equaliser"};
        moduleChain = Ui.row(context);
        android.widget.HorizontalScrollView chainScroll = new android.widget.HorizontalScrollView(context);
        chainScroll.setHorizontalScrollBarEnabled(true);
        for (int index = 0; index < moduleNames.length; index++) {
            final int module = index;
            FatModuleNode node = new FatModuleNode(context, index, moduleNames[index]);
            node.setOnClickListener(view -> showModule(module));
            moduleButtons[index] = node;
        }
        arrangeModules(false);
        chainScroll.addView(moduleChain);
        content.addView(chainScroll, Ui.spaced(context));

        LinearLayout filter = Ui.panel(context);
        modules[0] = filter;
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
        Ui.styleRack(filter);
        content.addView(filter, Ui.spaced(context));

        LinearLayout gate = Ui.panel(context);
        modules[1] = gate;
        gate.addView(Ui.section(context, "Gate / Expander"), Ui.match());
        gateOn = toggle(context, "On", "gate_on");
        gate.addView(gateOn, Ui.match());
        gateExpander = toggle(context, "Expander", "gate_expander");
        gate.addView(gateExpander, Ui.match());
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
        Ui.styleRack(gate);
        content.addView(gate, Ui.spaced(context));

        LinearLayout compressor = Ui.panel(context);
        modules[2] = compressor;
        compressor.addView(Ui.section(context, "Compressor"), Ui.match());
        compressorModel = spinner(
                context, new String[] {"Standard", "Tube", "FET"});
        compressorModel.setContentDescription("Compressor model");
        compressorModel.setOnItemSelectedListener(parameterSpinner(
                "compressor_model"));
        compressor.addView(compressorModel, Ui.match());
        compressor.addView(Ui.caption(context,
                "Audio is processed by the io24."), Ui.match());
        compressorOn = toggle(context, "On", "compressor_on");
        compressor.addView(compressorOn, Ui.match());
        dynamicsGraph = new DynamicsGraphView(context);
        compressor.addView(dynamicsGraph, Ui.spaced(context));
        LinearLayout standard = Ui.column(context);
        compressorPanels[0] = standard;
        compressorThreshold = slider(
                context, standard, "compressor_threshold", "Threshold",
                -56, 0, 112, ParameterSlider.Scale.LINEAR, Ui::db);
        compressorRatio = slider(
                context, standard, "compressor_ratio", "Ratio",
                1, 20, 190, ParameterSlider.Scale.LINEAR,
                value -> String.format(java.util.Locale.US, "%.1f:1", value));
        compressorAttack = slider(
                context, standard, "compressor_attack", "Attack",
                0.0002f, 0.15f, 250, ParameterSlider.Scale.LOG,
                value -> millis(value));
        compressorRelease = slider(
                context, standard, "compressor_release", "Release",
                0.0025f, 0.9f, 250, ParameterSlider.Scale.LOG,
                value -> millis(value));
        compressorGain = slider(
                context, standard, "compressor_gain", "Makeup",
                0, 28, 112, ParameterSlider.Scale.LINEAR, Ui::db);
        compressor.addView(standard, Ui.match());

        LinearLayout tube = Ui.column(context);
        compressorPanels[1] = tube;
        slider(context, tube, "compressor_tube_peak", "Peak reduction",
                0, 100, 200, ParameterSlider.Scale.LINEAR, value -> percent(value));
        slider(context, tube, "compressor_tube_gain", "Gain",
                0, 100, 200, ParameterSlider.Scale.LINEAR, value -> percent(value));
        tubeLimitMode = toggle(context, "Limit mode", "compressor_tube_limit_mode");
        tube.addView(tubeLimitMode, Ui.spaced(context));
        tube.addView(Ui.caption(context,
                "Attack follows Peak reduction. Release is fixed by the Tube model."),
                Ui.match());
        tube.setVisibility(View.GONE);
        compressor.addView(tube, Ui.match());

        LinearLayout fet = Ui.column(context);
        compressorPanels[2] = fet;
        fet.addView(Ui.label(context, "Ratio"), Ui.match());
        fetRatio = spinner(context, new String[] {"4:1", "8:1", "12:1", "20:1", "All"});
        fetRatio.setContentDescription("FET ratio");
        fetRatio.setOnItemSelectedListener(parameterSpinner("compressor_fet_ratio_index"));
        fet.addView(fetRatio, Ui.match());
        slider(context, fet, "compressor_fet_input", "Input",
                -56, 0, 224, ParameterSlider.Scale.LINEAR, Ui::db);
        slider(context, fet, "compressor_fet_output", "Output",
                -56, 0, 224, ParameterSlider.Scale.LINEAR, Ui::db);
        slider(context, fet, "compressor_fet_attack", "Attack",
                0.000021f, 0.0008f, 250, ParameterSlider.Scale.LOG,
                value -> String.format(java.util.Locale.US, "%.3f ms", value * 1000));
        slider(context, fet, "compressor_fet_release", "Release",
                0.05f, 1.1f, 250, ParameterSlider.Scale.LOG, value -> millis(value));
        fet.setVisibility(View.GONE);
        compressor.addView(fet, Ui.match());
        Ui.styleRack(compressor);
        content.addView(compressor, Ui.spaced(context));

        LinearLayout limiter = Ui.panel(context);
        modules[3] = limiter;
        limiter.addView(Ui.section(context, "Limiter"), Ui.match());
        limiterOn = toggle(context, "On", "limiter_on");
        limiter.addView(limiterOn, Ui.match());
        limiterThreshold = slider(
                context, limiter, "limiter_threshold", "Ceiling",
                -28, 0, 112, ParameterSlider.Scale.LINEAR, Ui::db);
        limiterRelease = slider(context, limiter, "limiter_release", "Release",
                0.05f, 1.5f, 250, ParameterSlider.Scale.LOG,
                value -> millis(value));
        Ui.styleRack(limiter);
        content.addView(limiter, Ui.spaced(context));

        LinearLayout equalizer = Ui.panel(context);
        modules[4] = equalizer;
        LinearLayout eqHeader = Ui.row(context);
        eqHeader.addView(Ui.section(context, "Equalizer"), Ui.weight(1));
        eqOn = eqToggle(context, "On", "eq_on");
        eqHeader.addView(eqOn);
        equalizer.addView(eqHeader, Ui.match());
        eqModel = spinner(context, new String[] {"Standard", "Passive", "Vintage"});
        eqModel.setContentDescription("EQ model");
        eqModel.setOnItemSelectedListener(parameterSpinner("eq_model"));
        equalizer.addView(eqModel, Ui.match());
        eqGraph = new EqGraphView(context);
        equalizer.addView(eqGraph, Ui.spaced(context));

        LinearLayout standardEq = Ui.column(context);
        eqPanels[0] = standardEq;
        equalizer.addView(standardEq, Ui.match());
        standardEq.addView(Ui.caption(context,
                "Four independent bands. Drag a graph node for frequency and gain."), Ui.match());
        String[] bandNames = {"Low", "Low mid", "High mid", "High"};
        String[] keys = {"low", "lowmid", "himid", "high"};
        eqBandSelector = spinner(context, bandNames);
        eqBandSelector.setContentDescription("Band");
        LinearLayout bandRow = Ui.row(context);
        bandRow.addView(Ui.label(context, "Band"));
        bandRow.addView(eqBandSelector, Ui.weight(1));
        standardEq.addView(bandRow, Ui.spaced(context));

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
            standardEq.addView(band, Ui.match());
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
        LinearLayout passive = Ui.column(context);
        eqPanels[1] = passive;
        passive.addView(Ui.caption(context,
                "Broad, interacting boost and attenuation curves."), Ui.match());
        eqChoice(context, passive, "eq_passive_low_frequency_index", "Low frequency",
                new String[] {"20 Hz", "30 Hz", "60 Hz", "100 Hz"});
        eqAmount(context, passive, "eq_passive_low_boost", "Low boost", 0, 10);
        eqAmount(context, passive, "eq_passive_low_attenuation", "Low attenuation", 0, 10);
        eqChoice(context, passive, "eq_passive_high_frequency_index", "High boost frequency",
                new String[] {"3 kHz", "4 kHz", "5 kHz", "8 kHz", "10 kHz", "12 kHz", "16 kHz"});
        eqAmount(context, passive, "eq_passive_high_boost", "High boost", 0, 10);
        eqAmount(context, passive, "eq_passive_bandwidth", "Bandwidth", 0, 10);
        eqChoice(context, passive, "eq_passive_attenuation_frequency_index", "High attenuation frequency",
                new String[] {"5 kHz", "10 kHz", "20 kHz"});
        eqAmount(context, passive, "eq_passive_high_attenuation", "High attenuation", 0, 10);
        equalizer.addView(passive, Ui.match());

        LinearLayout vintage = Ui.column(context);
        eqPanels[2] = vintage;
        vintage.addView(Ui.caption(context,
                "Four bands with stepped frequencies and a fixed high shelf."), Ui.match());
        eqChoice(context, vintage, "eq_vintage_low_frequency_index", "Low frequency",
                new String[] {"35 Hz", "60 Hz", "110 Hz", "220 Hz"});
        eqAmount(context, vintage, "eq_vintage_low_gain", "Low gain", -16, 16);
        eqChoice(context, vintage, "eq_vintage_lowmid_frequency_index", "Low-mid frequency",
                new String[] {"360 Hz", "700 Hz", "1.6 kHz"});
        eqAmount(context, vintage, "eq_vintage_lowmid_gain", "Low-mid gain", -16, 16);
        eqChoice(context, vintage, "eq_vintage_himid_frequency_index", "High-mid frequency",
                new String[] {"3.2 kHz", "4.8 kHz", "7.2 kHz"});
        eqAmount(context, vintage, "eq_vintage_himid_gain", "High-mid gain", -16, 16);
        eqAmount(context, vintage, "eq_vintage_high_gain", "High shelf gain", -16, 16);
        equalizer.addView(vintage, Ui.match());
        eqFirst = toggle(context, "EQ before compressor", "eq_first");
        equalizer.addView(eqFirst, Ui.spaced(context));
        Ui.styleRack(equalizer);
        content.addView(equalizer, Ui.spaced(context));
        showModule(0);
        root = Ui.scroll(context, content);
    }

    @Override
    public View view() {
        return root;
    }

    @Override
    public void render(Io24State state) {
        lastState = state;
        if (eqGraph.isEditing()) {
            return;
        }
        boolean enabled = state.connectionStatus()
                == Io24State.ConnectionStatus.CONNECTED && !state.busy();
        Map<String, Float> values = state.processing(channel);
        rendering = true;
        highPassOn.setChecked(state.input(channel).highPass());
        highPassFrequency.setValue(value(values, "hpf_hz", 80));
        gateOn.setChecked(on(values, "gate_on"));
        gateExpander.setChecked(onDefault(values, "gate_expander", true));
        gateThreshold.setValue(value(values, "gate_threshold", -48));
        gateRange.setValue(value(values, "gate_range", -48));
        gateAttack.setValue(value(values, "gate_attack", 0.005f));
        gateRelease.setValue(value(values, "gate_release", 0.7f));
        int model = Math.max(0, Math.min(2, Math.round(value(values, "compressor_model", 0))));
        compressorModel.setSelection(model, false);
        for (int index = 0; index < compressorPanels.length; index++) {
            compressorPanels[index].setVisibility(index == model ? View.VISIBLE : View.GONE);
        }
        Map<String, Float> defaults = PROCESSING_DEFAULTS;
        for (Map.Entry<String, ParameterSlider> entry : sliders.entrySet()) {
            if (entry.getKey().startsWith("compressor_tube_")
                    || entry.getKey().startsWith("compressor_fet_")
                    || entry.getKey().startsWith("eq_passive_")
                    || entry.getKey().startsWith("eq_vintage_")) {
                entry.getValue().setValue(value(values, entry.getKey(), defaults.get(entry.getKey())));
            }
        }
        tubeLimitMode.setChecked(on(values, "compressor_tube_limit_mode"));
        fetRatio.setSelection(Math.round(value(values, "compressor_fet_ratio_index", 0)), false);
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
        limiterRelease.setValue(value(values, "limiter_release", 0.4f));
        eqOn.setChecked(on(values, "eq_on"));
        int equalizerModel = Math.max(0, Math.min(2, Math.round(value(values, "eq_model", 0))));
        eqModel.setSelection(equalizerModel, false);
        for (int index = 0; index < eqPanels.length; index++) {
            eqPanels[index].setVisibility(index == equalizerModel ? View.VISIBLE : View.GONE);
        }
        for (Map.Entry<String, Spinner> entry : eqChoices.entrySet()) {
            entry.getValue().setSelection(Math.round(value(values,
                    entry.getKey(), defaults.get(entry.getKey()))), false);
            entry.getValue().setEnabled(enabled);
        }
        eqModel.setEnabled(enabled);
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
        arrangeModules(on(values, "eq_first"));
        for (FatModuleNode node : moduleButtons) {
            node.render(state, channel);
        }
        rendering = false;
        updateGraphs();
        highPassOn.setEnabled(enabled);
        compressorModel.setEnabled(enabled);
        fetRatio.setEnabled(enabled);
        tubeLimitMode.setEnabled(enabled);
        eqBandSelector.setEnabled(enabled);
        eqGraph.setEnabled(enabled);
        for (ParameterSlider slider : sliders.values()) {
            Ui.enabledTree(slider, enabled);
        }
        for (Switch toggle : new Switch[] {
                gateOn, gateExpander, compressorOn, limiterOn, eqOn, eqFirst}) {
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

    private void eqChoice(Context context, LinearLayout parent, String key,
                          String label, String[] options) {
        parent.addView(Ui.label(context, label), Ui.spaced(context));
        Spinner choice = spinner(context, options);
        choice.setContentDescription(label);
        choice.setSelection(Math.round(PROCESSING_DEFAULTS.get(key)), false);
        choice.setOnItemSelectedListener(parameterSpinner(key));
        eqChoices.put(key, choice);
        parent.addView(choice, Ui.match());
    }

    private void eqAmount(Context context, LinearLayout parent, String key,
                          String label, float min, float max) {
        slider(context, parent, key, label, min, max, Math.round((max - min) * 4),
                ParameterSlider.Scale.LINEAR, key.endsWith("_gain") ? Ui::db
                        : value -> String.format(java.util.Locale.US, "%.2f", value));
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
                if (!rendering && position != Math.round(value(
                        lastState.processing(channel), key, 0))) {
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
        Map<String, Float> graphValues = new LinkedHashMap<>(lastState.processing(channel));
        for (Map.Entry<String, ParameterSlider> entry : sliders.entrySet()) {
            if (entry.getKey().startsWith("compressor_")) {
                graphValues.put(entry.getKey(), entry.getValue().value());
            }
        }
        graphValues.put("compressor_model", (float) compressorModel.getSelectedItemPosition());
        graphValues.put("compressor_tube_limit_mode", tubeLimitMode.isChecked() ? 1.0f : 0.0f);
        graphValues.put("compressor_fet_ratio_index", (float) fetRatio.getSelectedItemPosition());
        java.nio.ByteBuffer cpxt = java.nio.ByteBuffer.wrap(
                NativeDsp.compressor(graphValues, lastState.sampleRateHz()).get(0))
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        // Nominal static curve; attack/release and the soft knee are not time-domain metering.
        dynamicsGraph.setDynamics(cpxt.getFloat(48),
                1.0f / Math.max(0.000001f, 1.0f - cpxt.getFloat(40)),
                (float) (20 * Math.log10(cpxt.getFloat(52))));
        int equalizerModel = Math.max(0, eqModel.getSelectedItemPosition());
        if (equalizerModel != 0) {
            for (Map.Entry<String, ParameterSlider> entry : sliders.entrySet()) {
                if (entry.getKey().startsWith("eq_")) {
                    graphValues.put(entry.getKey(), entry.getValue().value());
                }
            }
            for (Map.Entry<String, Spinner> entry : eqChoices.entrySet()) {
                graphValues.put(entry.getKey(), (float) entry.getValue().getSelectedItemPosition());
            }
            graphValues.put("eq_model", (float) equalizerModel);
            graphValues.put("eq_on", eqOn.isChecked() ? 1f : 0f);
            eqGraph.setAlternateEq(NativeDsp.eqSections(graphValues, lastState.sampleRateHz()),
                    lastState.sampleRateHz(), eqOn.isChecked(),
                    equalizerModel == 1 ? "Passive" : "Vintage");
            return;
        }
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

    private void arrangeModules(boolean eqBefore) {
        int[] order = eqBefore ? new int[] {0, 1, 4, 2, 3} : new int[] {0, 1, 2, 4, 3};
        if (moduleChain.getChildCount() == 5 && moduleChain.getChildAt(2) == moduleButtons[order[2]]) {
            return;
        }
        moduleChain.removeAllViews();
        Context context = moduleChain.getContext();
        float scale = Math.max(1f, context.getResources().getConfiguration().fontScale);
        for (int index : order) {
            moduleChain.addView(moduleButtons[index], new LinearLayout.LayoutParams(
                    Ui.dp(context, Math.round(148 * scale)), Ui.dp(context, Math.round(132 * scale))));
        }
    }

    private void showModule(int selected) {
        selectedModule = selected;
        for (int index = 0; index < modules.length; index++) {
            boolean active = index == selectedModule;
            modules[index].setVisibility(active ? View.VISIBLE : View.GONE);
            moduleButtons[index].setSelected(active);
            moduleButtons[index].invalidate();
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                moduleButtons[index].setStateDescription(active ? "Selected" : "");
            }
        }
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

    private static String percent(float value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private static String millis(float seconds) {
        return String.format(java.util.Locale.US, "%.1f ms", seconds * 1000.0f);
    }
}
