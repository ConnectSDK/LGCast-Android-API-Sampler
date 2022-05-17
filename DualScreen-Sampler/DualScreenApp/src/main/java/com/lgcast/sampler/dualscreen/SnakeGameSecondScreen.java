package com.lgcast.sampler.dualscreen;

import android.app.AlertDialog;
import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.connectsdk.service.webos.lgcast.common.utils.TimerUtil;
import com.lgcast.sampler.snakegame.SnakeGameControl;
import com.lgcast.sampler.snakegame.SnakeGameListener;
import java.util.Timer;

public class SnakeGameSecondScreen extends Presentation implements SnakeGameListener {
    private static final String TAG = "LGCAST (dual screen)";

    private Context mOuterContext;
    private SnakeGameControl mSnakeGameControl;

    private TextView mRefreshText;
    private Timer mRefeshTimer;

    public SnakeGameSecondScreen(@NonNull Context outerContext, @NonNull Display display) {
        super(outerContext, display);
        mOuterContext = outerContext;
    }

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.snake_game_second_screen_layout);

        mSnakeGameControl = new SnakeGameControl(getContext(), findViewById(R.id.secondGameView));
        mSnakeGameControl.setListener(this);
    }

    @Override
    public void show() {
        super.show();
        mSnakeGameControl.startGame();

        mRefreshText = findViewById(R.id.refreshText);
        mRefeshTimer = TimerUtil.schedule(() -> mRefreshText.setText("" + System.nanoTime()), 0, 15);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mSnakeGameControl.stopGame();

        if (mRefeshTimer != null) mRefeshTimer.cancel();
        mRefeshTimer = null;
    }

    @Override
    public void onGameFinished() {
        new Handler(Looper.getMainLooper()).post(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(mOuterContext);
            builder.setMessage(mOuterContext.getString(R.string.gameFinished));
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.retry, (dialog, id) -> mSnakeGameControl.restartGame());
            builder.setNegativeButton(R.string.exit, (dialog, index) -> dismiss());
            builder.show();
        });
    }

    public void changeDirection(SnakeGameControl.Direction direction) {
        mSnakeGameControl.changeDirection(direction);
    }
}
