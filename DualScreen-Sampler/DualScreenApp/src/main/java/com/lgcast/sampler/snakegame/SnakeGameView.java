package com.lgcast.sampler.snakegame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import java.util.List;

public class SnakeGameView extends View {
    private final int SNAKE_COLOR = Color.parseColor("#4CAF50");
    private final int FOOD_COLOR = Color.parseColor("#C62828");
    private final int BOARD_COLOR = Color.parseColor("#ff000000");

    private List<Rect> mSnake;
    private Rect mFood;
    private int mScore;
    private boolean mRefreshFrame;

    private int mViewWidth;
    private int mViewHeight;

    public SnakeGameView(Context context, List<Rect> snake, Rect food, int score, int viewWidth, int viewHeight) {
        super(context);
        mSnake = snake;
        mFood = food;
        mScore = score;
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
        mRefreshFrame = true;
    }

    //public void setSnake(List<Rect> snake) {
    //    mSnake = snake;
    //}
    //
    //public void setFood(Rect food) {
    //    mFood = food;
    //}
    //
    //public void setScore(int score) {
    //    mScore = score;
    //}

    //public void updateView() {
    //    invalidate();
    //}

    public void updateView(List<Rect> snake, Rect food, int score) {
        mSnake = snake;
        mFood = food;
        mScore = score;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawFrame(canvas);
        drawSnake(canvas);
        drawFood(canvas);
        drawScore(canvas);
    }

    private void drawFrame(Canvas canvas) {
        if (mRefreshFrame == true) {
            Paint paint = new Paint();
            paint.setColor(BOARD_COLOR);
            Rect rect = new Rect(0, 0, mViewWidth, mViewHeight);
            canvas.drawRect(rect, paint);
            mRefreshFrame = false;
        }
    }

    private void drawSnake(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(SNAKE_COLOR);
        paint.setStrokeWidth(1);

        for (Rect snake : mSnake)
            canvas.drawRect(snake, paint);
    }

    private void drawFood(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(FOOD_COLOR);
        canvas.drawRect(mFood, paint);
    }

    private void drawScore(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.LTGRAY);
        paint.setTextSize(50);
        canvas.drawText("SCORE: " + mScore, 20, 70, paint);
    }
}
