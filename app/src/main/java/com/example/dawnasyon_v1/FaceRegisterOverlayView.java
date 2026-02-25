package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FaceRegisterOverlayView extends View {
    private Paint backgroundPaint, borderPaint, transparentPaint;
    private RectF ovalRect = new RectF();
    private int borderColor = Color.CYAN;

    public FaceRegisterOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#99000000")); // Darken background

        borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(10f);
        borderPaint.setAntiAlias(true);

        transparentPaint = new Paint();
        transparentPaint.setAntiAlias(true);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        borderPaint.setColor(color);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();

        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Define a nice oval guide
        float ovalWidth = width * 0.7f;
        float ovalHeight = ovalWidth * 1.3f;
        float left = (width - ovalWidth) / 2;
        float top = (height - ovalHeight) / 2;
        ovalRect.set(left, top, left + ovalWidth, top + ovalHeight);

        canvas.drawOval(ovalRect, transparentPaint);
        canvas.drawOval(ovalRect, borderPaint);
    }
}