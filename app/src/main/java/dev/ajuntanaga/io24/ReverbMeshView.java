package dev.ajuntanaga.io24;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Bounded, parameter-driven acoustic illustration; not an audio analyzer. */
final class ReverbMeshView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path facet = new Path();
    private final float[][] x = new float[13][25];
    private final float[][] y = new float[13][25];
    private float size, mix, highPass, preDelay;
    private boolean on;
    ReverbMeshView(Context context) { super(context); }
    void setParameters(boolean enabled, float room, float wet, float hp, float delay) {
        on = enabled; size = room; mix = wet; highPass = hp; preDelay = delay;
        setContentDescription("Reverb room mesh, " + (on ? "enabled" : "bypassed")
                + ", room " + Ui.percent(size) + ", return " + Ui.percent(mix)
                + ". Parameter illustration.");
        invalidate();
    }
    @Override protected void onMeasure(int width, int height) {
        setMeasuredDimension(MeasureSpec.getSize(width), resolveSize(Ui.dp(getContext(), 170), height));
    }
    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xff0b100f);
        canvas.drawRoundRect(0, 0, w, h, 6, 6, paint);
        for (int row = 0; row <= 12; row++) {
            float depth = row / 12f;
            for (int col = 0; col <= 24; col++) {
                float time = col / 24f;
                float start = preDelay * 1.3f;
                double envelope = time < start ? 0 : Math.exp(-(time-start)*(5-3.5*size));
                float wave = (float)(Math.sin(time*17 + depth*8)*Math.sin(depth*Math.PI));
                float relief = (float)envelope * (0.25f+mix*.75f) * (1-highPass/1000f)
                        * (0.45f + wave*.28f);
                x[row][col] = w*(.08f + time*.74f + depth*.12f);
                y[row][col] = h*(.3f + depth*.54f - relief*.42f);
            }
        }
        for (int row = 0; row < 12; row++) {
            for (int col = 0; col < 24; col++) {
                facet.reset(); facet.moveTo(x[row][col], y[row][col]);
                facet.lineTo(x[row][col+1], y[row][col+1]);
                facet.lineTo(x[row+1][col+1], y[row+1][col+1]);
                facet.lineTo(x[row+1][col], y[row+1][col]); facet.close();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(on ? (row % 2 == 0 ? 0xff70351b : 0xff4c2a1c) : 0xff28302b);
                canvas.drawPath(facet, paint);
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(.65f*getResources().getDisplayMetrics().density);
                paint.setColor(on ? 0xfff58a40 : 0xff798778);
                canvas.drawPath(facet, paint);
                canvas.drawLine(x[row][col], y[row][col], x[row+1][col+1], y[row+1][col+1], paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
    }
}
