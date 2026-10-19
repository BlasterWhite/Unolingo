# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Unolingo is an ENSSAT school project: a small Duolingo-style Android app for learning Japanese characters through quizzes. The user picks a character set, answers a series of questions, and the app stores scores and statistics locally. A battle mode lets two players compete.

The repository must stay on Git with an up-to-date `README.md` at the root (project description, features, how to build/run, team). Update the README whenever a user-facing feature lands.

## Project state

The project is still the Android Studio "Empty Compose Activity" template: `MainActivity` renders a placeholder `Greeting`, and the tests are the template's `ExampleUnitTest` / `ExampleInstrumentedTest`. No architecture exists yet (no navigation, ViewModels, DI, persistence or networking). The sections "Functional scope" and "Implementation guidelines" below describe what is planned; when an architectural decision is actually implemented, move it into "Architecture decisions" and keep this section accurate.

## Functional scope

### Character sets

Three sets, in two families:

- Phonetic (syllabaries): **Hiragana** and **Katakana**, including dakuten/handakuten variants (が, ぱ…) and combined yōon (きゃ, しょ…) once the basic set works.
- Ideographic: **Kanji**. A kanji can have several readings (on'yomi / kun'yomi) and a meaning, so its data model differs from kana: one character maps to a list of readings, and an answer is correct if it matches any accepted reading.

Romanization uses Hepburn (し = "shi", つ = "tsu", ふ = "fu"). Answer comparison should be case-insensitive and trim whitespace.

### Quiz modes

| Mode | Length | Question types |
|------|--------|----------------|
| Display (visual) | 10 questions | character → romaji, romaji → character |
| Text-to-speech (audio) | 20 questions | sound → character, character → speak (the user pronounces it) |

- "character → romaji": show a character, the user picks or types the reading.
- "romaji → character": show a reading, the user picks the matching character among choices.
- "sound → character": the app pronounces a character with TTS, the user picks the character.
- "character → speak": show a character, the user says it aloud; speech recognition checks the answer.

Distractors in multiple-choice questions should come from the same set and be visually or phonetically close when possible.

### Persistence: scores, dates, stats

A local database stores every finished session: date/time, character set, mode, score, number of questions, and per-character results (correct/incorrect). This feeds a statistics screen (history, success rate per set, weakest characters).

### Battle mode

Two players answer the same series of questions and compare scores. Planned transports, in order of priority:

1. Local: same device, players take turns.
2. Bluetooth: two devices nearby.
3. Wi-Fi: two devices on the same network (or Wi-Fi Direct).

Keep the battle game logic independent from the transport so the same rules work for all three.

## Implementation guidelines

Defaults to follow unless the team decides otherwise (record any deviation in "Architecture decisions"):

- Architecture: MVVM with one `ViewModel` per screen exposing `StateFlow` UI state; Compose screens stay stateless where practical. Navigation with Navigation Compose.
- Packages under `bzh.enssat.unolingo`: `ui/` (screens + theme), `data/` (database, repositories, character data), `domain/` or `quiz/` (question generation, scoring, battle rules), `speech/` (TTS and speech recognition wrappers), `battle/` (transports).
- Quiz logic (question generation, answer checking, scoring) must be pure Kotlin with no Android dependencies so it can be covered by JVM unit tests in `app/src/test`.
- Database: Room (with KSP), declared in the version catalog.
- Character data: ship it as a bundled asset (e.g. JSON in `app/src/main/assets/`) or a prepopulated Room table; don't hard-code long character lists in Kotlin.
- Text-to-speech: Android `TextToSpeech` with `Locale.JAPANESE`. A Japanese voice may not be installed: check `setLanguage` result and show a clear message (and an action to install voice data) instead of failing silently. Release the engine in `onCleared`/`onDispose`.
- Speech recognition: `SpeechRecognizer` with language `ja-JP`, which needs the `RECORD_AUDIO` runtime permission and may require network access depending on the device. Match the recognized text against the expected kana/kanji and its readings, not only exact strings.
- Bluetooth: on Android 12+ use `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` (and `BLUETOOTH_ADVERTISE` if needed); minSdk is 26, so also handle the legacy permissions and location requirement for older versions.
- All user-visible strings go in `res/values/strings.xml` (French UI); Japanese characters in data files, not in string resources.

## Architecture decisions

None yet. Add entries here as decisions are implemented (what was chosen, and why).

## Commands

Run from the repo root. On Windows use `gradlew.bat` (or `./gradlew` from Git Bash).

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew testDebugUnitTest             # JVM unit tests (app/src/test)
./gradlew connectedDebugAndroidTest     # instrumented tests (app/src/androidTest), needs a device/emulator
./gradlew lint                          # Android lint

# Single unit test class / method
./gradlew testDebugUnitTest --tests "bzh.enssat.unolingo.ExampleUnitTest"
./gradlew testDebugUnitTest --tests "bzh.enssat.unolingo.ExampleUnitTest.addition_isCorrect"

# Single instrumented test class
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=bzh.enssat.unolingo.ExampleInstrumentedTest
```

## Build setup

- Single module `:app`, package / applicationId `bzh.enssat.unolingo`.
- Very recent toolchain: AGP 9.4, Gradle 9.6, Kotlin 2.2, compileSdk/targetSdk 37, minSdk 26, Java 11 source/target.
- AGP 9 provides built-in Kotlin support, so there is no `org.jetbrains.kotlin.android` plugin — only `kotlin.compose`. Don't add the standalone Kotlin Android plugin.
- AGP 9 DSL is used (`compileSdk { version = release(37) }`, `optimization { enable = false }` in release instead of `isMinifyEnabled`). Follow that DSL style when editing `app/build.gradle.kts`.
- Dependencies and plugins are declared in the version catalog `gradle/libs.versions.toml` and referenced as `libs.*`; Compose libraries are versioned via the Compose BOM. Add new dependencies to the catalog rather than hard-coding coordinates.
- Configuration cache is enabled (`gradle.properties`); build logic must be configuration-cache compatible.
- `repositoriesMode` is `FAIL_ON_PROJECT_REPOS`: repositories belong in `settings.gradle.kts`, not module build files.

## UI

Jetpack Compose with Material 3 only (no XML layouts). The app theme is `UnolingoTheme` in `ui/theme/` (`Theme.kt`, `Color.kt`, `Type.kt`), which uses dynamic color on Android 12+. `MainActivity` calls `enableEdgeToEdge()`, so screens must apply the `Scaffold` inner padding / window insets. Make sure the chosen font renders kana and kanji correctly at large sizes (quiz characters are displayed big).
