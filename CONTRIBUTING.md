# Contributing

OSSFlow is free software under the GNU General Public License, version 3 only. Contributions are welcome under that licence.

## Build

A debug build does not need a keystore:

```
./gradlew :app:assembleDebug
```

Unit tests:

```
./gradlew :app:testDebugUnitTest
```

Release signing needs `keystore.properties` in the project root. Do not commit keystores, `keystore.properties`, or a replacement `OSSFlow.apk`. The APK in the repository is the signed release published by the project.

## Store listing text

Fastlane metadata lives in `fastlane/metadata/android/` for `en-GB` and `en-US`. When `versionCode` changes, add `changelogs/<versionCode>.txt` in both locales.

## Reports

Bug reports and patches can go through GitHub issues and pull requests. For a security issue, see [SECURITY.md](SECURITY.md).
