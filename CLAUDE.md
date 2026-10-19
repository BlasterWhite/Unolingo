# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Unolingo is an ENSSAT school project: a small Duolingo-style Android app for learning Japanese characters through quizzes. The user picks a character set, answers a series of questions, and the app stores scores and statistics locally. A battle mode lets two players compete.

The repository must stay on Git with an up-to-date `README.md` at the root (project description, features, how to build/run, team). Update the README whenever a user-facing feature lands.

## Project state

Implemented: home screen (set + mode selection), character data for the three sets, and the full **display quiz mode** (10 questions, character → romaji typed, romaji → character with 4 choices, per-question feedback, result screen). Also implemented: the **audio quiz mode** (20 text-to-speech questions, multiple choice) and the **local battle mode** (1 vs 1 split screen on one device, 10 multiple-choice rounds). Not implemented yet: saving results to Room (no entities/database yet), statistics, Bluetooth/Wi-Fi battle. The sections "Functional scope" and "Implementation guidelines" below describe what is planned; when an architectural decision is actually implemented, move it into "Architecture decisions" and keep this section accurate.

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
| Audio (`QuizMode.AUDIO`) | 20 questions | sound → character, character → sound |

- "character → romaji": show a character, the user picks or types the reading.
- "romaji → character": show a reading, the user picks the matching character among choices.
- "sound → character": the app pronounces a reading with TTS (automatically, plus a replay button), the user picks the character among 4.
- "character → sound": show a character, the user plays 4 pronunciations ("Son 1…4"), selects one and validates.

The audio mode uses **text-to-speech only, no speech recognition** (the user's decision): no microphone, no `RECORD_AUDIO` permission.

Distractors in multiple-choice questions should come from the same set and be visually or phonetically close when possible.

### Persistence: scores, dates, stats

A local database stores every finished session: date/time, character set, mode, score, number of questions, and per-character results (correct/incorrect). This feeds a statistics screen (history, success rate per set, weakest characters).

### Battle mode

1 vs 1: both players get the same questions at the same time. A round is won by the first correct answer; a wrong answer locks that player out until the next round (the other can still answer); if both are wrong nobody scores. 10 rounds, multiple-choice only (romaji → character and character → romaji), since nobody can type on a shared screen. Transports, in order of priority:

1. Local (done): same device in split screen, phone lying flat between the players; the top half is rotated 180° for the player sitting across.
2. Bluetooth: two devices nearby.
3. Wi-Fi: two devices on the same network (or Wi-Fi Direct).

Keep the battle game logic independent from the transport so the same rules work for all three.

## Implementation guidelines

Defaults to follow unless the team decides otherwise (record any deviation in "Architecture decisions"):

- Architecture: MVVM with one `ViewModel` per screen exposing `StateFlow` UI state; Compose screens stay stateless where practical. Navigation with Navigation Compose.
- Packages under `bzh.enssat.unolingo`: `ui/` (screens + theme), `data/` (database, repositories, character data), `domain/` or `quiz/` (question generation, scoring, battle rules), `speech/` (text-to-speech wrapper), `sound/` (answer feedback sounds), `battle/` (transports).
- Quiz logic (question generation, answer checking, scoring) must be pure Kotlin with no Android dependencies so it can be covered by JVM unit tests in `app/src/test`.
- Database: Room (with KSP), declared in the version catalog.
- Character data: ship it as a bundled asset (e.g. JSON in `app/src/main/assets/`) or a prepopulated Room table; don't hard-code long character lists in Kotlin.
- Text-to-speech: Android `TextToSpeech` with `Locale.JAPANESE`. A Japanese voice may not be installed: check `setLanguage` result and show a clear message (and an action to install voice data) instead of failing silently. Release the engine in `onCleared`/`onDispose`.
- Bluetooth: on Android 12+ use `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` (and `BLUETOOTH_ADVERTISE` if needed); minSdk is 26, so also handle the legacy permissions and location requirement for older versions.
- All user-visible strings go in `res/values/strings.xml` (French UI); Japanese characters in data files, not in string resources.

## Architecture decisions

Add entries here as decisions are implemented (what was chosen, and why).

- **Navigation**: Navigation Compose with type-safe `@Serializable` routes in `ui/navigation/Routes.kt`; `UnolingoNavHost` owns the `NavController` and screens receive plain callbacks (`onStartQuiz`, `onBack`…), never the controller. Enums passed as route arguments (`CharacterSet`, `QuizMode`) are `@Serializable` and `@Keep`.
- **Domain model**: `domain/model/` holds pure Kotlin enums. `QuizMode` carries its question count and question types, so UI and quiz logic read them from there instead of hard-coding 10/20. UI labels for enums are mapped to string resources in `ui/common/Labels.kt`.
- **Screens**: each screen owns its `Scaffold` (and therefore its insets); `MainActivity` only sets the theme and the NavHost. `ui/common/PlaceholderScreen` is a temporary shell for unimplemented features; replace it as features land.
- **Icons**: no `material-icons` dependency; icons are vector drawables in `res/drawable` (Material Symbols paths) used via `painterResource`. There is no AppCompat, so don't reference `?attr/colorControlNormal` or other AppCompat attributes in resources.
- **Layers**: `domain/` is pure Kotlin (no Android imports): `domain/model` (`JapaneseCharacter`, enums) and `domain/quiz` (`QuestionGenerator`, `DistractorPicker`, `AnswerChecker`, immutable `QuizSession`). `data/` loads characters; `ui/<screen>/` holds the screen and its ViewModel. ViewModels stay thin and delegate rules to `domain/`.
- **Dependency injection**: manual. `UnolingoApplication` owns an `AppContainer` with app-wide singletons; ViewModels are built with `viewModelFactory { initializer { … } }` reading the container through `APPLICATION_KEY` (see `QuizViewModel.factory`). Route arguments are passed to the factory, not read from `SavedStateHandle`.
- **Character data**: `app/src/main/assets/characters/{hiragana,katakana,kanji}.json`, generated by `tools/gen_characters.py` (edit the script, then regenerate; katakana are derived from hiragana). Fields: `readings` = Hepburn readings that may be displayed (exactly one for kana, several for kanji), `alternatives` = other accepted spellings never displayed (`si`, `nn`, `di`…), `meaning` (French, kanji only), `similar` = visually confusable symbols. Kanji readings use wāpuro long vowels (`kou`, `juu`); answer normalization maps macrons to that form. `CharacterDataTest` validates the real files.
- **Quiz rules**: a multiple-choice question never offers a distractor that also accepts the prompt as an answer (じ/ぢ, 日/火 for "ka"). Distractors are ranked by similarity (`similar` list, shared vowel/consonant, same symbol length) with random tie-breaking.
- **Battle**: rules live in the immutable `domain/battle/BattleGame` (`submit(player, answer)`, `nextRound()`), which knows nothing about the transport; Bluetooth/Wi-Fi must reuse it and only relay answers. `BattleViewModel` drives the local game and auto-advances to the next round after a short reveal delay. The split screen rotates player TWO's panel with `Modifier.rotate(180f)` (Compose hit-testing follows the rotation), keeps the screen on and locks the orientation while a game is running.
- **Shared choice UI**: `ui/common/Choices.kt` (`ChoiceGrid`, `choiceState`, `QuestionPrompt`, `Question.choiceOptions`) is used by both the solo quiz and the battle. `choiceState` keeps the right answer hidden until the round is revealed, so a player locked out by a mistake cannot see it.
- **Audio**: `speech/JapaneseSpeaker` wraps `TextToSpeech` (status `READY` / `MISSING_VOICE` / `UNAVAILABLE`); `QuizViewModel` owns it in audio mode (created through `speakerFactory`, released in `onCleared`) and shows `QuizUiState.MissingVoice` with an install action when no Japanese voice exists, re-checking on resume. Readings are always spoken **as hiragana** produced by `domain/kana/KanaConverter` (built from the hiragana data, longest syllable first), never as a bare kanji whose reading the engine would pick itself; `CharacterDataTest` checks that every reading converts. The manifest declares a `<queries>` entry for `android.intent.action.TTS_SERVICE`, required for package visibility.
- **Feedback sounds**: `sound/AnswerSoundPlayer` (SoundPool, app-wide singleton in `AppContainer`) plays `res/raw/answer_correct.wav` / `answer_wrong.wav` when an answer is checked, from `QuizViewModel.submitAnswer` and `BattleViewModel.submitAnswer` (not from composables, so a rotation never replays it). The sounds are original, synthesized by `tools/gen_sounds.py`: never ship Duolingo's (or any third-party) sound files.
- **Art direction**: `docs/prompt-direction-artistique.md` is the brief for the visual identity (screen and state inventory, Material 3 token constraints). Keep its inventory in sync when screens change; when the art direction lands, apply it through `ui/theme/` tokens rather than per-screen colors.
- **Homophones**: for audio questions (`bySound = true` in `DistractorPicker`), a distractor never shares an accepted answer with the target in either direction, so を/お, ぢ/じ, づ/ず or kanji with a common reading are never offered together with the right answer.
- **Japanese text rendering**: display Japanese characters with `ui/common/JapaneseText`, which sets a `ja-JP` locale on the text style; otherwise a French-locale device may render kanji with Chinese glyph variants.
- **Room**: the `androidx.room` Gradle plugin exports schemas to `app/schemas/` (commit them). No `@Database` exists yet.
- **Library versions**: Kotlin stays at 2.2.10 for now, so kotlinx-serialization-json is pinned to 1.9.0 (newer versions target newer Kotlin). Bump Kotlin, the serialization plugin and the library together.

## Commands

Run from the repo root. On Windows use `gradlew.bat` (or `./gradlew` from Git Bash). There is no `java` on the PATH on the dev machine: from a shell, set `JAVA_HOME` to Android Studio's bundled JBR first (`export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` in Git Bash).

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew testDebugUnitTest             # JVM unit tests (app/src/test)
./gradlew connectedDebugAndroidTest     # instrumented tests (app/src/androidTest), needs a device/emulator
./gradlew lint                          # Android lint

# Screenshots of the main screens, written to app/build/screenshots/
./gradlew testDebugUnitTest --tests "bzh.enssat.unolingo.ui.ScreenshotTest"

# Single unit test class / method
./gradlew testDebugUnitTest --tests "bzh.enssat.unolingo.domain.model.QuizModeTest"
./gradlew testDebugUnitTest --tests "bzh.enssat.unolingo.domain.model.QuizModeTest.displayMode_hasTenVisualQuestions"

# Single instrumented test class
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=bzh.enssat.unolingo.ExampleInstrumentedTest
```

## Build setup

- Single module `:app`, package / applicationId `bzh.enssat.unolingo`.
- Very recent toolchain: AGP 9.4, Gradle 9.6, Kotlin 2.2, compileSdk/targetSdk 37, minSdk 26, Java 11 source/target.
- AGP 9 provides built-in Kotlin support, so there is no `org.jetbrains.kotlin.android` plugin (the app applies `kotlin.compose`, `kotlin.serialization`, `ksp` and `room`). Don't add the standalone Kotlin Android plugin.
- AGP 9 DSL is used (`compileSdk { version = release(37) }`, `optimization { enable = false }` in release instead of `isMinifyEnabled`). Follow that DSL style when editing `app/build.gradle.kts`.
- Dependencies and plugins are declared in the version catalog `gradle/libs.versions.toml` and referenced as `libs.*`; Compose libraries are versioned via the Compose BOM. Add new dependencies to the catalog rather than hard-coding coordinates.
- Configuration cache is enabled (`gradle.properties`); build logic must be configuration-cache compatible.
- `repositoriesMode` is `FAIL_ON_PROJECT_REPOS`: repositories belong in `settings.gradle.kts`, not module build files.

## UI

Jetpack Compose with Material 3 only (no XML layouts). `MainActivity` calls `enableEdgeToEdge()`, so screens must apply the `Scaffold` inner padding / window insets.

The visual identity is the **"Hanamaru" art direction**: mockups in `docs/ui/` (one PNG per screen and state, plus the concept, palette, typography and motion boards). Match them when changing a screen.

- **Theme** (`ui/theme/`): `UnolingoTheme` uses the brand color schemes of `Color.kt` (light and dark), **no dynamic color**, `UnolingoTypography` and `UnolingoShapes`. Color roles carry meaning: `primaryContainer` = right answer, `errorContainer` = wrong answer, `secondaryContainer` = selected, `primary` = player 1, `tertiary` (gold) = player 2 and celebration only. Never hard-code colors in screens.
- **Fonts** (`res/font/`, see `tools/subset_fonts.py`): Lexend for all UI and romaji text; **Klee One for every Japanese character**, never for Latin text. Klee One is subset to the characters of the data files, so **re-run `tools/subset_fonts.py` after adding characters** (otherwise they fall back to the system font). Display Japanese with `ui/common/JapaneseText` and a `KanaStyles` size (`Hero` 96 → `Compact` 28); in mixed French/Japanese text use `appendMixed` in a `buildAnnotatedString`.
- **Components** (`ui/common/`): `ChoiceButton` (raised face on a 4 dp lip that flattens when pressed, ◯/✕ marks, shake on a wrong answer) is used by quiz choices, battle choices and the home set cards; `PrimaryButton` / `SecondaryButton` (56 dp pills); `AnswerMark` (◯/✕: right/wrong never relies on color alone); `PlayerSeal` (round 一 for player 1, square 二 for player 2, "stamp" animation on a point); `Hanamaru` (reserved for a perfect series); `Modifier.seigaiha` / `Modifier.kanoko` patterns, **never behind a character to recognize, a choice grid or body text**.
- **Icons**: Material Symbols Rounded, weight 500, as vector drawables in `res/drawable` (`ic_*`; the filled variant marks an active state). The launcher icon is generated by `tools/gen_icon.py` (adaptive background, foreground and monochrome; the "う" is vectorized from Klee One).
- **Motion**: answer feedback ≤ 300 ms (◯ trace 200 ms, shake 240 ms, correction card 220 ms, next question 280 ms, battle stamp 260 ms); the hanamaru (700 ms) is the only longer animation.
- **Screenshot tests**: `ScreenshotTest` (Robolectric, `app/src/test/.../ui/`) renders the real screens and writes PNGs to `app/build/screenshots/` during `testDebugUnitTest`; review them after UI changes. Robolectric has no TTS engine, so audio questions can only be checked on a device.
