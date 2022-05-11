package com.lgcast.sampler.screenmirroring;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Presentation;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.ScreenMirroringControl;
import com.connectsdk.service.capability.ScreenMirroringControl.ScreenMirroringStartListener;
import com.connectsdk.service.webos.lgcast.common.utils.AppUtil;
import com.connectsdk.service.webos.lgcast.common.utils.DeviceUtil;
import com.connectsdk.service.webos.lgcast.common.utils.Logger;
import com.connectsdk.service.webos.lgcast.common.utils.StringUtil;
import com.connectsdk.service.webos.lgcast.screenmirroring.service.MirroringServiceFunc;

public class ScreenMirroringActivity extends AppCompatActivity {
    private static final String TAG = "LGCAST (screen mirroring)";

    private ScreenMirroringControl mScreenMirroringControl;
    private SimpleMediaPlayer mMediaPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screenmirroring_activity_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // LG Cast를 지원하지 않는 OS 버전의 경우 관련 기능을 출력하지 않거나, 앱을 종료하여야 한다.
        if (ScreenMirroringControl.isCompatibleOsVersion() == false) {
            Toast.makeText(this, getString(R.string.toast_unsupported_os_version), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            Log.v(TAG, "Open asset file descriptor");
            AssetFileDescriptor descriptor = getAssets().openFd("lg_donington_H264_800x480_15fpsR.mp4");
            mMediaPlayer = new SimpleMediaPlayer(findViewById(R.id.playerSurface));
            mMediaPlayer.play(descriptor);
            descriptor.close();
        } catch (Exception e) {
            Logger.error(e);
            Toast.makeText(this, getString(R.string.toast_error) + " : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (AppConfig.SHOW_DEVICE_INFO == true) {
            StringBuffer sb = new StringBuffer();
            sb.append("LGCast SDK Version : " + ScreenMirroringControl.getSdkVersion(this) + "\n");
            sb.append("Manufacturer : " + Build.MANUFACTURER + "\n");
            sb.append("Model Name : " + Build.MODEL + "\n");
            sb.append("Processor bits: " + DeviceUtil.getProcessorBits() + "\n");
            sb.append("Total RAM: " + StringUtil.toHumanReadableSize(DeviceUtil.getTotalMemorySpace(this)) + "\n");
            sb.append("Free RAM: " + StringUtil.toHumanReadableSize(DeviceUtil.getFreeMemorySpace(this)) + "\n");

            Point displaySize = AppUtil.getDisplaySizeInLandscape(this);
            sb.append("Display size: " + displaySize.x + " x " + displaySize.y + "\n");

            if (MirroringServiceFunc.isCaptureByDisplaySize(this) == true) sb.append("Capture size: " + displaySize.x + " x " + displaySize.y + "");
            else sb.append("Capture size: " + 1920 + " x " + 1080 + "");

            TextView displayInfo = findViewById(R.id.displayInfo);
            displayInfo.setText(sb.toString());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 예기치 않게 앱이 종료되는 경우에도 스크린미러링이 종료될 수 있도록
        // 앱 종료 시 stopMirroring을 호출하도록 한다.
        if (mScreenMirroringControl != null) mScreenMirroringControl.stopScreenMirroring(this, null);
        mScreenMirroringControl = null;

        if (mMediaPlayer != null) mMediaPlayer.stop();
        mMediaPlayer = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMediaPlayer.resume();
        updateButtonVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMediaPlayer.pause();
    }

    @Override
    public void onBackPressed() {
        if (ScreenMirroringControl.isRunning(this) == false) {
            super.onBackPressed();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(getString(R.string.dialog_title_notice));
        builder.setMessage(getString(R.string.dialog_close_on_mirroring));
        builder.setPositiveButton(android.R.string.ok, (dialog, index) -> finish());
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0x1234 && resultCode == Activity.RESULT_OK) {
            String deviceIpAddress = data.getStringExtra(DeviceChooserActivity.EXTRA_DEVICE_IP_ADDRESS);
            Intent projectionData = data.getParcelableExtra(DeviceChooserActivity.EXTRA_PROJECTION_DATA);
            ConnectableDevice connectableDevice = DiscoveryManager.getInstance().getDeviceByIpAddress(deviceIpAddress);
            mScreenMirroringControl = (connectableDevice != null) ? connectableDevice.getScreenMirroringControl() : null;

            if (mScreenMirroringControl != null) startMirroring(projectionData);
            else Toast.makeText(this, getString(R.string.toast_error), Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStartMirroring(View v) {
        if (AppConfig.USE_ACCESSBILITY_SERVICE_UIBC == true && ScreenMirroringControl.isUibcEnabled(this) == false) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.accessibility_service_label));
            builder.setMessage(getString(R.string.accessibility_user_popup));
            builder.setPositiveButton(android.R.string.yes, (dialog, index) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
            builder.setNegativeButton(android.R.string.no, (dialog, index) -> startActivityForResult(new Intent(this, DeviceChooserActivity.class), 0x1234));
            builder.show();
        } else {
            startActivityForResult(new Intent(this, DeviceChooserActivity.class), 0x1234);
        }
    }

    public void onClickStopMirroring(View v) {
        stopMirroring();
    }

    public void onClickDrawingBoard(View v) {
        startActivity(new Intent(this, DrawingBoardActivity.class));
    }

    private void startMirroring(Intent projectionData) {
        Log.v(TAG, "startMirroring");
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.dialog_connecting_tv));
        progress.show();

        AlertDialog pairingAlert = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_notice))
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_allow_pairing))
                .setNegativeButton(android.R.string.ok, null)
                .create();

        // 스크린미러링을 시작한다.
        // 각 진행 단계는 콜백 함수를 통해 전달된다.
        mScreenMirroringControl.startScreenMirroring(this, projectionData, new ScreenMirroringStartListener() {
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

                if (result == true) Toast.makeText(ScreenMirroringActivity.this, getString(R.string.toast_start_completed), Toast.LENGTH_SHORT).show();
                else Toast.makeText(ScreenMirroringActivity.this, getString(R.string.toast_start_failed), Toast.LENGTH_SHORT).show();
            }
        });

        // 스크린미러링 실행 중 예기치 않은 에러가 발생하면 호출되는 콜백함수이다.
        // 에러가 발생하는 경우는 네트워크 연결이 끊어지거나, TV가 종료되는 경우 등이다.
        mScreenMirroringControl.setErrorListener(this, error -> {
            String s = getString(R.string.toast_running_error) + "\n(" + error + ")";
            Toast.makeText(ScreenMirroringActivity.this, s, Toast.LENGTH_SHORT).show();
            updateButtonVisibility();
        });
    }

    private void stopMirroring() {
        Log.v(TAG, "stopMirroring");
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.dialog_disconnecting_tv));
        progress.show();

        // 스크린미러링을 종료한다. 정상 종료여부는 result 파라메터를 통해 전달된다.
        // 비정상적 종료의 경우는 스크린미러링이 실행되지 않은 상태에서 종료하는 경우 등이다.
        mScreenMirroringControl.stopScreenMirroring(this, result -> {
            Toast.makeText(ScreenMirroringActivity.this, getString(R.string.toast_stopped), Toast.LENGTH_SHORT).show();
            updateButtonVisibility();
            progress.dismiss();
        });
    }

    private void updateButtonVisibility() {
        if (ScreenMirroringControl.isRunning(this) == true) {
            findViewById(R.id.mirroringStartButton).setVisibility(View.GONE);
            findViewById(R.id.mirroringStopButton).setVisibility(View.VISIBLE);
            findViewById(R.id.drawingBoardButton).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.mirroringStartButton).setVisibility(View.VISIBLE);
            findViewById(R.id.mirroringStopButton).setVisibility(View.GONE);
            findViewById(R.id.drawingBoardButton).setVisibility(View.GONE);
        }
    }
}
