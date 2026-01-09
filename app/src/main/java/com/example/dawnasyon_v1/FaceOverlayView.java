package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.List;

public class FaceOverlayView extends View {

    private Paint dotPaint, linePaint, borderPaint;
    private Face mFace;
    private int mImgWidth, mImgHeight;
    private float mScaleFactor = 1.0f;
    private float mOffsetX = 0f, mOffsetY = 0f;

    // Reuse Memory
    private final Path mPath = new Path();

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
    }

    public void updateFace(Face face, int imgWidth, int imgHeight) {
        this.mFace = face;
        this.mImgWidth = imgWidth;
        this.mImgHeight = imgHeight;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFace == null || mImgWidth == 0 || mImgHeight == 0) return;

        // Scale Logic
        float inputWidth = mImgHeight;
        float inputHeight = mImgWidth;
        float scaleX = (float) getWidth() / inputWidth;
        float scaleY = (float) getHeight() / inputHeight;
        mScaleFactor = Math.max(scaleX, scaleY);
        mOffsetX = (getWidth() - (inputWidth * mScaleFactor)) / 2f;
        mOffsetY = (getHeight() - (inputHeight * mScaleFactor)) / 2f;

        // Draw Border
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

    private void drawContourOptimized(Canvas canvas, FaceContour contour) {
        if (contour == null) return;
        List<PointF> points = contour.getPoints();
        if (points.isEmpty()) return;

        mPath.reset();
        for (int i = 0; i < points.size(); i++) {
            PointF p = points.get(i);
            float mirroredX = mImgHeight - p.x;
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

    public void setBorderColor(int color) {
        borderPaint.setColor(color);
        invalidate();
    }
}