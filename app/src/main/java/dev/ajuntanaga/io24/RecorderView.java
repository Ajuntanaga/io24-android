package dev.ajuntanaga.io24;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;

@SuppressLint("SetTextI18n")
final class RecorderView implements ControllerView {
    interface StartTake { void accept(int channel); }
    interface SaveTake { void accept(File file); }
    private final Context context;
    private final TakeRecorder recorder;
    private final View root;
    private final TextView status, clock;
    private final Spinner source;
    private final Button record;
    private final LinearLayout takes;
    private final SaveTake save;
    private boolean wasBusy;

    RecorderView(Context context, TakeRecorder recorder, StartTake start, SaveTake save) {
        this.context = context;
        this.recorder = recorder;
        this.save = save;
        LinearLayout content = Ui.column(context);
        content.setPadding(Ui.dp(context, 16), Ui.dp(context, 12), Ui.dp(context, 16), Ui.dp(context, 24));
        content.addView(Ui.title(context, "Recorder"), Ui.match());
        content.addView(Ui.caption(context, "Record a take from your io24. Monitor through the interface."), Ui.spaced(context));
        LinearLayout panel = Ui.panel(context);
        panel.addView(Ui.section(context, "Recording input"), Ui.match());
        source = new Spinner(context);
        source.setContentDescription("Recording input channels");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Input 1 · mono", "Input 2 · mono", "Inputs 1 + 2 · stereo"});
        source.setAdapter(adapter);
        panel.addView(source, Ui.match());
        clock = Ui.text(context, "00:00", 26, true);
        panel.addView(clock, Ui.spaced(context));
        status = Ui.label(context, recorder.status());
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        panel.addView(status, Ui.spaced(context));
        record = Ui.button(context, "Record");
        record.setOnClickListener(v -> {
            if (recorder.busy()) recorder.stop(); else start.accept(source.getSelectedItemPosition());
        });
        panel.addView(record, Ui.spaced(context));
        panel.addView(Ui.caption(context,
                "48 kHz · 16-bit WAV. Keep the app open; leaving it stops and saves the take. "
                + "Records the USB input signal, which may differ from your headphone mix."), Ui.spaced(context));
        content.addView(panel, Ui.spaced(context));
        content.addView(Ui.section(context, "Saved takes"), Ui.spaced(context));
        content.addView(Ui.caption(context,
                "Saved automatically inside the app on this phone. Use Save to Files or Share to keep a copy outside the app before uninstalling."), Ui.match());
        takes = Ui.column(context);
        content.addView(takes, Ui.spaced(context));
        root = Ui.scroll(context, content);
        refreshTakes();
        refresh();
    }

    void refresh() {
        status.setText(recorder.status());
        long seconds = recorder.seconds();
        clock.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
        record.setText(recorder.busy() ? "Stop & save" : "Record");
        source.setEnabled(!recorder.busy());
        if (wasBusy && !recorder.busy()) refreshTakes();
        wasBusy = recorder.busy();
    }

    private void refreshTakes() {
        takes.removeAllViews();
        File[] files = recorder.directory().listFiles((dir, name) -> name.endsWith(".wav"));
        if (files == null || files.length == 0) {
            takes.addView(Ui.caption(context, "Your finished takes will appear here."), Ui.match());
            return;
        }
        Arrays.sort(files, (a, b) -> b.getName().compareTo(a.getName()));
        for (File file : files) {
            LinearLayout item = Ui.column(context);
            item.addView(Ui.label(context, file.getName()), Ui.match());
            LinearLayout actions = Ui.row(context);
            Button export = Ui.button(context, "Save to Files");
            export.setOnClickListener(v -> save.accept(file));
            actions.addView(export, Ui.weight(1));
            Button share = Ui.button(context, "Share");
            share.setOnClickListener(v -> {
                Uri uri = Uri.parse("content://" + context.getPackageName() + ".takes/" + file.getName());
                Intent intent = new Intent(Intent.ACTION_SEND).setType("audio/wav")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setClipData(ClipData.newRawUri("Recorded take", uri));
                context.startActivity(Intent.createChooser(intent, "Share take"));
            });
            actions.addView(share, Ui.weight(1));
            item.addView(actions, Ui.spaced(context));
            takes.addView(item, Ui.spaced(context));
        }
    }

    @Override public View view() { return root; }
    @Override public void render(Io24State state) { refresh(); }
}
