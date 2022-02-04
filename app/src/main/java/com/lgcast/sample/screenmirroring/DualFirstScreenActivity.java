package com.lgcast.sample.screenmirroring;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.LGCastControl;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.webos.lgcast.screenmirroring.ScreenMirroringHelper;
import com.connectsdk.service.webos.lgcast.screenmirroring.SecondScreen;
import com.lgcast.sample.utils.CommUtil;
import com.lgcast.sample.utils.SimpleMediaPlayer;
import java.util.ArrayList;
import java.util.List;

public class DualFirstScreenActivity extends AppCompatActivity {
    private static final String TAG = "Dual Screen";
    private static final int REQUEST_CODE_CAPTURE_PERMISSION = 0x2000;
    private static final int REQUEST_CODE_CAPTURE_CONSENT = 0x3000;

    private WebOSTVService mWebOSTVService;
    private SimpleMediaPlayer mMediaPlayer;
    private DualSecondScreenActivity mSecondScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.firstscreen_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!ScreenMirroringHelper.isOsCompatible()) {
            Toast.makeText(this, getString(R.string.toast_unsupported_device), Toast.LENGTH_LONG).show();
            finish();
        }

        List<String> capabilities = new ArrayList<>();
        capabilities.add(LGCastControl.ScreenMirroring);
        CapabilityFilter filter = new CapabilityFilter(capabilities);

        Log.v(TAG, "Start discovery...");
        DiscoveryManager.init(this);
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
        DiscoveryManager.getInstance().setCapabilityFilters(filter);
        DiscoveryManager.getInstance().start();

        mMediaPlayer = new SimpleMediaPlayer(this, findViewById(R.id.fsPlayerSurface));
        mMediaPlayer.play("http://connectsdk.com/ConnectSDK.mp4");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (mSecondScreen != null) mSecondScreen.stop();
        mSecondScreen = null;

        if (mMediaPlayer != null) mMediaPlayer.stop();
        mMediaPlayer = null;

        Log.v(TAG, "Stop discovery...");
        DiscoveryManager.getInstance().stop();
        DiscoveryManager.destroy();
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
        if (ScreenMirroringHelper.isRunning(this)) CommUtil.showDialog(this, false, getString(R.string.dialog_title_notice), getString(R.string.dialog_message_process), index -> finish(), index -> Log.v(TAG, ""));
        else super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v(TAG, "onRequestPermissionsResult. requestCode=" + requestCode);

        if (requestCode == REQUEST_CODE_CAPTURE_PERMISSION) {
            if (hasCapturePermission()) chooseDevice();
            else Toast.makeText(this, getString(R.string.toast_need_permission), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v(TAG, "onActivityResult. requestCode=" + requestCode);

        if (requestCode == REQUEST_CODE_CAPTURE_CONSENT) {
            if (resultCode == RESULT_OK) startMirroring(data);
            else Log.v(TAG, "User didn't consent Screen Capture...");
        }
    }

    public void onClickStartMirroring(View v) {
        if (ScreenMirroringHelper.isRunning(this)) {
            Toast.makeText(this, getString(R.string.toast_already_started), Toast.LENGTH_SHORT).show();
        } else {
            if (hasCapturePermission()) chooseDevice();
            else requestCapturePermission();
        }
    }

    public void onClickSecondScreenPause(View v) {
        mSecondScreen.pause();
        findViewById(R.id.fsSecondScreenPause).setVisibility(View.GONE);
        findViewById(R.id.fsSecondScreenResume).setVisibility(View.VISIBLE);
    }

    public void onClickSecondScreenResume(View v) {
        mSecondScreen.resume();
        findViewById(R.id.fsSecondScreenPause).setVisibility(View.VISIBLE);
        findViewById(R.id.fsSecondScreenResume).setVisibility(View.GONE);
    }

    public void onClickSecondScreenStop(View v) {
        if (ScreenMirroringHelper.isRunning(this)) stopMirroring();
        else Toast.makeText(this, getString(R.string.toast_not_started), Toast.LENGTH_SHORT).show();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private boolean hasCapturePermission() {
        Log.v(TAG, "hasCapturePermission");
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCapturePermission() {
        Log.v(TAG, "requestCapturePermission");
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_CAPTURE_PERMISSION);
    }

    private void chooseDevice() {
        Log.v(TAG, "chooseDevice");
        AdapterView.OnItemClickListener listener = (adapter, parent, position, id) -> {
            ConnectableDevice device = (ConnectableDevice) adapter.getItemAtPosition(position);
            mWebOSTVService = (WebOSTVService) device.getServiceByName(WebOSTVService.ID);
            requestCaptureConsent();
        };

        AlertDialog dialog = new DevicePicker(this).getPickerDialog(getString(R.string.dialog_message_choose), listener);
        dialog.show();
    }

    private void requestCaptureConsent() {
        Log.v(TAG, "requestCaptureConsent");
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE_CONSENT);
    }

    private void startMirroring(Intent projectionData) {
        Log.v(TAG, "startMirroring");
        if (mWebOSTVService == null) return;

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.dialog_message_start));
        progress.show();

        AlertDialog pairingAlert = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_notice))
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_message_pairing))
                .setNegativeButton(android.R.string.ok, null)
                .create();

        mWebOSTVService.startScreenMirroring(this, projectionData, DualSecondScreenActivity.class, new LGCastControl.ScreenMirroringStartListener() {
            @Override
            public void onPairing() {
                pairingAlert.show();
            }

            @Override
            public void onSuccess(SecondScreen secondScreen) {
                Toast.makeText(getBaseContext(), getString(R.string.toast_start), Toast.LENGTH_LONG).show();
                updateButtonVisibility();
                pairingAlert.dismiss();
                progress.dismiss();

                if (secondScreen != null) {
                    mSecondScreen = (DualSecondScreenActivity) secondScreen;
                    mSecondScreen.start(mMediaPlayer.getContentUrl(), mMediaPlayer.getCurrentPosition());

                    Log.v(TAG, "Stop 1st media player");
                    mMediaPlayer.stop();

                    findViewById(R.id.fsSecondScreenPause).setVisibility(View.VISIBLE);
                    findViewById(R.id.fsSecondScreenResume).setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Toast.makeText(DualFirstScreenActivity.this, getString(R.string.toast_start_fail), Toast.LENGTH_LONG).show();
                Log.v(TAG, "Mirroring error: " + error.getMessage());
                updateButtonVisibility();
                pairingAlert.dismiss();
                progress.dismiss();
            }
        });
    }

    private void stopMirroring() {
        Log.v(TAG, "stopMirroring");
        if (mWebOSTVService == null) return;

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.dialog_message_stop));
        progress.show();

        mWebOSTVService.stopScreenMirroring(this, new LGCastControl.ScreenMirroringStopListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(DualFirstScreenActivity.this, getString(R.string.toast_stop), Toast.LENGTH_LONG).show();
                updateButtonVisibility();
                progress.dismiss();

                Log.v(TAG, "Play 1st media player again");
                String contentUrl = mSecondScreen.getContentUrl();
                int currentPosition = mSecondScreen.getCurrentPosition();
                mMediaPlayer.play(contentUrl, currentPosition);
            }

            @Override
            public void onError(ServiceCommandError error) {
            }
        });
    }

    private void updateButtonVisibility() {
        if (ScreenMirroringHelper.isRunning(this)) {
            findViewById(R.id.fsPlayerSurface).setVisibility(View.GONE);
            findViewById(R.id.fsMirroringButton).setVisibility(View.GONE);
            findViewById(R.id.fsControlButton).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.fsPlayerSurface).setVisibility(View.VISIBLE);
            findViewById(R.id.fsMirroringButton).setVisibility(View.VISIBLE);
            findViewById(R.id.fsControlButton).setVisibility(View.GONE);
        }
    }
}
