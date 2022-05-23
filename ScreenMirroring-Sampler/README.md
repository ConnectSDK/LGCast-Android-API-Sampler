LG Cast 스크린 미러링
====================
LG Cast 스크린 미러링은 앱의 스크린과 오디오를 TV로 출력하는 기능을 제공합니다.
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
스크린 미러링을 위해서는 RECORD_AUDIO 퍼미션을 필요로 하며, 앱 실행 시 이에 대한 사용자 동의를 득하여야 합니다.
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```
<br>


스크린 미러링 시작
------------------
스크린 미러링 시작 절차는 다음과 같은 순서로 진행합니다.
<br>


#### 1. Android 버전 확인
스크린미러링 기능은 Android 10 (Q) 버전부터 지원됩니다. 앱 실행 시 OS 버전을 확인하여 스크린 미러링이 제공 가능한지 확인하여야 합니다.
```java
if (ScreenMirroringControl.isCompatibleOsVersion() == false) {
    // OS 버전이 Android 10 미만으로
    // 스크린 미러링이 지원되지 않음.
}
```
<br>


#### 2. TV 검색
연결할 TV를 검색합니다. 검색시 스크린 미러링 기능을 지원하는 TV만 선별적으로 검색하기 위해 filter를 설정할 수 있습니다.
```java
// DiscoveryManager 초기화.
DiscoveryManager.init(this);

// 스크린미러링 (듀얼스크린) 지원기기 검색 필터 설정
ArrayList<String> capabilities = new ArrayList<>();
capabilities.add(ScreenMirroringControl.ScreenMirroring);
CapabilityFilter filter = new CapabilityFilter(capabilities);

// 기기 검색
DiscoveryManager.getInstance().setPairingLevel(DiscoveryManager.PairingLevel.ON);
DiscoveryManager.getInstance().setCapabilityFilters(filter);
DiscoveryManager.getInstance().registerDeviceService(WebOSTVService.class, SSDPDiscoveryProvider.class);
DiscoveryManager.getInstance().start();
```
<br>


#### 3. 퍼미션 확인
오디오 캡쳐를 위해 RECORD_AUDIO 퍼미션 사용자 승인을 필요로 합니다.  스크린 미러링 시작 전 오디오 사용권한을 확인하고, 권한이 없는 경우 다음과 같이 동의 절차를 진행합니다.
```java
// 퍼미션 요청
String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
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
    return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
}
```
<br>


#### 4. 화면캡쳐 사용자 동의
폰 화면을 캡쳐하기 위해서는 관련 시스템 다이얼로그를 출력하고, 사용자 승인을 받아야 합니다.
```java
// 화면캡쳐 사용자 승인을 위한 시스템 다이얼로그를 출력한다.
MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE_CONSENT);

// 사용자 승인 결과는 onActivityResult로 전달된다.
public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_CODE_CAPTURE_CONSENT) {
        if (resultCode == Activity.RESULT_OK) {
            // 사용자 승인 성공,
            // 인텐트 데이터(data)를 저장하여 LG Cast API에 전달하여야 함
            mProjectionData = data;
        } else {
            // 사용자 승인 실패
        }
    }
}
```
<br>


#### 5. TV 선택
검색된 TV 목록을 출력하고 스크린을 미러링할 TV를 선택합니다. TV 디바이스 선택 후 Screen Mirroring API 사용을 위한 ScreenMirroringControl 객체를 저장하여야 합니다.
```java
private ScreenMirroringControl mScreenMirroringControl;

AdapterView.OnItemClickListener listener = (adapter, parent, position, id) -> {
    ConnectableDevice connectableDevice = DiscoveryManager.getInstance().getDeviceByIpAddress(deviceIpAddress);
    mScreenMirroringControl = (connectableDevice != null) ? connectableDevice.getCapability(ScreenMirroringControl.class) : null;
    // 생략
};

// TV 검색 Picker 다이얼로그 출력
AlertDialog dialog = new DevicePicker(this).getPickerDialog(getString(R.string.dialog_select_tv), listener);
dialog.show();
```
<br>


#### 6. 미러링 시작
상기 절차가 완료되면 스크린 미러링을 실행할 수 있습니다. 처음으로 TV에 연결하는 경우 Paring이 필요합니다.  스크린 미러링 실행 중 다음과 같은 런타임 에러가 발생할 수 있습니다.
+ 네트워크 연결이 종료된 경우
+ TV가 종료된 경우
+ TV에서 Screen Mirroing이 종료된 경우
+ 폰 Notification으로 미러링 기능을 종료한 경우
+ 기타 예외상황 발생

이러한 에러에 대해서는 Lisener를 통해 전달받아 적절한 처리를 하여야 합니다.

```java
// 페어링 안내팝업
AlertDialog pairingAlert = new AlertDialog.Builder(this)
        .setTitle(getString(R.string.dialog_title_notice))
        .setCancelable(false)
        .setMessage(getString(R.string.dialog_allow_pairing))
        .setNegativeButton(android.R.string.ok, null)
        .create();

// 스크린미러링을 시작한다.
// 각 진행 단계는 콜백 함수를 통해 전달된다.
mScreenMirroringControl.startScreenMirroring(this, mProjectionData, new ScreenMirroringStartListener() {
    // TV에 처음 연결하는 경우 TV에서 모바일 연결에 대한 안내 팝업이 출력되며
    // 이를 사용자가 리모컨으로 [확인]하는 페이링 절차가 필요하다. (최초 1회)
    // 이 경우 앱에서는 이에 대한 안내 팝업을 출력하도록 한다.
    public void onPairing() {
        pairingAlert.show();
    }

    // 스크린미러링이 시작되면 호출되는 콜백함수이며
    // 성공 여부를 result 파라메터를 통해 전달한다.
    public void onStart(boolean result, Presentation secondScreen) {
        updateButtonVisibility();
        pairingAlert.dismiss();

        if (result == true) Toast.makeText(ScreenMirroringActivity.this, getString(R.string.toast_start_completed), Toast.LENGTH_SHORT).show();
        else Toast.makeText(ScreenMirroringActivity.this, getString(R.string.toast_start_failed), Toast.LENGTH_SHORT).show();
    }
});

// 스크린미러링 실행 중 예기치 않은 에러가 발생하면 호출되는 콜백함수이다.
// 에러가 발생하는 경우는 네트워크 연결이 끊어지거나, TV가 종료되는 경우 등이다.
mScreenMirroringControl.setErrorListener(this, error -> {
    // 에러 발생
});
```
<br>


스크린 미러링 종료
------------------
스크린 미러링을 종료하는 경우 다음과 같이 진행합니다.

```java
// 스크린미러링 종료.
mScreenMirroringControl.stopScreenMirroring(this, result -> {
    Toast.makeText(ScreenMirroringActivity.this, getString(R.string.toast_stopped), Toast.LENGTH_SHORT).show();
    updateButtonVisibility();
});

// 디바이스 검색 종료
DiscoveryManager.getInstance().stop();
DiscoveryManager.destroy();
```
