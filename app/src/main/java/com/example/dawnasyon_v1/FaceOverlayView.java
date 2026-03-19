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

    private Paint borderPaint, dotPaint, linePaint, backgroundPaint, transparentPaint;
    private RectF ovalRect;
    private Face mFace;
    private int mImgWidth, mImgHeight;
    private final Path mPath = new Path();

    // ⭐ THE FIX: Using a single Scale Factor to prevent "shrinking"
    private float mScale = 1.0f;
    private float mPostScaleWidthOffset = 0f;
    private float mPostScaleHeightOffset = 0f;

    private boolean isRegistrationMode = false;

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
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

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#99000000"));

        transparentPaint = new Paint();
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void setRegistrationMode(boolean enable) {
        this.isRegistrationMode = enable;
        invalidate();
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isRegistrationMode) {
            // Draw registration hole (unchanged)
            float width = getWidth();
            float height = getHeight();
            ovalRect = new RectF(width * 0.125f, height * 0.225f, width * 0.875f, height * 0.775f);
            int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
            canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
            canvas.drawOval(ovalRect, transparentPaint);
            canvas.drawOval(ovalRect, borderPaint);
            canvas.restoreToCount(saveCount);

        } else {
            if (mFace == null || mImgWidth == 0 || mImgHeight == 0) return;

            // ⭐ IMPROVED SCALING MATH
            // We use Math.max to make sure the face fills the screen view
            float viewWidth = (float) getWidth();
            float viewHeight = (float) getHeight();

            float scaleX = viewWidth / (float) mImgWidth;
            float scaleY = viewHeight / (float) mImgHeight;
            mScale = Math.max(scaleX, scaleY);

            mPostScaleWidthOffset = (viewWidth - (float) mImgWidth * mScale) / 2;
            mPostScaleHeightOffset = (viewHeight - (float) mImgHeight * mScale) / 2;

            int[] contours = {
                    FaceContour.FACE, FaceContour.LEFT_EYEBROW_TOP, FaceContour.RIGHT_EYEBROW_TOP,
                    FaceContour.LEFT_EYE, FaceContour.RIGHT_EYE, FaceContour.NOSE_BRIDGE,
                    FaceContour.NOSE_BOTTOM, FaceContour.UPPER_LIP_TOP, FaceContour.LOWER_LIP_BOTTOM
            };

            for (int c : contours) {
                drawContourOptimized(canvas, mFace.getContour(c));
            }

            if (mFace.getBoundingBox() != null) {
                drawTechCorners(canvas, mFace.getBoundingBox());
            }
        }
    }

    private float translateX(float x) {
        // Front camera is mirrored
        float flippedX = mImgWidth - x;
        return (flippedX * mScale) + mPostScaleWidthOffset;
    }

    private float translateY(float y) {
        return (y * mScale) + mPostScaleHeightOffset;
    }

    private void drawContourOptimized(Canvas canvas, FaceContour contour) {
        if (contour == null) return;
        List<PointF> points = contour.getPoints();
        if (points.isEmpty()) return;

        mPath.reset();
        for (int i = 0; i < points.size(); i++) {
            PointF p = points.get(i);
            float sx = translateX(p.x);
            float sy = translateY(p.y);

            if (i == 0) mPath.moveTo(sx, sy);
            else mPath.lineTo(sx, sy);

            canvas.drawCircle(sx, sy, 4f, dotPaint);
        }
        canvas.drawPath(mPath, linePaint);
    }

    private void drawTechCorners(Canvas canvas, android.graphics.Rect boundingBox) {
        float left = translateX(boundingBox.right);
        float right = translateX(boundingBox.left);
        float top = translateY(boundingBox.top);
        float bottom = translateY(boundingBox.bottom);

        if (left > right) { float temp = left; left = right; right = temp; }

        float len = (right - left) * 0.2f;

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