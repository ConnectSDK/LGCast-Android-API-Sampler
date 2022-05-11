package com.lgcast.sampler.screenmirroring;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.ScreenMirroringControl;
import java.util.ArrayList;

public class DeviceChooserActivity extends AppCompatActivity {
    private static final String TAG = "LGCAST (screen mirroring)";
    public static final int REQUEST_CODE_ACCESS_PERMISSIONS = 0x1000;
    public static final int REQUEST_CODE_CAPTURE_CONSENT = 0x2000;

    public static final String EXTRA_DEVICE_IP_ADDRESS = "EXTRA_DEVICE_IP_ADDRESS";
    public static final String EXTRA_PROJECTION_DATA = "EXTRA_PROJECTION_DATA";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_chooser_activity_layout);

        // Step 1: 앱 시작 시 초기화 및 TV 검색을 시작한다.

        // DiscoveryManager 초기화.
        DiscoveryManager.init(this);

        // 스크린미러링 (듀얼스크린) 지원기기 검색 필터 설정
        Log.v(TAG, "Start discovery...");
        ArrayList<String> capabilities = new ArrayList<>();
        capabilities.add(ScreenMirroringControl.ScreenMirroring);
        CapabilityFilter filter = new CapabilityFilter(capabilities);

        // 기기 검색
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
        DiscoveryManager.getInstance().setCapabilityFilters(filter);
        DiscoveryManager.getInstance().registerDeviceService(WebOSTVService.class, SSDPDiscoveryProvider.class);
        DiscoveryManager.getInstance().start();

        // 퍼미션 또는 Capture 동의를 구한다.
        if (hasPermission() == true) requestCaptureConsent();
        else requestPermission();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 앱 종료 시 기기검색을 종료한다.
        Log.v(TAG, "Stop discovery...");
        DiscoveryManager.getInstance().stop();
        DiscoveryManager.destroy();
    }

    // Step 2: 최초 사용 시 사용자에게 스크린 미러링에 필요한 사용권한 승인을 요청한다.
    private boolean hasPermission() {
        Log.v(TAG, "Check permission...");
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        // 스크린미러링은 오디오 캡쳐를 위해 RECORD_AUDIO 퍼미션을 필요로 한다.
        // 화면 캡쳐는 별도로 사용자 승인을 받기 때문에, 퍼미션은 필요하지 않다.
        Log.v(TAG, "Request permission...");
        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ACCESS_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        if (requestCode == REQUEST_CODE_ACCESS_PERMISSIONS) {
            if (hasPermission() == true) {
                requestCaptureConsent();
            } else {
                Toast.makeText(this, getString(R.string.toast_allow_permission), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Step 3: 화면 캡쳐를 위한 사용자 동의를 획득한다.
    private void requestCaptureConsent() {
        // 화면을 캡쳐하기 위해서는 사용자 승인이 필요하다.
        // 사용자 승인을 위한 시스템 다이얼로그를 출력한다.
        Log.v(TAG, "requestCaptureConsent");
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE_CONSENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v(TAG, "onActivityResult: requestCode=" + requestCode);

        if (requestCode == REQUEST_CODE_CAPTURE_CONSENT) {
            if (resultCode == Activity.RESULT_OK) {
                selectTv(data);
            } else {
                Toast.makeText(this, getString(R.string.toast_allow_capture), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Step 4: 스크린 미러링을 출력한 TV를 선택한다.
    private void selectTv(Intent projectionData) {
        AdapterView.OnItemClickListener listener = (adapter, parent, position, id) -> {
            ConnectableDevice tvDevice = (ConnectableDevice) adapter.getItemAtPosition(position);
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_IP_ADDRESS, tvDevice.getIpAddress());
            intent.putExtra(EXTRA_PROJECTION_DATA, projectionData);
            setResult(RESULT_OK, intent);
            finish();
        };

        // TV 검색 Picker 다이얼로그 출력
        AlertDialog dialog = new DevicePicker(this).getPickerDialog(getString(R.string.dialog_select_tv), listener);
        dialog.setOnCancelListener(d -> finish());
        dialog.setOnDismissListener(d -> finish());
        dialog.show();
    }
}
