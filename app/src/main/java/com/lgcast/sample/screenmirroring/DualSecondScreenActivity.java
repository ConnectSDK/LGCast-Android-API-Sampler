package com.lgcast.sample.screenmirroring;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import com.connectsdk.service.webos.lgcast.screenmirroring.SecondScreen;
import com.lgcast.sample.utils.SimpleMediaPlayer;

public class DualSecondScreenActivity extends SecondScreen {
    private static final String TAG = "Dual Screen";
    private final Context mContext;
    private SimpleMediaPlayer mMediaPlayer;

    public DualSecondScreenActivity(Context context, Display display) {
        super(context, display);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "[Second Screen] onCreate");
        setContentView(R.layout.secondscreen_activity);
        mMediaPlayer = new SimpleMediaPlayer(mContext, findViewById(R.id.ssPlayerSurface));
    }

    public void start(String url, int position) {
        Log.v(TAG, "[Second Screen] start");
        mMediaPlayer.play(url, position, true);
    }

    public void stop() {
        Log.v(TAG, "[Second Screen] stop");
        if (mMediaPlayer != null) mMediaPlayer.release();
        mMediaPlayer = null;
    }

    public void pause() {
        Log.v(TAG, "[Second Screen] pause");
        mMediaPlayer.pause();
    }

    public void resume() {
        Log.v(TAG, "[Second Screen] resume");
        mMediaPlayer.resume();
    }

    public String getContentUrl() {
        return mMediaPlayer.getContentUrl();
    }

    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }
}
