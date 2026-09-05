# EstateFinder — Real Estate Property Finder

Mini project for Mobile Computing, Practical 5 (Roll No 45).
A simple Android app to browse, search, filter and favourite demo property
listings, view them on Google Maps and fetch data from a REST API.

## Tech stack

Java + XML (no Compose/Kotlin) · Material 3 · MVVM · Room · Retrofit ·
Google Maps SDK · Fused Location Provider · Glide

## Configuration

| Setting | Value |
|---|---|
| Package | `com.example.estatefinder` |
| minSdk / targetSdk / compileSdk | 26 / 34 / 34 |
| Gradle / AGP | 8.9 / 8.7.3 (runs on Android Studio's bundled JDK 21) |

## Opening in Android Studio

1. File → Open → select this folder.
2. Let the Gradle sync finish; accept any prompt to install missing SDK
   components (Android SDK Platform 34 is the compile target).
3. Create an emulator: Device Manager → Create Device → e.g. Pixel 7 with a
   system image (API 34, "Google APIs", x86_64).

## Google Maps API key (needed from Phase 5)

The key is stored in `local.properties` (git-ignored, machine-local). Add:

```
MAPS_API_KEY=your_key_here
```

To get a free key: Google Cloud Console → create/select a project → enable
**Maps SDK for Android** → Credentials → Create API key → restrict it to this
app's package name and SHA-1 fingerprint. Never commit the key to Git.

## Build & run

```
gradlew.bat assembleDebug      # build APK
gradlew.bat installDebug       # install on running emulator/device
```

## Package structure

```
com.example.estatefinder
├── data/local      Room entities, DAOs, AppDatabase
├── data/remote     Retrofit API interface + client
├── data/repository PropertyRepository
├── model           Property model
├── viewmodel       PropertyViewModel (MVVM)
└── ui              splash / home / property / details / favorites / map
```
