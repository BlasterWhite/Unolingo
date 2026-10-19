# Unolingo

Application Android de type Duolingo pour apprendre les caractères japonais par des quiz. Projet réalisé dans le cadre de l'ENSSAT.

## Fonctionnalités

| Fonctionnalité | État |
|----------------|------|
| Choix du jeu de caractères (104 hiragana, 104 katakana, 49 kanji) et du mode de quiz | Disponible |
| Mode affichage : 10 questions (caractère → romaji à saisir, romaji → caractère parmi 4 choix), correction immédiate et écran de résultat | Disponible |
| Mode audio : 20 questions en synthèse vocale japonaise (son → caractère parmi 4, caractère → choisir la bonne prononciation parmi 4) | Disponible |
| Historique des scores et statistiques (base de données locale) | À venir |
| Mode duel 1 contre 1 en écran partagé sur le même téléphone (moitié haute retournée pour le joueur d'en face), 10 manches à choix multiples | Disponible |
| Mode duel entre deux téléphones (Bluetooth, Wi-Fi) | À venir |

## Stack technique

- Kotlin, Jetpack Compose et Material 3
- Navigation Compose (routes typées avec kotlinx.serialization)
- Room (via KSP) pour la persistance locale
- Android `TextToSpeech` (voix japonaise) pour le mode audio. Si la voix japonaise n'est pas installée, l'application propose de la télécharger.

Configuration : minSdk 26, targetSdk/compileSdk 37, Android Gradle Plugin 9.4, Gradle 9.6, Kotlin 2.2.

## Compiler et lancer

Prérequis : Android Studio récent (ou un JDK 17+ et le SDK Android, avec `sdk.dir` renseigné dans `local.properties`).

```bash
./gradlew assembleDebug              # construit l'APK de debug
./gradlew installDebug               # installe l'application sur un appareil ou un émulateur
./gradlew testDebugUnitTest          # tests unitaires
./gradlew connectedDebugAndroidTest  # tests instrumentés (appareil ou émulateur requis)
./gradlew lintDebug                  # analyse lint
```

Sous Windows, utiliser `gradlew.bat` à la place de `./gradlew`.

## Organisation du code

```
app/src/main/java/bzh/enssat/unolingo/
├── domain/         logique métier en Kotlin pur
│   ├── model/      caractères, jeux de caractères, modes de quiz
│   ├── battle/     règles du duel
│   ├── kana/       conversion rōmaji → hiragana (texte envoyé à la synthèse vocale)
│   └── quiz/       génération des questions, correction, déroulement d'une série
├── data/           chargement des caractères depuis les assets
├── speech/         synthèse vocale
├── sound/          sons de bonne et de mauvaise réponse
└── ui/
    ├── navigation/ routes et NavHost
    ├── home/       écran d'accueil
    ├── quiz/       écran de quiz
    ├── stats/      statistiques
    ├── battle/     mode duel
    ├── common/     composants partagés
    └── theme/      thème Material 3
```

## Données des caractères

Les caractères sont dans `app/src/main/assets/characters/` (un fichier JSON par jeu). Ils sont générés par `tools/gen_characters.py` : pour ajouter ou corriger un caractère, modifier le script puis le relancer.

```bash
python tools/gen_characters.py app/src/main/assets/characters
```

## Sons et direction artistique

- Les sons de bonne et de mauvaise réponse (`app/src/main/res/raw/`) sont des sons originaux générés par `tools/gen_sounds.py` : `python tools/gen_sounds.py app/src/main/res/raw`.
- `docs/prompt-direction-artistique.md` contient le brief de direction artistique (inventaire des écrans et contraintes Material 3), à utiliser avec un outil de design ou à transmettre à un·e graphiste.
- La direction artistique retenue, « Hanamaru » (cahier d’écriture japonais : papier washi, encre sumi, corrections ◯ / ✕, sceaux hanko), est décrite par les maquettes de `docs/ui/`.
- Polices embarquées (licence SIL Open Font License, textes dans `app/src/main/assets/licenses/`) : Lexend pour l’interface, Klee One pour les caractères japonais, réduite aux caractères utilisés par `tools/subset_fonts.py`. L’icône de l’application est générée par `tools/gen_icon.py`.
- Captures des écrans principaux sans appareil : `./gradlew testDebugUnitTest --tests "bzh.enssat.unolingo.ui.ScreenshotTest"`, images dans `app/build/screenshots/`.

## Équipe

À compléter (noms des membres de l'équipe).
