package dev.ajuntanaga.io24;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ViewParent;

/** Exact Standard-EQ response with touch nodes for frequency and gain. */
final class EqGraphView extends TraceView {
    interface Listener {
        void selected(int band);

        void changed(int band, float frequency, float gainDb, boolean committed);
    }

    private static final float MIN_FREQUENCY = 36.0f;
    private static final float MAX_FREQUENCY = 18_000.0f;
    private final Paint node = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selected = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] frequencies = {130, 320, 1400, 5000};
    private float[] gainsDb = {0, 0, 0, 0};
    private float[] q = {0.6f, 0.6f, 0.6f, 0.6f};
    private boolean[] on = {true, true, true, true};
    private boolean[] shelf = {true, false, false, true};
    private int sampleRateHz = 48_000;
    private int selectedBand;
    private int activeBand = -1;
    private float downX;
    private float downY;
    private boolean dragged;
    private Listener listener;

    EqGraphView(Context context) {
        super(context);
        node.setColor(context.getColor(R.color.accent));
        node.setStyle(Paint.Style.FILL);
        selected.setColor(context.getColor(R.color.text_primary));
        selected.setStyle(Paint.Style.STROKE);
        selected.setStrokeWidth(Ui.dp(context, 2));
        selected.setShadowLayer(
                Ui.dp(context, 5), 0, 0,
                context.getColor(R.color.accent_glow));
        text.setColor(context.getColor(R.color.surface_0));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(Ui.dp(context, 10));
        text.setFakeBoldText(true);
        setFocusable(true);
        setClickable(true);
    }

    void setListener(Listener value) {
        listener = value;
    }

    void setSelectedBand(int value) {
        selectedBand = Math.max(0, Math.min(3, value));
        invalidate();
    }

    void setEq(
            float[] frequencyValues,
            float[] gainValues,
            float[] qValues,
            boolean[] bandOn,
            boolean[] shelfValues,
            int rate,
            boolean allOn) {
        frequencies = frequencyValues.clone();
        gainsDb = gainValues.clone();
        q = qValues.clone();
        on = bandOn.clone();
        shelf = shelfValues.clone();
        sampleRateHz = rate > 0 ? rate : 48_000;
        setAlpha(allOn ? 1.0f : 0.55f);
        refreshTrace();
    }

    private void refreshTrace() {
        setTrace(
                GraphMath.eqCurve(
                        frequencies, gainsDb, q, on, shelf, sampleRateHz,
                        Math.max(1, getWidth()), Ui.dp(getContext(), 120)),
                "Standard four band equalizer response. Drag a numbered node "
                        + "to change its frequency and gain.");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int band = 0; band < 4; band++) {
            float x = xForFrequency(frequencies[band]);
            float y = yForGain(gainsDb[band]);
            node.setColor(getContext().getColor(
                    on[band] ? R.color.accent : R.color.line));
            canvas.drawCircle(x, y, Ui.dp(getContext(), 11), node);
            canvas.drawText(Integer.toString(band + 1), x,
                    y + Ui.dp(getContext(), 4), text);
            if (band == selectedBand) {
                canvas.drawCircle(x, y, Ui.dp(getContext(), 15), selected);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeBand = nearestBand(event.getX(), event.getY());
                selectedBand = activeBand;
                downX = event.getX();
                downY = event.getY();
                dragged = false;
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                if (listener != null) {
                    listener.selected(activeBand);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activeBand < 0) {
                    return false;
                }
                if (!dragged && Math.hypot(
                        event.getX() - downX, event.getY() - downY)
                        >= Ui.dp(getContext(), 3)) {
                    dragged = true;
                }
                if (dragged) {
                    updateFromTouch(event.getX(), event.getY(), false);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (activeBand >= 0 && dragged) {
                    updateFromTouch(event.getX(), event.getY(), true);
                }
                releaseParent();
                activeBand = -1;
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                releaseParent();
                activeBand = -1;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateFromTouch(float x, float y, boolean committed) {
        frequencies[activeBand] = frequencyForX(x);
        gainsDb[activeBand] = gainForY(y);
        refreshTrace();
        if (listener != null) {
            listener.changed(
                    activeBand,
                    frequencies[activeBand],
                    gainsDb[activeBand],
                    committed);
        }
    }

    private int nearestBand(float x, float y) {
        int nearest = 0;
        double distance = Double.MAX_VALUE;
        for (int band = 0; band < 4; band++) {
            double current = Math.hypot(
                    x - xForFrequency(frequencies[band]),
                    y - yForGain(gainsDb[band]));
            if (current < distance) {
                distance = current;
                nearest = band;
            }
        }
        return nearest;
    }

    private float xForFrequency(float frequency) {
        double normalized = Math.log(Math.max(20.0, frequency) / 20.0)
                / Math.log(1000.0);
        return (float) Math.max(0.0, Math.min(getWidth(),
                normalized * getWidth()));
    }

    private float frequencyForX(float x) {
        double normalized = Math.max(0.0, Math.min(1.0,
                x / Math.max(1.0, getWidth())));
        return (float) Math.max(MIN_FREQUENCY, Math.min(MAX_FREQUENCY,
                20.0 * Math.pow(1000.0, normalized)));
    }

    private float yForGain(float gain) {
        float padding = Ui.dp(getContext(), 12);
        float available = Math.max(1.0f, getHeight() - 2.0f * padding);
        return padding + (15.0f - Math.max(-15.0f, Math.min(15.0f, gain)))
                / 30.0f * available;
    }

    private float gainForY(float y) {
        float padding = Ui.dp(getContext(), 12);
        float available = Math.max(1.0f, getHeight() - 2.0f * padding);
        float normalized = Math.max(0.0f, Math.min(1.0f,
                (y - padding) / available));
        return 15.0f - normalized * 30.0f;
    }

    private void releaseParent() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
        }
    }
}
