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

## Play Console Assets
- Play Store icon (1024×1024): `play-assets/graphics/ic_launcher_playstore.png`
- Feature graphic (1024×500): `play-assets/graphics/feature-graphic-1024x500.png`
- Phone screenshots (1080×2412): `play-assets/screenshots/oppo-home.png`, `play-assets/screenshots/oppo-test.png`
- Store listing copy: short description `play-assets/store-listing/short-description_zh.txt`, full description `play-assets/store-listing/full-description_zh.txt`, release notes `play-assets/store-listing/release-notes_zh.txt`
- Privacy policy HTML: `play-assets/privacy-policy/privacy-policy.html` — publish to `https://swiftest.thucloud.com/privacy-policy.html` (or another publicly reachable URL) before submitting
- Confirm final package name/versionCode before upload; current default is `com.example.swiftestplus` with `versionCode=1`
- Plan to enroll in Google Play App Signing or store keystore securely for manual signing

## Immediate Next Steps
1. Smoke-test the signed APK on a clean device outside the VPN environment.
2. Upload the privacy policy HTML to the production website and verify the public URL.
3. Import assets into Play Console, upload `app-release.aab` plus `mapping.txt` to a closed testing track, then monitor vitals before promotion.
