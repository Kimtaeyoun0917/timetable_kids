#!/usr/bin/env bash
# 연아연우시간표 APK 빌드 — Gradle 없이 JDK 17 + build-tools 34 로 조립한다.
#
# 필요한 환경 변수
#   JAVA_HOME    JDK 17 경로 (javac, keytool)
#   BUILD_TOOLS  android build-tools 폴더 (aapt2, d8, zipalign, apksigner)
#   ANDROID_JAR  platforms/android-34/android.jar
# 선택
#   KEYSTORE     서명 키 (기본: android/keystore/release.keystore)
#   KS_ALIAS     키 별칭 (기본: timetablekids)
#   KS_PASS      키 비밀번호 (키 파일을 새로 만들 때도 이 값 사용)
#   OUT          중간 산출물 폴더 (기본: android/build)
#
# 사용: android/tools/build-apk.sh [/출력/경로.apk]
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
ANDROID_DIR=$(dirname "$HERE")
REPO_DIR=$(dirname "$ANDROID_DIR")
MAIN="$ANDROID_DIR/app/src/main"

: "${JAVA_HOME:?JAVA_HOME 을 JDK 17 경로로 지정하세요}"
: "${BUILD_TOOLS:?BUILD_TOOLS 를 build-tools 폴더로 지정하세요}"
: "${ANDROID_JAR:?ANDROID_JAR 를 android.jar 경로로 지정하세요}"

KEYSTORE=${KEYSTORE:-$ANDROID_DIR/keystore/release.keystore}
KS_ALIAS=${KS_ALIAS:-timetablekids}
KS_PASS=${KS_PASS:?KS_PASS 를 지정하세요}
OUT=${OUT:-$ANDROID_DIR/build}
APK_OUT=${1:-$REPO_DIR/timetable_kids.apk}
PKG=com.kimtaeyoun.timetablekids
VERSION_CODE=1
VERSION_NAME=1.0.0

export PATH="$JAVA_HOME/bin:$PATH"

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/assets/www"

echo "== 1. 웹 자산 복사 (index.html·manifest.json·sw.js)"
cp -f "$REPO_DIR/index.html" "$REPO_DIR/manifest.json" "$REPO_DIR/sw.js" "$OUT/assets/www/"
ls -l "$OUT/assets/www"

echo "== 2. Java 컴파일"
javac -nowarn -source 11 -target 11 -classpath "$ANDROID_JAR" -d "$OUT/classes" \
  $(find "$MAIN/java" -name '*.java')

echo "== 3. dex 생성 (d8, min-api 24)"
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 24 --output "$OUT/dex" \
  $(find "$OUT/classes" -name '*.class')

echo "== 4. 리소스 컴파일 (aapt2 compile)"
"$BUILD_TOOLS/aapt2" compile --dir "$MAIN/res" -o "$OUT/res.zip"

echo "== 5. 리소스 링크 (aapt2 link) — 링크용 manifest 사본에 package 를 넣는다"
sed "s|<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">|<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"$PKG\">|" \
  "$MAIN/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"
grep -q "package=\"$PKG\"" "$OUT/AndroidManifest.xml"

"$BUILD_TOOLS/aapt2" link -o "$OUT/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$OUT/AndroidManifest.xml" \
  -R "$OUT/res.zip" \
  -A "$OUT/assets" \
  --min-sdk-version 24 \
  --target-sdk-version 34 \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --auto-add-overlay

echo "== 6. classes.dex 를 APK 에 넣기"
python3 - "$OUT" <<'PY'
import sys, zipfile, os
out = sys.argv[1]
apk = os.path.join(out, 'base.apk')
with zipfile.ZipFile(apk, 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(os.path.join(out, 'dex', 'classes.dex'), 'classes.dex')
with zipfile.ZipFile(apk) as z:
    print('entries:', z.namelist())
PY

echo "== 7. zipalign"
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/base.apk" "$OUT/aligned.apk"

echo "== 8. 서명 (v1+v2+v3)"
if [ ! -f "$KEYSTORE" ]; then
  mkdir -p "$(dirname "$KEYSTORE")"
  keytool -genkeypair -v -keystore "$KEYSTORE" -alias "$KS_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12 \
    -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -dname "CN=Timetable Kids, OU=Personal, O=Kimtaeyoun, L=Daejeon, C=KR" 2>&1 | tail -3
  echo "새 서명 키 생성: $KEYSTORE (다음 빌드도 같은 키를 쓰려면 이 파일을 보관하세요)"
fi

"$BUILD_TOOLS/apksigner" sign \
  --ks "$KEYSTORE" --ks-key-alias "$KS_ALIAS" \
  --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --min-sdk-version 21 \
  --out "$APK_OUT" "$OUT/aligned.apk"

echo "== 9. 서명 검증"
"$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$APK_OUT"
ls -l "$APK_OUT"
sha256sum "$APK_OUT"
