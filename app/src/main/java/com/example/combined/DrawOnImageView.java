package com.example.combined;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DrawOnImageView extends AppCompatImageView {
    private Bitmap imageBitmap;
    private Bitmap drawingBitmap;
    private Canvas drawingCanvas;

    private Paint imagePaint;
    private Paint drawPaint;
    private boolean isDrawingEnabled = false;
    private int drawingColor = Color.BLACK; // Default color is black

    private Path drawPath;
    private List<DrawPath> drawPathList;
    private Stack<DrawPath> undoneDrawPathStack; // Use a stack to store undone paths

    private static class DrawPath {
        Path path;
        int color;

        DrawPath(Path path, int color) {
            this.path = path;
            this.color = color;
        }
    }

    public DrawOnImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        imagePaint = new Paint();
        drawPaint = new Paint();
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(5);
        drawPathList = new ArrayList<>(); // Initialize the drawPathList
        undoneDrawPathStack = new Stack<>(); // Initialize the undoneDrawPathStack
    }

    public void setImageBitmap(Bitmap bitmap) {
        imageBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        drawingBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        drawingCanvas = new Canvas(drawingBitmap);
        invalidate();
    }

    public Bitmap getBitmap() {
        return imageBitmap;
    }

    public void enableDrawing() {
        isDrawingEnabled = true;
    }

    public void disableDrawing() {
        isDrawingEnabled = false;
    }

    public void setDrawingColor(@ColorInt int color) {
        drawingColor = color;
    }

    public void undo() {
        if (!drawPathList.isEmpty()) {
            int lastIndex = drawPathList.size() - 1;
            DrawPath lastDrawnPath = drawPathList.remove(lastIndex); // Remove the last drawn path from the list
            undoneDrawPathStack.push(lastDrawnPath); // Push the path to the undoneDrawPathStack
            redrawCanvas(); // Redraw the drawing canvas
        }
    }

    public void redo() {
        if (!undoneDrawPathStack.isEmpty()) {
            DrawPath undonePath = undoneDrawPathStack.pop(); // Pop the last undone path from the stack
            drawPathList.add(undonePath); // Add the path to the drawPathList
            redrawCanvas(); // Redraw the drawing canvas
        }
    }

    private void redrawCanvas() {
        // Clear the drawing canvas
        drawingCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        // Draw all the paths stored in the drawPathList on the drawing canvas
        for (DrawPath drawPath : drawPathList) {
            drawPaint.setColor(drawPath.color); // Set the color for each path
            drawingCanvas.drawPath(drawPath.path, drawPaint);
        }

        invalidate();
    }

    public Bitmap getMergedBitmap() {
        Bitmap mergedBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mergedCanvas = new Canvas(mergedBitmap);

        // Draw the original image
        if (imageBitmap != null) {
            mergedCanvas.drawBitmap(imageBitmap, 0, 0, imagePaint);
        }

        // Draw the drawing canvas on top of the original image
        if (drawingBitmap != null) {
            mergedCanvas.drawBitmap(drawingBitmap, 0, 0, null);
        }

        return mergedBitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the original image
        if (imageBitmap != null) {
            canvas.drawBitmap(imageBitmap, 0, 0, imagePaint);
        }

        // Draw the drawing canvas on top of the original image
        if (drawingBitmap != null) {
            canvas.drawBitmap(drawingBitmap, 0, 0, null);
        }

        // Draw the current path if drawing is enabled
        if (isDrawingEnabled && drawPath != null) {
            drawPaint.setColor(drawingColor);
            canvas.drawPath(drawPath, drawPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isDrawingEnabled) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    drawPath = new Path();
                    drawPath.moveTo(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    drawPath.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    drawPath.lineTo(x, y);
                    drawPaint.setColor(drawingColor);
                    drawPathList.add(new DrawPath(drawPath, drawingColor));
                    drawingCanvas.drawPath(drawPath, drawPaint); // Draw the current path on the drawing canvas
                    drawPath = null; // Set drawPath to null after drawing
                    undoneDrawPathStack.clear(); // Clear the undoneDrawPathStack when a new path is drawn
                    break;
            }

            invalidate();
            return true;
        }
        return false;
    }
}
