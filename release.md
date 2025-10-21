# Release Prep Progress

## Build & Signing
- Generated `app/swiftest-release.jks` and configured `signing.properties` for repeatable release builds (keystore excluded from VCS).
- Updated `app/build.gradle` to load the signing config, enable R8/resource shrinking, and require the signing file before release tasks run.
- Added TLS-related `-dontwarn` rules to `app/proguard-rules.pro` to keep OkHttp shrink-safe.
- Resolved duplicate view IDs in `app/src/main/res/layout/activity_main.xml`.

## Artifacts (2025-10-20)
- `./gradlew clean assembleRelease bundleRelease`
- APK: `app/build/outputs/apk/release/app-release.apk`  
  SHA-256: `85b4a7b5eb4a013d96d0c973abb15d833a039ba67ba1af9f1ef997e4536a4f71`
- AAB: `app/build/outputs/bundle/release/app-release.aab`  
  SHA-256: `9c787f74759eabf4e43d6be0d611d4fbd755562d83089e204a3179a2788f9171`
- Mapping bundle: `app/build/outputs/mapping/release/`

## Play Console Checklist
- Pending assets: hi-res icon export (1024×1024), feature graphic (1024×500), at least two phone screenshots, final short/full descriptions, privacy policy URL.
- Confirm final package name/versionCode before upload; current default is `com.example.swiftestplus` with `versionCode=1`.
- Plan to enroll in Google Play App Signing or store keystore securely for manual signing.

## Immediate Next Steps
1. Smoke-test the signed APK on a clean device outside the VPN environment.
2. Gather/store listing creatives and policy documentation.
3. Upload `app-release.aab` plus `mapping.txt` to a closed testing track, then monitor vitals before promotion.
