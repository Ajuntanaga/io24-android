package dev.ajuntanaga.io24;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/** Foreground-only recorder using Android's USB audio driver, not the control interface. */
final class TakeRecorder {
    private static final int RATE = 48_000;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean stop;
    private volatile long frames;
    private boolean busy;
    private String status = "Ready · 48 kHz / 16-bit WAV";
    private Runnable listener = () -> {};

    TakeRecorder(Context context) { this.context = context.getApplicationContext(); }
    void setListener(Runnable listener) { this.listener = listener; }
    boolean busy() { return busy; }
    String status() { return status; }
    long seconds() { return frames / RATE; }
    File directory() { return new File(context.getFilesDir(), "takes"); }
    void stop() { stop = true; }

    void start(int channel) {
        if (busy) return;
        busy = true;
        stop = false;
        frames = 0;
        status = "Opening io24 audio…";
        listener.run();
        new Thread(() -> capture(channel), "io24-recorder").start();
    }

    @SuppressLint("MissingPermission") // Activity obtains RECORD_AUDIO before start.
    private void capture(int channel) {
        AudioRecord audio = null;
        File partial = null;
        WavTake take = null;
        String result;
        try {
            AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo input = null;
            for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                String name = device.getProductName().toString().toLowerCase(Locale.ROOT);
                if ((device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE
                        || (android.os.Build.VERSION.SDK_INT >= 26
                            && device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET))
                        && (name.contains("io24") || name.contains("revelator"))) {
                    if (input != null) throw new IOException("More than one io24 audio input; connect only one");
                    input = device;
                }
            }
            if (input == null) throw new IOException("No io24 USB audio input found. Check USB audio routing in Android.");
            int minimum = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (minimum <= 0) throw new IOException("Stereo USB recording at 48 kHz is unavailable");
            audio = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(minimum, 16_384));
            if (audio.getState() != AudioRecord.STATE_INITIALIZED || !audio.setPreferredDevice(input))
                throw new IOException("Could not select io24 audio input");
            if (!directory().isDirectory() && !directory().mkdirs()) throw new IOException("Cannot create takes folder");
            String name = "Take-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                    .format(new java.util.Date()) + "-" + UUID.randomUUID().toString().substring(0, 8);
            partial = new File(directory(), name + ".partial");
            audio.startRecording();
            if (audio.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
                throw new IOException("Android could not start recording");
            final String sourceName = input.getProductName().toString();
            main.post(() -> { status = "Recording · " + sourceName; listener.run(); });
            take = new WavTake(partial, RATE, channel == 2 ? 2 : 1);
            try (WavTake wav = take) {
                short[] samples = new short[4096];
                byte[] pcm = new byte[8192];
                long lastData = android.os.SystemClock.elapsedRealtime();
                while (!stop) {
                    int count = audio.read(samples, 0, samples.length, AudioRecord.READ_NON_BLOCKING);
                    if (count < 0) throw new IOException("Audio capture stopped (" + count + ")");
                    AudioDeviceInfo routed = audio.getRoutedDevice();
                    if (count > 0) {
                        if (routed == null || routed.getId() != input.getId())
                            throw new IOException("io24 audio route lost; phone microphone was not recorded");
                        if (count % 2 != 0) throw new IOException("Incomplete stereo audio frame");
                        int bytes = 0;
                        for (int i = 0; i < count; i++) {
                            if (channel != 2 && i % 2 != channel) continue;
                            pcm[bytes++] = (byte) samples[i];
                            pcm[bytes++] = (byte) (samples[i] >> 8);
                        }
                        wav.write(pcm, bytes);
                        frames = wav.frames();
                        lastData = android.os.SystemClock.elapsedRealtime();
                    } else {
                        if (android.os.SystemClock.elapsedRealtime() - lastData > 3000)
                            throw new IOException("No audio received from io24");
                        Thread.sleep(10);
                    }
                }
            }
            result = frames > 0 ? "Take saved on this phone" : "Stopped before audio arrived; no take saved";
        } catch (Exception error) {
            result = "Stopped: " + error.getMessage();
        } finally {
            if (audio != null) {
                try { audio.stop(); } catch (IllegalStateException ignored) { }
                audio.release();
            }
        }
        if (partial != null && partial.exists()) {
            if (frames > 0 && take != null && take.finalized()) {
                File finished = new File(directory(), partial.getName().replace(".partial", ".wav"));
                if (!partial.renameTo(finished)) result += "; could not finalize take filename";
            } else if (frames > 0) {
                result += "; unfinished take retained for recovery";
            } else if (!partial.delete()) {
                result += "; empty temporary file retained";
            }
        }
        final String message = result;
        main.post(() -> { busy = false; status = message; listener.run(); });
    }
}
