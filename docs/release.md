# Release builds

## Signing key

| | |
| --- | --- |
| Keystore | `keystore/durtles-reborn-release.p12` (PKCS12) |
| Alias | `durtlesreborn` |
| Key | RSA 4096, SHA384withRSA |
| Valid until | 2054-03-18 |
| Certificate SHA-256 | `C5:0F:C4:C7:A2:DC:5E:FC:8C:07:5D:C0:2A:48:6E:11:E5:19:14:0C:29:1D:14:CF:34:97:A0:A8:35:3A:51:2D` |
| Passwords | `keystore.properties` (store and key use the same password) |

Both files are git-ignored. **Back up the keystore and the password somewhere safe and offline.**
If either is lost, the app can never be updated under this identity again on Play, F-Droid or
GitHub: Android refuses an update signed with a different key. Never commit either file, and never
paste the password into an issue, a commit or a store listing.

## Build

```bash
./gradlew assembleRelease bundleRelease
```

| Artifact | Path | Use |
| --- | --- | --- |
| APK | `app/build/outputs/apk/release/app-release.apk` | GitHub releases, sideloading, F-Droid (they rebuild it) |
| AAB | `app/build/outputs/bundle/release/app-release.aab` | Google Play upload |

The release build runs R8 (`isMinifyEnabled`), resource shrinking and `lintVitalRelease`. The
signing config is read from `keystore.properties` at configuration time. Without that file the
release build is simply left unsigned and still builds, which is what a store that signs from
source (F-Droid) needs.

## Every upload

1. Bump `versionCode` in `app/build.gradle.kts` (Play refuses a code it has already seen).
   `versionName` is what users see; change it when the release is worth announcing.
2. Run the two Gradle tasks above.
3. Check the signature:

   ```bash
   "$ANDROID_HOME/build-tools/36.0.0/apksigner.bat" verify --print-certs \
       app/build/outputs/apk/release/app-release.apk
   ```

   The SHA-256 digest must be the one in the table above.
4. Record the SHA-256 of the artifacts if you publish checksums.

## Installing over an existing build

The debug APK and any older release APK are signed with different keys, so Android refuses to
install the signed release over them. Uninstall the old app first — that deletes its database,
settings and cached audio — or keep using the debug build for day-to-day testing.

## Before the first Play upload

- The applicationId is still `com.example.durtlesreborn`. Play does not accept `com.example.*`;
  changing it is a new install identity, so decide before the first public release.
- The app name is a working title; it must not contain "WaniKani" and should not be too close to
  "Durtles".
- Confirm the store description says the app is unofficial and third-party.
