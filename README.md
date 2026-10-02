# MuScriptor Android Offline

Native Android application for offline audio-to-score transcription.

## Goal
Import an audio file on Android, run transcription locally on the device, and export MIDI / MusicXML without requiring an online service.

## Current status
Version 0.1 foundation:
- Native Kotlin + Jetpack Compose UI
- Android document/audio picker
- Offline-first architecture
- TranscriptionEngine abstraction
- GitHub Actions APK build

## Planned engine
The inference backend will be integrated through a native C++/JNI layer, targeting arm64 Android devices. Model files are kept separate from the repository because model licensing and APK size must be handled independently.

## Build
Open in Android Studio, or run the GitHub Actions workflow **Android APK**.

This project is not affiliated with Kyutai.
