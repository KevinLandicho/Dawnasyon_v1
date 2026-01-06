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

public class FaceOverlayView extends View {

    private Paint scrimPaint;
    private Paint borderPaint;
    private Paint eraserPaint;
    private RectF faceRect;
    private float cornerRadius = 40f;

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 1. Dark Background (Semi-transparent)
        scrimPaint = new Paint();
        scrimPaint.setColor(Color.parseColor("#99000000")); // 60% Black

        // 2. The "Tech" Border (Cyan)
        borderPaint = new Paint();
        borderPaint.setColor(Color.CYAN);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8f);
        borderPaint.setAntiAlias(true);

        // 3. The Eraser (To cut the hole)
        eraserPaint = new Paint();
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraserPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Create a focused rectangle in the center (Head shape)
        float width = w * 0.75f;
        float height = h * 0.55f; // Slightly oval for a face
        float left = (w - width) / 2;
        float top = (h - height) / 3; // Position slightly higher than center
        faceRect = new RectF(left, top, left + width, top + height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw dark background
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);

        // Cut out the hole (The Face Window)
        // We use a separate layer to ensure the clear mode works on the background
        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);
        canvas.drawOval(faceRect, eraserPaint);

        // Draw the "Tech" Brackets/Border
        canvas.drawOval(faceRect, borderPaint);

        canvas.restoreToCount(saveCount);
    }

    public RectF getFaceRect() {
        return faceRect;
    }

    // Change color when aligned (e.g., to Green)
    public void setBorderColor(int color) {
        borderPaint.setColor(color);
        invalidate();
    }
}