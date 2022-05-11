package com.lgcast.sampler.dualscreen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Presentation;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.ScreenMirroringControl;
import com.lgcast.sampler.snakegame.SnakeGameControl;

public class SnakeGameActivity extends AppCompatActivity {
    private static final String TAG = "LGCAST (dual screen)";

    private SnakeGameControl mSnakeGameControl;
    private ScreenMirroringControl mScreenMirroringControl;
    private SnakeGameSecondScreen mSnakeGameSecondScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.snake_game_activity_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // LG Cast를 지원하지 않는 OS 버전의 경우 관련 기능을 출력하지 않거나, 앱을 종료하여야 한다.
        if (ScreenMirroringControl.isCompatibleOsVersion() == false) {
            Toast.makeText(this, getString(R.string.toast_unsupported_os_version), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mSnakeGameControl = new SnakeGameControl(this, findViewById(R.id.firstGameView));
        mSnakeGameControl.startGame();

        mSnakeGameControl.setListener(() -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.gameFinished));
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.retry, (dialog, id) -> mSnakeGameControl.restartGame());
                builder.setNegativeButton(R.string.exit, (dlgIf, index) -> finish());
                builder.show();
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 예기치 않게 앱이 종료되는 경우에도 스크린미러링이 종료될 수 있도록
        // 앱 종료 시 stopMirroring을 호출하도록 한다.
        if (mScreenMirroringControl != null) mScreenMirroringControl.stopScreenMirroring(this, null);
        mScreenMirroringControl = null;

        if (mSnakeGameControl != null) mSnakeGameControl.stopGame();
        mSnakeGameControl = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        mSnakeGameControl.pauseGame();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSnakeGameControl.resumeGame();
        updateButtonVisibility();
    }

    @Override
    public void onBackPressed() {
        mSnakeGameControl.pauseGame();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.queryExit);
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> finish());
        builder.setNegativeButton(android.R.string.no, (dialog, which) -> mSnakeGameControl.resumeGame());
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0x1234 && resultCode == Activity.RESULT_OK) {
            String deviceId = data.getStringExtra(DeviceChooserActivity.EXTRA_DEVICE_ID);
            Intent projectionData = data.getParcelableExtra(DeviceChooserActivity.EXTRA_PROJECTION_DATA);
            ConnectableDevice connectableDevice = DiscoveryManager.getInstance().getDeviceById(deviceId);
            mScreenMirroringControl = (connectableDevice != null) ? connectableDevice.getScreenMirroringControl() : null;

            if (mScreenMirroringControl != null) startMirroring(projectionData);
            else Toast.makeText(this, getString(R.string.toast_error), Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStartDualScreen(View view) {
        startActivityForResult(new Intent(this, DeviceChooserActivity.class), 0x1234);
    }

    public void onClickStopDualScreen(View view) {
        stopMirroring();
    }

    public void onClickButton(View view) {
        switch (view.getId()) {
            case R.id.btnRight:
            case R.id.btnRight2nd:
                mSnakeGameControl.changeDirection(SnakeGameControl.Direction.RIGHT);
                break;
            case R.id.btnLeft:
            case R.id.btnLeft2nd:
                mSnakeGameControl.changeDirection(SnakeGameControl.Direction.LEFT);
                break;
            case R.id.btnUp:
            case R.id.btnUp2nd:
                mSnakeGameControl.changeDirection(SnakeGameControl.Direction.UP);
                break;
            case R.id.btnDown:
            case R.id.btnDown2nd:
                mSnakeGameControl.changeDirection(SnakeGameControl.Direction.DOWN);
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void startMirroring(Intent projectionData) {
        Log.v(TAG, "startMirroring");
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.dialog_connecting_tv));
        progress.show();

        Log.v(TAG, "Stop game");
        mSnakeGameControl.stopGame();

        android.app.AlertDialog pairingAlert = new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_notice))
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_allow_pairing))
                .setNegativeButton(android.R.string.ok, null)
                .create();

        // 스크린 미러링을 시작한다.
        // 각 진행 단계는 콜백 함수를 통해 전달된다.
        mScreenMirroringControl.startScreenMirroring(this, projectionData, SnakeGameSecondScreen.class, new ScreenMirroringControl.ScreenMirroringStartListener() {
            // TV에 처음 연결하는 경우 TV에서 모바일 연결에 대한 안내 팝업이 출력되며
            // 이를 사용자가 리모컨으로 [확인]하는 페이링 절차가 필요하다. (최초 1회)
            // 이 경우 앱에서는 이에 대한 안내 팝업을 출력하도록 한다.
            @Override
            public void onPairing() {
                pairingAlert.show();
            }

            // 스크린미러링이 시작되면 호출되는 콜백함수이며
            // 성공 여부를 result 파라메터를 통해 전달한다.
            @Override
            public void onStart(boolean result, Presentation secondScreen) {
                updateButtonVisibility();
                pairingAlert.dismiss();
                progress.dismiss();

                if (result == true) Toast.makeText(getBaseContext(), getString(R.string.toast_start_completed), Toast.LENGTH_SHORT).show();
                else Toast.makeText(getBaseContext(), getString(R.string.toast_start_failed), Toast.LENGTH_SHORT).show();

                if (secondScreen != null) {
                    mSnakeGameSecondScreen = (SnakeGameSecondScreen) secondScreen;
                    mSnakeGameControl = mSnakeGameSecondScreen.getSnakeGameControl();
                    mSnakeGameSecondScreen.setOnDismissListener(dialog -> finish());
                }
            }
        });

        // 스크린미러링 실행 중 예기치 않은 에러가 발생하면 호출되는 콜백함수이다.
        // 에러가 발생하는 경우는 네트워크 연결이 끊어지거나, TV가 종료되는 경우 등이다.
        mScreenMirroringControl.setErrorListener(this, error -> {
            String s = getString(R.string.toast_running_error) + "\n(" + error + ")";
            Toast.makeText(getBaseContext(), s, Toast.LENGTH_SHORT).show();
            updateButtonVisibility();
        });
    }

    private void stopMirroring() {
        Log.v(TAG, "stopMirroring");

        // 스크린미러링을 종료한다. 정상 종료여부는 result 파라메터를 통해 전달된다.
        // 비정상적 종료의 경우는 스크린미러링이 실행되지 않은 상태에서 종료하는 경우 등이다.
        mScreenMirroringControl.stopScreenMirroring(this, result -> {
            Toast.makeText(getBaseContext(), getString(R.string.toast_stopped), Toast.LENGTH_SHORT).show();
            updateButtonVisibility();
        });
    }

    private void updateButtonVisibility() {
        if (ScreenMirroringControl.isRunning(this) == true) {
            findViewById(R.id.dualScreenStartButton).setVisibility(View.GONE);
            findViewById(R.id.dualScreenStopButton).setVisibility(View.VISIBLE);
            findViewById(R.id.firstGameView).setVisibility(View.GONE);
            findViewById(R.id.firstScreenControl).setVisibility(View.GONE);
            findViewById(R.id.secondScreenControl).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.dualScreenStartButton).setVisibility(View.VISIBLE);
            findViewById(R.id.dualScreenStopButton).setVisibility(View.GONE);
            findViewById(R.id.firstGameView).setVisibility(View.VISIBLE);
            findViewById(R.id.firstScreenControl).setVisibility(View.VISIBLE);
            findViewById(R.id.secondScreenControl).setVisibility(View.GONE);
        }
    }
}
