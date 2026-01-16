package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.List;

public class FaceOverlayView extends View {

    // Common Paints
    private Paint borderPaint;

    // Verification Paints (Tech Look)
    private Paint dotPaint, linePaint;

    // Registration Paints (Hole Punch Look)
    private Paint backgroundPaint, transparentPaint;
    private RectF ovalRect;

    // Data
    private Face mFace;
    private int mImgWidth, mImgHeight;
    private float mScaleFactor = 1.0f;
    private float mOffsetX = 0f, mOffsetY = 0f;
    private final Path mPath = new Path();

    // ⭐ MODE SWITCH: Default is False (Verification Mode)
    private boolean isRegistrationMode = false;

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // --- 1. SETUP VERIFICATION PAINTS (Your Original Code) ---
        dotPaint = new Paint();
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);
        dotPaint.setShadowLayer(5f, 0, 0, Color.CYAN);

        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#80FFFFFF"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
        linePaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(Color.CYAN);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(10f);
        borderPaint.setStrokeCap(Paint.Cap.SQUARE);
        borderPaint.setAntiAlias(true);

        // --- 2. SETUP REGISTRATION PAINTS (New Code) ---
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#99000000")); // Dark semi-transparent

        transparentPaint = new Paint();
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR)); // Cuts the hole
    }

    // ⭐ Call this in your Activity to switch modes
    public void setRegistrationMode(boolean enable) {
        this.isRegistrationMode = enable;
        invalidate(); // Trigger redraw
    }

    public void updateFace(Face face, int imgWidth, int imgHeight) {
        this.mFace = face;
        this.mImgWidth = imgWidth;
        this.mImgHeight = imgHeight;
        invalidate();
    }

    public void setBorderColor(int color) {
        borderPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Calculate the oval hole for Registration Mode
        float width = getWidth();
        float height = getHeight();
        float holeWidth = width * 0.75f;
        float holeHeight = height * 0.55f;
        float leftPos = (width - holeWidth) / 2;
        float topPos = (height - holeHeight) / 2;
        ovalRect = new RectF(leftPos, topPos, leftPos + holeWidth, topPos + holeHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isRegistrationMode) {
            // ==================================================
            // MODE A: REGISTRATION (Static Guide + Dark Screen)
            // ==================================================
            // 1. Save Layer to allow "Clear" mode to work
            int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

            // 2. Draw Dark Background
            canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

            // 3. Cut the Oval Hole
            canvas.drawOval(ovalRect, transparentPaint);

            // 4. Draw the Colored Ring (Re-using borderPaint)
            canvas.drawOval(ovalRect, borderPaint);

            canvas.restoreToCount(saveCount);

        } else {
            // ==================================================
            // MODE B: VERIFICATION (Your Dynamic Tech Look)
            // ==================================================
            if (mFace == null || mImgWidth == 0 || mImgHeight == 0) return;

            // Scale Logic (Your original logic)
            float inputWidth = mImgHeight;
            float inputHeight = mImgWidth;
            float scaleX = (float) getWidth() / inputWidth;
            float scaleY = (float) getHeight() / inputHeight;
            mScaleFactor = Math.max(scaleX, scaleY);
            mOffsetX = (getWidth() - (inputWidth * mScaleFactor)) / 2f;
            mOffsetY = (getHeight() - (inputHeight * mScaleFactor)) / 2f;

            // Draw Border Corners
            if (mFace.getBoundingBox() != null) {
                drawTechCorners(canvas, mFace.getBoundingBox().left, mFace.getBoundingBox().top,
                        mFace.getBoundingBox().right, mFace.getBoundingBox().bottom);
            }

            // Draw Contours
            int[] contours = {
                    FaceContour.FACE, FaceContour.LEFT_EYEBROW_TOP, FaceContour.RIGHT_EYEBROW_TOP,
                    FaceContour.LEFT_EYE, FaceContour.RIGHT_EYE, FaceContour.NOSE_BRIDGE,
                    FaceContour.NOSE_BOTTOM, FaceContour.UPPER_LIP_TOP, FaceContour.LOWER_LIP_BOTTOM
            };

            for (int c : contours) {
                drawContourOptimized(canvas, mFace.getContour(c));
            }
        }
    }

    // --- Helper Methods for Verification Mode (Untouched) ---

    private void drawContourOptimized(Canvas canvas, FaceContour contour) {
        if (contour == null) return;
        List<PointF> points = contour.getPoints();
        if (points.isEmpty()) return;

        mPath.reset();
        for (int i = 0; i < points.size(); i++) {
            PointF p = points.get(i);
            float mirroredX = mImgHeight - p.x; // Mirror logic for front camera
            float sx = (mirroredX * mScaleFactor) + mOffsetX;
            float sy = (p.y * mScaleFactor) + mOffsetY;

            if (i == 0) mPath.moveTo(sx, sy);
            else mPath.lineTo(sx, sy);
            canvas.drawCircle(sx, sy, 4f, dotPaint);
        }
        canvas.drawPath(mPath, linePaint);
    }

    private void drawTechCorners(Canvas canvas, float l, float t, float r, float b) {
        float inputWidth = mImgHeight;

        float left = ((inputWidth - l) * mScaleFactor) + mOffsetX;
        float right = ((inputWidth - r) * mScaleFactor) + mOffsetX;
        float top = (t * mScaleFactor) + mOffsetY;
        float bottom = (b * mScaleFactor) + mOffsetY;

        if (left > right) { float temp = left; left = right; right = temp; }
        float len = (right - left) * 0.2f;

        // Draw corners
        canvas.drawLine(left, top, left + len, top, borderPaint);
        canvas.drawLine(left, top, left, top + len, borderPaint);
        canvas.drawLine(right, top, right - len, top, borderPaint);
        canvas.drawLine(right, top, right, top + len, borderPaint);
        canvas.drawLine(left, bottom, left + len, bottom, borderPaint);
        canvas.drawLine(left, bottom, left, bottom - len, borderPaint);
        canvas.drawLine(right, bottom, right - len, bottom, borderPaint);
        canvas.drawLine(right, bottom, right, bottom - len, borderPaint);
    }
}