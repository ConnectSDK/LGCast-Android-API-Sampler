LG Cast 원격카메라
====================
LG Cast 원격카메라는 핸드폰의 카메라 프리뷰 영상을 TV로 출력하는 기능을 제공합니다. 이 기능을 이용하면 카메라가 없는 TV에서, 핸드폰의 카메라를 TV 카메라도 이용할 수 있습니다.
<br>


LG Cast SDK 설정
------------------
다음과 같이 Project gradle에 Connect SDK의 의존성을 설정합니다.
```gradle
dependencies {
    //.....
     implementation 'com.github.ConnectSDK:Connect-SDK-Android-Lite:-SNAPSHOT'
}
```
<br>


퍼미션 설정
------------------
원력 카메라 기능은 CAMERA 및 RECORD_AUDIO 퍼미션을 필요로 하며, 앱 실행 시 이에 대한 사용자 동의를 득하여야 합니다.
```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```
<br>


원력 카메라 시작
------------------
원력 카메라 시작 절차는 다음과 같은 순서로 진행합니다.
<br>


#### 1. 원격카메라 지원 여부 확인
원격카메라 기능은 Android 7 (API 24, N) 버전부터 지원됩니다. 앱 실행 시 OS 버전을 확인하여 원격카메라 가능 여부를 확인하고, 원격카메라 기능이 지원되지 않는 OS 버전의 경우 관련 기능을 출력하지 않거나, 앱을 종료합니다.

```java
if (RemoteCameraApi.getInstance().isCompatibleOsVersion() == false) {
    // OS 버전이 Android 7 미만으로
    // 원격카메라 기능이 지원되지 않음.
}
```
<br>


#### 2. TV 검색
홈네트워크에 연결된 TV를 검색합니다. 검색시 원격카메라 기능을 지원하는 TV만 선별적으로 검색하기 위해 filter를 설정할 수 있습니다.
```java
// DiscoveryManager 초기화.
DiscoveryManager.init(this);

// 원격카메라 지원기기 검색 필터 설정
ArrayList<String> capabilities = new ArrayList<>();
capabilities.add(RemoteCameraControl.RemoteCamera);
CapabilityFilter filter = new CapabilityFilter(capabilities);

// 기기 검색
DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
DiscoveryManager.getInstance().setCapabilityFilters(filter);
DiscoveryManager.getInstance().registerDeviceService(WebOSTVService.class, SSDPDiscoveryProvider.class);
DiscoveryManager.getInstance().start();
```
<br>


#### 3. 퍼미션 획득
원격카메라 기능은 android.permission.CAMERA 및 android.permission.RECORD_AUDIO  퍼미션을 필요로 합니다. 앱 최초 실행이 이 퍼미션에 대한 사용자 승인을 받아야 합니다.
```java
// 퍼미션 요청
String[] permissions = new String[]{android.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ACCESS_PERMISSIONS);

// 요청 결과는 onRequestPermissionsResult로 전달된다.
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == REQUEST_CODE_ACCESS_PERMISSIONS) {
        if (hasPermission() == true) {
            // 퍼미션 획득 성공
        } else {
            // 퍼미션 획득 실패
        }
    }
}

// 퍼미션 확인하기
private boolean hasPermission() {
   return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
}
```
<br>


#### 4. TV 선택
검색된 TV 목록을 출력하고 원격카메라를 실행할 TV를 선택합니다. TV 디바이스 선택 후 원격카메라 API 사용을 위한 RemoteCameraControl 객체를 얻어옵니다.
```java
private RemoteCameraControl mRemoteCameraControl ;

AdapterView.OnItemClickListener listener = (adapter, parent, position, id) -> {
    ConnectableDevice connectableDevice = (ConnectableDevice) adapter.getItemAtPosition(position);
    mRemoteCameraControl  = connectableDevice.getRemoteCameraControlControl();
    // 생략
};

// TV 검색 Picker 다이얼로그 출력
AlertDialog dialog = new DevicePicker(this).getPickerDialog(getString(R.string.dialog_select_tv), listener);
dialog.show();
```
<br>


#### 5. 원격카메라 실행
상기의 사전 작업이 완료되면 원격카메라를 실행할 수 있습니다.

먼저 카메라 미리보기를 출력할 Surface View 콤포넌트를 생성하고, 생성 완료 시 이의 Surface를 매개변수로 전달합니다. 만약 미리보기가 필요없는 경우 Surface를 null로 지정하도록 합니다.

이외, 마이크 음소거 여부, 카메라 렌즈 방향 등의 초기값을 정하여 매개변수로 전달합니다.  TV에 최초 연결 시 Paring 절차가 필요하며, 이에 대해 사용자에게 안내를 제공한다.
```java
// 카메라 Preview 출력할 SurfaceView를 만들도록 한다.
SurfaceView surfaceView = findViewById(R.id.surfaceView);
SurfaceHolder holder = surfaceView.getHolder();

holder.addCallback(new SurfaceHolder.Callback() {
    public void surfaceCreated(SurfaceHolder holder) {
        // SurfaceView가 생성되면 이를 인자로 넘겨
        // 원격카메라를 시작을 요청한다.
        startRemoteCamera(holder.getSurface());
    }

    // 생략
});

private void startRemoteCamera(Surface surface) {
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
        public void onPairing() {
            pairingAlert.show();
        }

        // 원격카메라가 시작되면 호출되는 콜백함수이며
        // 성공 여부를 result 파라메터를 통해 전달한다.
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
```
<br>


#### 6. 카메라 선택 및 프리뷰 시작
TV에서 폰 카메라를 선택하여 카메라 스트림 전송 및 재생이 시작된 경우 Callback으로 전달 받기위해 setCameraPlayingListener를 지정합니다.
```java
// TV에서 모바일을 선택하여 원격카메라 Preview 화면 전송이 시작되면
// 호출되는 콜백을 처리한다.
mRemoteCameraControl.setCameraPlayingListener(this, () -> {
    Toast.makeText(this, getString(R.string.toast_play_started), Toast.LENGTH_SHORT).show();
    mPlayingAlert.dismiss();
});
```
<br>


#### 7. 카메라 Property 변경
TV에서 밝기, AWB 등의 카메라 속성을 변경할 수 있고, setCameraPlayingListener 리스너를 지정하여 Callback을 전달받을 수 있습니다.
```java
// TV에서 카메라 Property를 변경하면 호출되는 콜백을 처리한다.
mRemoteCameraControl.setPropertyChangeListener(this, property -> {
    Toast.makeText(this, getString(R.string.toast_property_changed) + ": " + property, Toast.LENGTH_SHORT).show();
});
```
<br>


#### 8. 카메라 Property 변경
원격카메라가 실행된 후 다음과 같은 Run-Time error가 발생할 수 있습니다. 이러한 에러에 대해서는 리스너를 등록하여 실시간으로 전달받아 적절한 처리를 하여야 합니다.
  1) 네트워크 연결이 종료된 경우
  2) TV가 종료된 경우
  3) TV에서 Screen Mirroing이 종료된 경우
  4) 폰 Notification으로 미러링 기능을 종료한 경우
  5) 기타 예외상황 발생
```java
// 원격카메라 실행 중 예기치 않은 에러가 발생하면 호출되는 콜백함수이다.
// 에러가 발생하는 경우는 네트워크 연결이 끊어지거나, TV가 종료되는 경우 등이다.
mRemoteCameraControl.setErrorListener(this, error -> {
    Toast.makeText(this, getString(R.string.toast_running_error) + ": " + error, Toast.LENGTH_SHORT).show();
    mPlayingAlert.dismiss();
});
```
<br>


#### 9. 마이크 음소거 변경
마이크 음소거 여부를 변경한 경우 음소거 여부를 전달하여야 합니다. 앱에서는 현재 음소거 설정 값을 유지하여야 합니다.
```java
mRemoteCameraControl.setMicMute(this, mMicMute); // true or false
```
<br>


#### 10. 전/후 렌즈 변경하기
카메라 렌즈의 전/후 방향을 변경한 경우 카메라 방향을 전달하여야 합니다. 앱에서는 현재 카메라 방향 값을 유지하여야 합니다.
```java
mRemoteCameraControl.setLensFacing(this, mLensFacing); // RemoteCameraApi.LENS_FACING_BACK or RemoteCameraApi.LENS_FACING_FRONT
```
<br>


원격카메라 종료
------------------
사용자가 원격카메라를 종료하는 경우 startRemoteCamera을 호출합니다.
```java
mRemoteCameraControl.stopRemoteCamera(this, result->{
    // 생략
});
```
