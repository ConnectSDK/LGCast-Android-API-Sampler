package com.lgcast.sampler.snakegame;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.widget.LinearLayout;
import com.connectsdk.service.webos.lgcast.common.utils.TimerUtil;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

public class SnakeGameControl {
    private static final String TAG = "LGCAST Snake Game";

    public enum Direction {LEFT, RIGHT, UP, DOWN}

    private static final int WIDTH = 1800;
    private static final int HEIGHT = 900;
    private static final int RECT_SIZE = 90;

    private static final int INITIAL_SNAKE_SIZE = 3;
    private static final int SNAKE_MOVE_SPEED = 500;

    private Context mContext;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mBlockSize;
    private SnakeGameListener mSnakeGameListener;

    private List<Rect> mSnake;
    private Rect mFood;
    private int mScore;
    private SnakeGameView mGameView;

    private Direction mDirection;
    private AtomicBoolean mPauseGame;
    private Timer mMoveTimer;

    public SnakeGameControl(Context context, LinearLayout gameLayout) {
        mContext = context;
        mScreenWidth = WIDTH;
        mScreenHeight = HEIGHT;
        mBlockSize = RECT_SIZE;

        mSnake = createSnake();
        mFood = generateNewFood();
        mScore = 0;

        mGameView = new SnakeGameView(mContext, mSnake, mFood, mScore, mScreenWidth, mScreenHeight);
        mGameView.setLayoutParams(new LinearLayout.LayoutParams(mScreenWidth, mScreenHeight));
        gameLayout.addView(mGameView, 0);
    }

    public void setListener(SnakeGameListener listener) {
        mSnakeGameListener = listener;
    }

    public void startGame() {
        Log.v(TAG, "startGame");
        mDirection = Direction.RIGHT;
        mPauseGame = new AtomicBoolean(false);
        mMoveTimer = TimerUtil.schedule(() -> moveSnake(), 0, SNAKE_MOVE_SPEED);
    }

    public void stopGame() {
        Log.v(TAG, "stopGame");
        if (mMoveTimer != null) mMoveTimer.cancel();
        mMoveTimer = null;
    }

    public void restartGame() {
        Log.v(TAG, "restartGame");
        mSnake = createSnake();
        mFood = generateNewFood();

        stopGame();
        startGame();
    }

    public void pauseGame() {
        Log.v(TAG, "pauseGame");
        mPauseGame.set(true);
    }

    public void resumeGame() {
        Log.v(TAG, "resumeGame");
        mPauseGame.set(false);
    }

    public void changeDirection(Direction direction) {
        Log.v(TAG, "changeDirection: " + direction);

        switch (direction) {
            case RIGHT:
                if (mDirection != Direction.LEFT) mDirection = Direction.RIGHT;
                break;
            case LEFT:
                if (mDirection != Direction.RIGHT) mDirection = Direction.LEFT;
                break;
            case UP:
                if (mDirection != Direction.DOWN) mDirection = Direction.UP;
                break;
            case DOWN:
                if (mDirection != Direction.UP) mDirection = Direction.DOWN;
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private List<Rect> createSnake() {
        List<Rect> snake = new LinkedList<>();
        int startX = 0;
        int startY = mBlockSize * 5;

        for (int i = 0; i < INITIAL_SNAKE_SIZE; i++) {
            int left = startX - (mBlockSize * i);
            int top = startY;
            int right = (startX + mBlockSize) - (mBlockSize * i);
            int bottom = startY + mBlockSize;
            snake.add(new Rect(left, top, right, bottom));
        }

        return snake;
    }

    private Rect generateNewFood() {
        while (true) {
            Rect food = generateRandomFood();
            boolean duplicated = false;

            for (Rect snakeBody : mSnake) {
                if (food.left == snakeBody.left && food.top == snakeBody.top) {
                    duplicated = true;
                    break;
                }
            }

            if (duplicated == false) return food;
        }
    }

    private Rect generateRandomFood() {
        while (true) {
            int randomX = new Random().nextInt(mScreenWidth / mBlockSize) * mBlockSize;
            int randomY = new Random().nextInt(mScreenHeight / mBlockSize) * mBlockSize;

            if (randomX == 0 || randomY == 0) continue;
            if (randomX == mScreenWidth - mBlockSize || randomY == mScreenHeight - mBlockSize) continue;

            return new Rect(randomX, randomY, randomX + mBlockSize, randomY + mBlockSize);
        }
    }

    private void moveSnake() {
        if (mPauseGame.get() == true) return;
        Rect head = addHead();

        if (isBorderClash(head) == true || isSnakeBodyClash(head) == true) {
            stopGame();
            if (mSnakeGameListener != null) mSnakeGameListener.onGameFinished();
        } else {
            eatFoodOrDeleteTail(head);
            mGameView.updateView(mSnake, mFood, mScore);
        }
    }

    private Rect addHead() {
        Rect newHead = null;
        Rect currHead = mSnake.get(0);

        if (mDirection == Direction.RIGHT) newHead = new Rect(currHead.left + mBlockSize, currHead.top, currHead.right + mBlockSize, currHead.bottom);
        else if (mDirection == Direction.LEFT) newHead = new Rect(currHead.left - mBlockSize, currHead.top, currHead.right - mBlockSize, currHead.bottom);
        else if (mDirection == Direction.UP) newHead = new Rect(currHead.left, currHead.top - mBlockSize, currHead.right, currHead.bottom - mBlockSize);
        else if (mDirection == Direction.DOWN) newHead = new Rect(currHead.left, currHead.top + mBlockSize, currHead.right, currHead.bottom + mBlockSize);

        mSnake.add(0, newHead);
        return newHead;
    }

    private void eatFoodOrDeleteTail(Rect head) {
        if (head.left == mFood.left && head.top == mFood.top) {
            mFood = generateNewFood();
            mScore++;
        } else {
            Iterator<Rect> iterator = mSnake.iterator();
            while (iterator.hasNext()) iterator.next(); // Getting last item
            iterator.remove();
        }
    }

    private boolean isBorderClash(Rect head) {
        return head.left < 0 || head.top < 0 || head.right > mScreenWidth || head.bottom > mScreenHeight;
    }

    private boolean isSnakeBodyClash(Rect head) {
        for (int i = 1; i < mSnake.size(); i++) {
            Rect currentElem = mSnake.get(i);
            if (head.left == currentElem.left && head.top == currentElem.top) return true;
        }

        return false;
    }
}
