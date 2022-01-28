package com.lgcast.sample.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class SimpleMediaPlayer {
    private static final String TAG = "LGCast Sampler";
    private final Context mContext;
    private final SurfaceView mSurfaceView;
    private final MediaPlayer mMediaPlayer;

    private int mPausedPosition;

    public SimpleMediaPlayer(Context context, SurfaceView surfaceView) {
        mContext = context;
        mSurfaceView = surfaceView;

        SurfaceHolder.Callback cb = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                mMediaPlayer.setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            }
        };

        mMediaPlayer = new MediaPlayer();
        mSurfaceView.getHolder().addCallback(cb);
    }

    public void play(String url) {
        play(url, 0, false);
    }

    public void play(String url, int position, boolean mute) {
        Log.v(TAG, "play url=" + url + ", position=" + position);
        if (url == null) throw new IllegalArgumentException("Invalid url");

        SimpleProgress progress = new SimpleProgress(mContext, "Loading...", false);
        progress.show();

        mSurfaceView.setVisibility(View.VISIBLE);

        CommUtil.runInBackground(() -> {
            try {
                mMediaPlayer.setOnPreparedListener(mp -> {
                    Log.d(TAG, "Seek to " + position);
                    mMediaPlayer.seekTo(position);
                });

                mMediaPlayer.setOnSeekCompleteListener(mp -> {
                    mMediaPlayer.start();
                    if (mute) mMediaPlayer.setVolume(0.0F, 0.0F);
                    progress.dismiss();
                });

                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(url);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
                progress.dismiss();
            }
        });
    }

    public void stop() {
        Log.v(TAG, "stop");
        mMediaPlayer.stop();
    }

    public void pause() {
        Log.v(TAG, "pause");
        mMediaPlayer.pause();
        mPausedPosition = mMediaPlayer.getCurrentPosition();
    }

    public void resume() {
        Log.v(TAG, "resume. mPausedPosition=" + mPausedPosition);
        mMediaPlayer.seekTo(mPausedPosition);
        mMediaPlayer.start();
    }
}
