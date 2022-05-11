package com.lgcast.sampler.remotecamera;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.RemoteCameraControl;
import com.connectsdk.service.capability.RemoteCameraControl.RemoteCameraStartListener;

public class CameraPreviewActivity extends AppCompatActivity {
    private static final String TAG = "LGCAST Remote Camera";
    public static final String EXTRA_DEVICE_IP_ADDRESS = "EXTRA_DEVICE_IP_ADDRESS";
    public static final String EXTRA_DEVICE_FRIENDLY_NAME = "EXTRA_DEVICE_FRIENDLY_NAME";

    private RemoteCameraControl mRemoteCameraControl;
    private boolean mMicMute = false;
    private int mLensFacing = RemoteCameraControl.LENS_FACING_FRONT;
    private AlertDialog mPlayingAlert;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camerapreview_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String deviceIpAddress = getIntent().getStringExtra(EXTRA_DEVICE_IP_ADDRESS);
        ConnectableDevice connectableDevice = (ConnectableDevice) DiscoveryManager.getInstance().getDeviceByIpAddress(deviceIpAddress);
        WebOSTVService webOSTVService = (connectableDevice != null) ? (WebOSTVService) connectableDevice.getServiceByName(WebOSTVService.ID) : null;
        mRemoteCameraControl = (webOSTVService != null) ? webOSTVService.getRemoteCameraControl() : null;
        if (mRemoteCameraControl == null) throw new IllegalArgumentException("Invalid Remote Camera Control");

        String deviceFriendlyName = getIntent().getStringExtra(EXTRA_DEVICE_FRIENDLY_NAME);
        String modelName = Build.MODEL;

        mPlayingAlert = new AlertDialog.Builder(CameraPreviewActivity.this)
                .setMessage(String.format(getString(R.string.dialog_select_camera), deviceFriendlyName, modelName))
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
                .create();

        // 카메라 Preview를 출력할 SurfaceView를 만들도록 한다.
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        SurfaceHolder holder = surfaceView.getHolder();

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // SurfaceView가 생성되면 이를 인자로 넘겨
                // 원격카메라를 시작을 요청한다.
                Log.v(TAG, "surfaceCreated");
                startRemoteCamera(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.v(TAG, "surfaceDestroyed");
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 예기치 않게 앱이 종료되는 경우에도 원격카메라가 종료될 수 있도록
        // 앱 종료 시 stopRemoteCamera를 호출하도록 한다.
        mRemoteCameraControl.stopRemoteCamera(this, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");

        // 다른 화면이나, 홈화면으로 이동하면
        // 원격카메라를 종료한다.
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        updateButtons();
    }

    public void onChangeMicMute(View v) {
        // 마이크 음소거를 toggle하여 이를 요청한다.
        Log.v(TAG, "onChangeMicMute");
        mMicMute = !mMicMute;
        mRemoteCameraControl.setMicMute(this, mMicMute);
        updateButtons();
    }

    public void onChangeLensFacing(View v) {
        // 카메라 렌즈 방향을 toggle하여 이를 요청한다.
        Log.v(TAG, "onChangeLensFacing");
        mLensFacing = (mLensFacing == RemoteCameraControl.LENS_FACING_FRONT) ? RemoteCameraControl.LENS_FACING_BACK : RemoteCameraControl.LENS_FACING_FRONT;
        mRemoteCameraControl.setLensFacing(this, mLensFacing);
        updateButtons();
    }

    public void onClosePreview(View v) {
        Log.v(TAG, "onClosePreview");
        finish();
    }

    private void startRemoteCamera(Surface surface) {
        Log.v(TAG, "Start remote camera...");
        AlertDialog pairingAlert = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_notice))
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_allow_pairing))
                .setNegativeButton(android.R.string.ok, null)
                .create();

        // 원격카메라를 시작한다.
        // 각 진행 단계는 콜백 함수를 통해 전달된다.
        mRemoteCameraControl.startRemoteCamera(this, surface, mMicMute, mLensFacing, new RemoteCameraStartListener() {
            // TV에 처음 연결하는 경우 TV에서 모바일 연결에 대한 안내 팝업이 출력되며
            // 이를 사용자가 리모컨으로 [확인]하는 페이링 절차가 필요하다. (최초 1회)
            // 이 경우 앱에서는 이에 대한 안내 팝업을 출력하도록 한다.
            @Override
            public void onPairing() {
                pairingAlert.show();
            }

            // 원격카메라가 시작되면 호출되는 콜백함수이며
            // 성공 여부를 result 파라메터를 통해 전달한다.
            @Override
            public void onStart(boolean result) {
                if (result == true) {
                    mPlayingAlert.show();
                } else {
                    Toast.makeText(CameraPreviewActivity.this, getString(R.string.toast_start_failed), Toast.LENGTH_SHORT).show();
                    finish();
                }
                pairingAlert.dismiss();
            }
        });

        // TV에서 모바일을 선택하여 원격카메라 Preview 화면 전송이 시작되면
        // 호출되는 콜백을 처리한다.
        mRemoteCameraControl.setCameraPlayingListener(this, () -> {
            Toast.makeText(this, getString(R.string.toast_play_started), Toast.LENGTH_SHORT).show();
            mPlayingAlert.dismiss();
        });

        // TV에서 카메라 Property를 변경하면 호출되는 콜백을 처리한다.
        mRemoteCameraControl.setPropertyChangeListener(this, property -> {
            Toast.makeText(this, getString(R.string.toast_property_changed) + ": " + property, Toast.LENGTH_SHORT).show();
        });

        // 원격카메라 실행 중 예기치 않은 에러가 발생하면 호출되는 콜백함수이다.
        // 에러가 발생하는 경우는 네트워크 연결이 끊어지거나, TV가 종료되는 경우 등이다.
        mRemoteCameraControl.setErrorListener(this, error -> {
            Toast.makeText(this, getString(R.string.toast_running_error) + ": " + error, Toast.LENGTH_SHORT).show();
            mPlayingAlert.dismiss();
        });
    }

    private void updateButtons() {
        Button micMuteButton = findViewById(R.id.micMuteButton);
        micMuteButton.setText((mMicMute == false) ? getString(R.string.buttion_turn_mic_off) : getString(R.string.buttion_turn_mic_on));
        micMuteButton.setBackgroundColor((mMicMute == false) ? Color.parseColor("#FFD863") : Color.LTGRAY);

        Button lensFacingButton = findViewById(R.id.lensFacingButton);
        lensFacingButton.setText((mLensFacing == RemoteCameraControl.LENS_FACING_FRONT) ? getString(R.string.buttion_change_lens_to_back) : getString(R.string.buttion_change_lens_to_front));
        lensFacingButton.setBackgroundColor((mLensFacing == RemoteCameraControl.LENS_FACING_FRONT) ? Color.parseColor("#FFD863") : Color.LTGRAY);
    }
}
