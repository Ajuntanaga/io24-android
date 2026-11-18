package dev.ajuntanaga.io24;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.Map;

/** Compact host-style signal-chain tile. Graphs use confirmed device parameters. */
final class FatModuleNode extends View {
    private final int module;
    private final String title;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int accent = Color.rgb(61, 214, 222);
    private boolean powered;
    private String caption = "Bypassed";
    private float[] curve;
    private float threshold;

    FatModuleNode(Context context, int module, String title) {
        super(context);
        this.module = module;
        this.title = title;
        setClickable(true);
        setFocusable(true);
        setContentDescription("Show " + title + " module");
    }

    void render(Io24State state, int channel) {
        Map<String, Float> values = state.processing(channel);
        accent = channel == 1 ? Color.rgb(61, 214, 222) : Color.rgb(168, 140, 250);
        String[] keys = {"hpf_on", "gate_on", "compressor_on", "limiter_on", "eq_on"};
        powered = module == 0 ? state.input(channel).highPass() : value(values, keys[module], 0) > .5f;
        curve = null;
        if (module == 0) {
            float hz = value(values, "hpf_hz", 80);
            caption = String.format(Locale.US, "%.0f Hz", hz);
            curve = new float[72];
            for (int i = 0; i < curve.length; i++) {
                double f = 20 * Math.pow(1200, i / 71.0);
                double r = Math.pow(f / Math.max(1, hz), 2);
                double db = 20 * Math.log10(Math.max(.001, r / Math.sqrt(1 + r * r)));
                curve[i] = (float) (100 - Math.max(0, Math.min(1, (db + 24) / 24)) * 100);
            }
        } else if (module == 2) {
            ByteBuffer data = ByteBuffer.wrap(NativeDsp.compressor(values, state.sampleRateHz()).get(0))
                    .order(ByteOrder.LITTLE_ENDIAN);
            curve = GraphMath.dynamicsCurve(data.getFloat(48),
                    1 / Math.max(.000001f, 1 - data.getFloat(40)), 0, 100, 100);
            caption = new String[] {"Standard", "Tube", "FET"}[model(values, "compressor_model")];
        } else if (module == 4) {
            curve = GraphMath.eqCoefficientCurve(NativeDsp.eqSections(values, state.sampleRateHz()),
                    state.sampleRateHz(), 100, 100);
            caption = new String[] {"Standard", "Passive", "Vintage"}[model(values, "eq_model")];
        } else {
            threshold = value(values, module == 1 ? "gate_threshold" : "limiter_threshold", -48);
            caption = String.format(Locale.US, "%s · %.0f dB",
                    module == 1 ? (value(values, "gate_expander", 1) > .5f ? "Exp" : "Gate") : "Peak", threshold);
        }
        setContentDescription("Show " + title + ", " + (powered ? "on, " : "bypassed, ") + caption);
        invalidate();
    }

    private static float value(Map<String, Float> values, String key, float fallback) {
        Float result = values.get(key);
        return result == null ? fallback : result;
    }

    private static int model(Map<String, Float> values, String key) {
        return Math.max(0, Math.min(2, Math.round(value(values, key, 0))));
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Button");
        info.setSelected(isSelected());
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Logical coordinates keep the host proportions at every density/font scale.
        canvas.save();
        canvas.scale(getWidth() / 148f, getHeight() / 132f);
        int ink = Color.rgb(201, 217, 237);
        int grid = Color.rgb(89, 158, 199);
        line(canvas, 0, 66, 148, 66, alpha(ink, 72), 1.5f);
        fill(canvas, 5, 4, 143, 128, Color.rgb(17, 21, 25));
        fill(canvas, 5, 4, 143, 128, alpha(powered ? accent : grid, powered ? 61 : 33));
        fill(canvas, 5, 4, 143, 29, alpha(powered ? accent : grid, powered ? 87 : 51));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(alpha(powered ? accent : grid, 100));
        canvas.drawRect(5, 4, 143, 128, paint);
        if (isSelected() || isFocused() || isPressed()) {
            paint.setColor(accent);
            paint.setStrokeWidth(isFocused() ? 2 : 1);
            canvas.drawRect(3, 2, 145, 130, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(alpha(accent, powered ? 64 : 0));
        canvas.drawCircle(16, 16, 7, paint);
        paint.setColor(powered ? accent : alpha(ink, 100));
        canvas.drawCircle(16, 16, 3.5f, paint);
        text(canvas, title, 27, 21, 12, ink);
        float left = 14, top = 40, width = 120, height = 59;
        line(canvas, left, top + height / 2, left + width, top + height / 2, alpha(grid, 45), 1);
        for (int i = 1; i < 4; i++) {
            line(canvas, left + width * i / 4, top, left + width * i / 4, top + height, alpha(grid, 35), 1);
        }
        if (curve != null) {
            if (module == 2) line(canvas, left, top + height, left + width, top, alpha(ink, 50), 1);
            path.reset();
            for (int i = 0; i < curve.length; i++) {
                float x = left + width * i / (curve.length - 1);
                float y = top + height * curve[i] / 100;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            paint.setColor(powered ? accent : alpha(ink, 115));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.7f);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
        } else {
            // A threshold scale, not a simulated signal meter.
            fill(canvas, left, 87, left + width, 93, alpha(grid, 65));
            float floor = module == 1 ? -84 : -40;
            float x = left + width * Math.max(0, Math.min(1, (threshold - floor) / -floor));
            line(canvas, x, 82, x, 97, Color.rgb(241, 188, 97), 2);
            text(canvas, "THRESHOLD", left, 60, 10, ink);
        }
        text(canvas, powered ? caption : "Bypassed", 14, 117, 11, ink);
        canvas.restore();
    }

    private static int alpha(int color, int alpha) { return (color & 0xFFFFFF) | (alpha << 24); }
    private void fill(Canvas c, float l, float t, float r, float b, int color) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); c.drawRect(l, t, r, b, paint);
    }
    private void line(Canvas c, float x, float y, float x2, float y2, int color, float width) {
        paint.setColor(color); paint.setStrokeWidth(width); c.drawLine(x, y, x2, y2, paint);
    }
    private void text(Canvas c, String text, float x, float y, float size, int color) {
        paint.setStyle(Paint.Style.FILL); paint.setTextSize(size); paint.setColor(color); c.drawText(text, x, y, paint);
    }
}
