LG Cast 듀얼 스크린
====================
앱의 화면과 별도로 추가적으로 화면을 생성하여, 이를 TV에 출력하는 기능을 제공합니다.
즉, 폰에 나타나는 앱의 화면은 First Screen이 되고, TV에 출력되는 화면은 앱의 Second Screen이 되어 앱이 듀얼 스크린으로 동작하게 됩니다.

듀얼 스크린을 사용하기 위한 절차는 스크린 미러링과 동일하며
미러링 시작 시 사용자가 정의한 세컨드 스크린 클래스를 전달하면 됩니다.
<br>


듀얼 스크린 정의
------------------
듀얼 스크린을 위한 세컨드 클래스를 정의하기 위해서는 Android Presentation 클래스를 상속받습니다.
```java
public class SecondScreenDemo extends Presentation implements SnakeGameListener {
    private Context mOuterContext;

    public SecondScreenDemo(@NonNull Context outerContext, @NonNull Display display) {
        super(outerContext, display);
        mOuterContext = outerContext;
    }

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.snake_game_second_screen_layout);
    }
    // 생략
}
```
<br>


듀얼 스크린 실행
------------------
듀얼 스크린 실행은 사용자가 정의한 Presentation 상속 클래스를 지정하여 스크린 미러링을 시작합니다.

TV와 연결되면 스크린 미러링 SDK에서 Second Screen을 위한 가상의 Display를 생성하고
Second Screen 클래스의 인스턴스를 생성하여 이를 onStart 콜백으로 전달합니다.

이후 사용자는 Second Screen 클래스에 접근하여 듀얼 스크린을 실행 및 조작할 수 있습니다.
```java
mScreenMirroringControl.startScreenMirroring(this, projectionData, SecondScreenDemo.class, new ScreenMirroringControl.ScreenMirroringStartListener() {
    // 생략

    // 스크린미러링이 시작되면 호출되는 콜백함수이며
    // 성공 여부를 result 파라메터를 통해 전달한다.
    public void onStart(boolean result, Presentation secondScreen) {
        updateButtonVisibility();
        pairingAlert.dismiss();
        progress.dismiss();

        if (result == true) Toast.makeText(getBaseContext(), getString(R.string.toast_start_completed), Toast.LENGTH_SHORT).show();
        else Toast.makeText(getBaseContext(), getString(R.string.toast_start_failed), Toast.LENGTH_SHORT).show();

        if (secondScreen != null) {
            mSecondScreenDemo = (SecondScreenDemo) secondScreen;
            mSecondScreenDemo = mSecondScreenDemo.start();
        }
    }
});
```
