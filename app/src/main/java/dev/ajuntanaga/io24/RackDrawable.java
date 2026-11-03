package dev.ajuntanaga.io24;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Powder-coated faceplate with hardware confined to the rack ears. */
final class RackDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    RackDrawable(float density) { this.density = density; }
    @Override public void draw(Canvas canvas) {
        float w = getBounds().width(), h = getBounds().height();
        canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        paint.setColor(0xffe8e7dc);
        canvas.drawRoundRect(0, 0, w, h, 5*density, 5*density, paint);
        paint.setColor(0x16727565);
        // Fixed stipple: no bitmap allocations or animated noise.
        for (int y = 3; y < h; y += 7*density) {
            for (int x = 3; x < w; x += 9*density) {
                canvas.drawPoint(x + (y % 5), y, paint);
            }
        }
        paint.setColor(0xffb4b7ab);
        canvas.drawLine(14*density, 0, 14*density, h, paint);
        canvas.drawLine(w-14*density, 0, w-14*density, h, paint);
        for (float x : new float[] {7*density, w-7*density}) {
            for (float y : new float[] {12*density, h-12*density}) {
                paint.setColor(0xff555c53);
                canvas.drawCircle(x, y, 3*density, paint);
                paint.setColor(0xfff7f5e9);
                canvas.drawLine(x-2*density, y-density, x+2*density, y-density, paint);
            }
        }
        paint.setColor(0xffef661f);
        canvas.drawRect(15*density, h-3*density, w-15*density, h-density, paint);
        canvas.restore();
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
