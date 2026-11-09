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
- Packages under `fr.enssat.unolingo.mguezenn_mphongsa`: `ui/` (screens + theme), `data/` (database, repositories, character data), `domain/` or `quiz/` (question generation, scoring, battle rules), `speech/` (text-to-speech wrapper), `sound/` (answer feedback sounds), `battle/` (transports).
- Quiz logic (question generation, answer checking, scoring) must be pure Kotlin with no Android dependencies so it can be covered by JVM unit tests in `app/src/test`.
- Database: Room (with KSP), declared in the version catalog.
- Character data: ship it as a bundled asset (e.g. JSON in `app/src/main/assets/`) or a prepopulated Room table; don't hard-code long character lists in Kotlin.
- Text-to-speech: Android `TextToSpeech` with `Locale.JAPANESE`. A Japanese voice may not be installed: check `setLanguage` result and show a clear message (and an action to install voice data) instead of failing silently. Release the engine in `onCleared`/`onDispose`.
- Bluetooth: on Android 12+ use `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` (and `BLUETOOTH_ADVERTISE` if needed); minSdk is 26, so also handle the legacy permissions and location requirement for older versions.
- All user-visible strings go in `res/values/strings.xml` (French UI); Japanese characters in data files, not in string resources.

## Architecture decisions

Add entries here as decisions are implemented (what was chosen, and why).

- **Package name**: the TP imposes `fr.enssat.<project>.<login1>_<login2>`, so the namespace and applicationId are `fr.enssat.unolingo.mguezenn_mphongsa` (lets the teacher install every project on one phone).
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
./gradlew testDebugUnitTest --tests "fr.enssat.unolingo.mguezenn_mphongsa.ui.ScreenshotTest"

# Single unit test class / method
./gradlew testDebugUnitTest --tests "fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizModeTest"
./gradlew testDebugUnitTest --tests "fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizModeTest.displayMode_hasTenVisualQuestions"

# Single instrumented test class
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=fr.enssat.unolingo.mguezenn_mphongsa.ExampleInstrumentedTest
```

## Build setup

- Single module `:app`, package / applicationId `fr.enssat.unolingo.mguezenn_mphongsa`.
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

<!-- HANGAR:START - generated, do not edit by hand -->

## Shared task board (Hangar.AI)

This project is driven from Hangar.AI. Several agents work **in parallel** in separate
terminals on this same directory. A shared kanban board is the source of truth for
who is doing what. Reach it through the MCP tools prefixed `board_`.

### Hard rule

**Never start work on a task without claiming it first.** Another agent may already
be on it. `board_claim_task` fails when the task is taken — when that happens, do not
retry it, pick a different one.

### Expected loop

1. **On startup**, call `board_list_tasks` to see the current state of the work.
2. **To pick work**, call `board_next_task`. It returns the highest-priority free task
   whose dependencies are all done. Do not pick arbitrarily from the todo column, or
   you may start work that is still blocked.
3. **Claim it** with `board_claim_task`. It moves to `doing` automatically.
   - If the call fails, go back to step 2.
4. **Do the work.**
5. **Report** with `board_comment_task`: decisions made, files touched, problems hit.
   This is the only channel other agents have to learn what you did.
6. **Finish** with `board_update_task`, setting `column` to `review` if the work needs
   a second pair of eyes, or `done` otherwise.
   - If you give up, call `board_update_task` with `release: true` so the task returns
     to the pool instead of sitting blocked under your name.

### Splitting work

When you find work beyond your current task, do not silently pile it into the same
change: create tasks with `board_create_task` so other agents can pick them up.

- Use `depends_on` with the ids of prerequisite tasks. `board_next_task` relies on it
  to never hand out work that cannot start yet.
- Use `priority` (integer, higher is more urgent) for work that unblocks others.

### Columns

| Column   | Meaning |
|----------|---------|
| `todo`   | ready to be picked up |
| `doing`  | someone is working on it |
| `review` | finished, waiting for a review |
| `done`   | accepted - unblocks tasks that depend on it |

### Staying out of each other's way

Before editing a central file, check with `board_list_tasks` that no other agent holds
a `doing` task covering the same area. If one does, leave a comment on their task
rather than editing the same files at the same time.

### Talking to the agent next door

Claude Code sessions in Hangar.AI are named after their pane and its workspace, for
example `Ava (Shop)`. A pane's first name is unique across every workspace, so `Ava` is
one agent and the workspace in brackets tells you which project it works on. When another
agent needs to know something now (you changed a file it is working on, you answered its
question), message it directly with `SendMessage` to that name; `/list-agents` shows who
is reachable. The board stays the record: write the same thing there if it matters beyond
this moment.

### Showing a 3D model

To show the user a 3D model, open it with `model_show` rather than giving them a path, and
check your own work with `model_snapshot`, which returns a picture of it.

### Showing a file, or sending it to the phone

To show the user a render, screenshot, recording, report, Markdown, PDF or HTML page, open
it with `preview_show {path, title?}` rather than giving them a path: it appears in the
workspace's "Aperçu" panel. An HTML page runs isolated, so inline its scripts and styles
(its own `.js` and `.css` files do not load). Call `preview_close` once you are done, so it
does not keep a part of their screen.

To send a file to the user's phone (Hangar Mobile), use `phone_share {path, caption?}`, when
they ask for it or a result is clearly worth it, never for routine output.

### Opening a kit's tab

A kit enabled on this workspace can bring small tabs that take params, such as one showing a
CI run for its id. `kit_tab_list` says which there are, `kit_tab_open {kit, tab, params?, reveal?}`
opens one in the same panel without taking the keyboard from anyone, and
`kit_tab_close {kit, tab, params?}` closes it.

## Global memory

You also have a **persistent global memory**, reached through the MCP tools prefixed
`memory_`. It is shared by every workspace, every project and every agent, and it
survives across sessions: what you write there, the next agent reads. The origin
(workspace, agent) is recorded for you.

### Read before you dig

**At the start of a task, call `memory_search`** with the project name and the subject you
are about to touch. Another agent may already have paid the price of finding this out —
do not re-discover what is already written down. `memory_list` gives a compact index,
`memory_read` pulls one entry in full.

### Write the moment you learn

Call `memory_write` as soon as a durable, reusable fact appears, not at the end of the
task. Worth keeping:

- architecture decisions **and the reason behind them**
- environment or build traps: the flag that is required, the command that hangs, the
  version that breaks things
- project conventions that are not obvious from a quick read
- preferences the user stated explicitly

Do not write: task progress, which belongs in `board_comment_task`; anything readable
from the code or the git history; ephemeral details of the current conversation.

### How to write an entry

- **Title** short, stable, searchable. `memory_write` upserts on the title
  (case-insensitive), so reuse the same title to update a fact instead of stacking a
  near-duplicate next to it.
- **Content** brief and factual — actionable by someone arriving with no context.
- **Tags** must include the project name. The memory is global, so an entry that does not
  carry its own project context is noise everywhere else.

### Hygiene

When a fact turns out to be false, correct it with `memory_update` or drop it with
`memory_delete`. Never leave two entries contradicting each other: the next agent has no
way to tell which one is still true.

<!-- HANGAR:END -->
