package com.lgcast.sampler.remotecamera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.RemoteCameraControl;
import java.util.ArrayList;

public class StartActivity extends AppCompatActivity {
    private static final String TAG = "LGCAST Remote Camera";
    private ConnectableDevice mSelectedDevice;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);

        // 제일 먼저 DiscoveryManager 초기화를 한다.
        DiscoveryManager.init(this);

        // LG Cast를 지원하지 않는 OS 버전의 경우 관련 기능을 출력하지 않거나, 앱을 종료한다.
        if (RemoteCameraControl.isCompatibleOsVersion() == false) {
            Toast.makeText(this, getString(R.string.toast_unsupported_os_version), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 원격카메라 지원기기 검색 필터 설정
        Log.v(TAG, "Start discovery...");
        ArrayList<String> capabilities = new ArrayList<>();
        capabilities.add(RemoteCameraControl.RemoteCamera);
        CapabilityFilter filter = new CapabilityFilter(capabilities);

        // 기기 검색
        DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
        DiscoveryManager.getInstance().setCapabilityFilters(filter);
        DiscoveryManager.getInstance().registerDeviceService(WebOSTVService.class, SSDPDiscoveryProvider.class);
        DiscoveryManager.getInstance().start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 앱 종료 시 기기검색을 종료한다.
        Log.v(TAG, "Stop discovery...");
        DiscoveryManager.init(this);
        DiscoveryManager.getInstance().stop();
        DiscoveryManager.destroy();
    }

    public void onClickStartRemoteCamera(View v) {
        if (hasPermission() == true) selectTv();
        else requestPermission();
    }

    private boolean hasPermission() {
        Log.v(TAG, "Check permission...");
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        // 원격카메라는 CAMERA 및 RECORD_AUDIO 퍼미션을 필요로 한다.
        Log.v(TAG, "Request permission...");
        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(this, permissions, 0x1234);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        if (requestCode == 0x1234) {
            if (hasPermission() == true) selectTv();
            else Toast.makeText(this, getString(R.string.toast_allow_permission), Toast.LENGTH_LONG).show();
        }
    }

    public void selectTv() {
        AdapterView.OnItemClickListener listener = (adapter, parent, position, id) -> {
            mSelectedDevice = (ConnectableDevice) adapter.getItemAtPosition(position);

            // 필요한 경우 선택한 TV가 원격카메라를 지원하는지 확인할 수 있다.
            // 본 예제에서는 검색 단계에서 원격카메라를 지원하는 TV만 검색하였기 때문에 원격카메라 지원여부 확인은 불필요하다.
            if (RemoteCameraControl.isSupportRemoteCamera(mSelectedDevice.getId()) == true) startPreview();
            else Toast.makeText(this, getString(R.string.toast_not_support_device), Toast.LENGTH_SHORT).show();
        };

        // TV 검색 Picker 다이얼로그 출력
        AlertDialog dialog = new DevicePicker(this).getPickerDialog(getString(R.string.dialog_select_tv), listener);
        dialog.show();
    }

    private void startPreview() {
        Log.v(TAG, "startPreview");
        Intent intent = new Intent(this, CameraPreviewActivity.class);
        intent.putExtra(CameraPreviewActivity.EXTRA_DEVICE_IP_ADDRESS, mSelectedDevice.getIpAddress());
        intent.putExtra(CameraPreviewActivity.EXTRA_DEVICE_FRIENDLY_NAME, mSelectedDevice.getFriendlyName());
        startActivity(intent);
    }
}
