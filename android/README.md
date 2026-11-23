# 연아연우시간표 — 안드로이드 앱 (WebView APK)

저장소 최상단의 웹앱(`index.html`·`manifest.json`·`sw.js`)을 그대로 담아 설치형 앱으로 만든다.
웹 쪽(GitHub Pages)은 그대로 두고, 이 `android/` 폴더만 앱 포장이다.

| 항목 | 값 |
| --- | --- |
| 앱 이름 | 연아연우시간표 |
| 패키지 | `com.kimtaeyoun.timetablekids` |
| versionCode / versionName | 1 / `1.0.0` |
| minSdk / targetSdk | 24 / 34 |

## 앱이 웹앱과 다른 점

- 웹 파일은 APK 안(`assets/www`)에 들어 있어 **인터넷이 없어도** 시간표를 열 수 있고,
  마지막으로 받은 시간표·학원비는 `localStorage` 캐시로 보인다.
- 구글 시트 연동(Apps Script `fetch` GET/POST)은 WebView 설정에서 `file://` →
  `https://script.google.com` 직접 통신을 허용해 웹과 동일하게 동작한다.
- `alert()`·`confirm()` 은 안드로이드 다이얼로그로 뜬다(일정·학원비 삭제 확인 포함).
- 키보드가 입력칸을 가리지 않도록 `adjustResize`, 뒤로가기는 모달 닫기 → 웹 뒤로가기 → 앱 종료 순서.

## 빌드

### Android Studio / Gradle

```
# android/ 폴더를 Android Studio 로 연다 (JDK 17, Android SDK 34 필요)
# Gradle 래퍼는 저장소에 넣지 않았으므로 Android Studio 가 쓰는 Gradle 로 빌드한다.
#   Android Studio: File > Open > android/  → Build > Build Bundle(s)/APK(s)
```

`copyWebAssets` 태스크가 저장소 최상단의 `index.html` 등을 `src/main/assets/www` 로 복사한다.

### Gradle 없이 (이 저장소에서 실제로 쓴 방법)

JDK 17 + build-tools 34 의 `aapt2`·`d8`·`zipalign`·`apksigner` 만으로 조립한다.
상세 스크립트는 `android/tools/build-apk.sh` 참고(서명 키는 저장소에 넣지 않는다).
