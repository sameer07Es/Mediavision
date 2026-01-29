# Build Instructions

## Prerequisites

This is an Android application that requires the Android SDK to build. You cannot build Android apps in this environment without the Android SDK installed.

### Requirements for Building:
1. **Android Studio** (Recommended) - Download from https://developer.android.com/studio
2. **Android SDK** with:
   - Android SDK Platform 34
   - Android Build Tools 34.0.0 or higher
   - Android SDK Platform-Tools
3. **JDK 8 or higher**
4. **Gradle 8.2** (included via wrapper)

## Building the App

### Option 1: Using Android Studio (Recommended)
1. Install Android Studio
2. Open Android Studio
3. Click "Open an existing project"
4. Navigate to the Mediavision directory
5. Wait for Gradle sync to complete
6. Click "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"

### Option 2: Using Command Line
1. Install Android Studio to get the Android SDK
2. Set up environment variables:
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```
3. Run Gradle build:
   ```bash
   ./gradlew assembleDebug
   ```

### Output Location
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Installing on Device

### Via Android Studio:
1. Connect your Android device via USB
2. Enable USB debugging on your device
3. Click "Run" → "Run 'app'"

### Via ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### SDK Not Found Error
If you see errors about the Android SDK not being found:
1. Install Android Studio
2. Open Android Studio and download the required SDK components
3. Set the `ANDROID_HOME` environment variable to your SDK location
4. Create a `local.properties` file in the project root with:
   ```
   sdk.dir=/path/to/your/Android/Sdk
   ```

### Gradle Sync Failed
1. Check your internet connection
2. Try "File" → "Invalidate Caches / Restart" in Android Studio
3. Delete the `.gradle` directory and sync again

### Build Failed
1. Make sure you have installed all required SDK components
2. Check that you're using JDK 8 or higher
3. Try cleaning the project: `./gradlew clean`
