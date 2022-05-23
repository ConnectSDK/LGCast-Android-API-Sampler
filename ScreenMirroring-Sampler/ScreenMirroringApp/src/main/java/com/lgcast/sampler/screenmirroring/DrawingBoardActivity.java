package com.lgcast.sampler.screenmirroring;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class DrawingBoardActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private Path mPath;
    private Paint mPaint = new Paint();
    private final int[] mColors = new int[]{Color.WHITE, Color.GREEN, Color.MAGENTA, Color.BLUE, Color.RED, Color.YELLOW};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawingline_activity_layout);

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView.setZOrderOnTop(true);
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mSurfaceView.getHolder().addCallback(this);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(10);

        int colorIndex = new Random().nextInt(mColors.length);
        int currentColor = mColors[colorIndex];
        findViewById(R.id.surfaceBackground).setBackgroundColor(currentColor);
        findViewById(R.id.surfaceBackground).setBackgroundColor(Color.parseColor("#b5db92"));
    }

    @Override
    public void onBackPressed() {
        queryClose();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPath = new Path();
        mSurfaceView.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mPath.reset();
                    mPath.moveTo(event.getX(), event.getY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    mPath.lineTo(event.getX(), event.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    mPath.lineTo(event.getX(), event.getY());
                    Canvas canvas = mSurfaceView.getHolder().lockCanvas();
                    canvas.drawPath(mPath, mPaint);
                    mSurfaceView.getHolder().unlockCanvasAndPost(canvas);
                    break;
            }
            if (mPath != null) {
                Canvas canvas = mSurfaceView.getHolder().lockCanvas();
                canvas.drawPath(mPath, mPaint);
                mSurfaceView.getHolder().unlockCanvasAndPost(canvas);
            }
            return true;
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void onClickShare(View v) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    public void onClickClose(View v) {
        queryClose();
    }

    private void queryClose() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.dialog_title_notice);
        builder.setMessage(R.string.dialog_close);
        builder.setPositiveButton(android.R.string.ok, (dialog, index) -> finish());
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }
}
