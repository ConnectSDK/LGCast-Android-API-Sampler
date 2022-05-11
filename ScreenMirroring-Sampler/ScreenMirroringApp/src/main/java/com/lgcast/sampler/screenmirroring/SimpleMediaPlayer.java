package com.lgcast.sampler.screenmirroring;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SimpleMediaPlayer {
    private MediaPlayer mMediaPlayer;
    private int mPausedPosition;

    public SimpleMediaPlayer(SurfaceView surfaceView) {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.reset();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
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
        });
    }

    public void play(AssetFileDescriptor descriptor) throws Exception {
        if (descriptor == null) throw new Exception("Invalid descriptor");

        if (mMediaPlayer.isPlaying() == true) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }

        mMediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
        descriptor.close();

        mMediaPlayer.setOnPreparedListener(mp -> mMediaPlayer.start());
        mMediaPlayer.setLooping(true);
        mMediaPlayer.prepare();
    }

    public void stop() {
        mMediaPlayer.stop();
    }

    public void pause() {
        mMediaPlayer.pause();
        mPausedPosition = mMediaPlayer.getCurrentPosition();
    }

    public void resume() {
        mMediaPlayer.setOnSeekCompleteListener(mp->mMediaPlayer.start());
        mMediaPlayer.seekTo(mPausedPosition);
    }
}
