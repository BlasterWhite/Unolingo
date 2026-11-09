# Unolingo

Application Android de type Duolingo pour apprendre les caractères japonais par des quiz. Projet réalisé dans le cadre de l'ENSSAT.

## Fonctionnalités

| Fonctionnalité | État |
|----------------|------|
| **Apprendre** : fiches caractère pour les 104 hiragana, 104 katakana et 2 136 kanji, précédent / suivant (ou balayage). Une fiche kanji donne la traduction anglaise, le grade, les lectures on'yomi et kun'yomi en hiragana et en rōmaji, et un filtre par grade. Le 🔊 lit la lecture en hiragana, en alternant vitesse normale et lente (0,3) à chaque écoute | Disponible |
| **Réglages du quiz** : jeu de caractères, type de question, aide oui/non, synthèse vocale oui/non, grade des kanji | Disponible |
| **Quiz** : 10 questions, toujours 4 propositions. Types par jeu : caractère → lecture, lecture → caractère (kana et kanji), caractère → traduction, traduction → caractère (kanji). Aide facultative, synthèse vocale facultative (questions « son → caractère » et « caractère → son »), passage automatique à la question suivante après 2 s, pastilles ◯ / ✕ des 10 réponses | Disponible |
| **Grade du joueur** et **page de progression** : rang de 10 kyū à 5 dan sur l'accueil, graphique de l'évolution, historique, meilleur rang | Disponible |
| Historique des quiz en base locale (Room) | Disponible |
| **Duel** 1 contre 1 en écran partagé sur le même téléphone (moitié haute retournée pour le joueur d'en face), 10 manches à choix multiples | Disponible |
| **Crédits** : kanji-data, polices, icônes, avec les licences | Disponible |
| Duel entre deux téléphones (Bluetooth, Wi-Fi) | À venir |

### Grade des kanji : « ≤ g » et non « ≥ g »

L'énoncé du TP2 parle des kanji de grade « ≥ g ». L'équipe a choisi **« ≤ g »** : un quiz de grade `g` tire les kanji de grade 1 à `g`. Le grade choisi plafonne donc la difficulté : on ne tombe jamais sur un kanji plus difficile que prévu, et le grade 1 reste jouable (240 kanji pour les grades 1 et 2, contre plus de 2 000 avec « ≥ »). Les grades des données sont 1 à 6 et 8 ; les quiz de kana ont toujours le grade 1. Si la sélection ne donne aucun kanji, tout le jeu est utilisé. Le duel utilise les kanji de grade ≤ 2.

## Grade du joueur

Le grade résume le niveau actuel de l'apprenant, de **10 kyū** à **5 dan** (15 rangs).

1. Chaque quiz terminé rapporte des points : `points = score / total × difficulté × aide`
   - `difficulté` : 1 pour un quiz de kana ; 1 + g/2 pour un quiz de kanji de grade g (1,5 au grade 1, 4 au grade 6, 5 au grade 8) ;
   - `aide` : 0,8 si l'aide était activée, 1 sinon.
2. La **valeur** du joueur est la moyenne des points des **20 derniers quiz** (fenêtre glissante ; moins de 20 quiz : moyenne de ceux qui existent ; aucun quiz : 0).
3. Le **rang** découpe la valeur de 0 à 5 en 15 tranches égales : `niveau = min(14, ⌊valeur / 5 × 15⌋)`, niveau 0 = 10 kyū, niveau 9 = 1 kyū, niveau 10 = 1 dan, niveau 14 = 5 dan. Une barre de progression indique l'avancement vers le rang suivant.

Exemples : une série parfaite en kana vaut 1 point (7 kyū) ; une série parfaite en kanji de grade 2 vaut 2 points (4 kyū).

**Pourquoi cette formule.** Le grade mesure la régularité plutôt que la chance : c'est la moyenne des points des 20 derniers quiz, donc un seul bon score isolé ne fait pas monter de rang, et un seul mauvais ne fait pas tout perdre ; il faut réussir de façon répétée pour progresser. Chaque quiz rapporte son taux de réussite multiplié par une difficulté : 1 pour les kana, 1 + g/2 pour un kanji de grade g, si bien que les kanji difficiles rapportent davantage (un quiz kanji parfait de grade 6 vaut 4 fois un quiz kana parfait). Les kana seuls plafonnent à 7 kyū, ce qui pousse l'apprenant vers les kanji. L'aide reste utilisable mais coûte 20 % des points (× 0,8) : on ne peut pas tricher pour grimper, une série parfaite avec aide vaut moins qu'une série sans aide. Enfin, la fenêtre glissante fait sortir les vieux échecs au bout de 20 quiz : le grade reflète le niveau actuel et non l'historique complet, et l'on peut toujours se rattraper après une mauvaise passe. Le rang (15 niveaux, de 10 kyū à 5 dan) découpe la valeur de 0 à 5 en tranches égales, avec une barre de progression vers le rang suivant.

Le calcul est en Kotlin pur dans `domain/progress/` (`GradeCalculator`, constantes dans `GradeConfig`), testé en JVM.

## Base de données

Les résultats sont stockés localement avec **Room** (`data/results/`, base `UnolingoDatabase`, schéma versionné dans `app/schemas/`) :

- `quiz_result` : un quiz terminé (date, jeu de caractères, type de question, aide, synthèse vocale, grade, score, nombre de questions, `isFake`) ;
- `question_result` : une ligne par question (caractère demandé, réponse correcte ou non), supprimée avec son quiz.

Un quiz n'est enregistré qu'une fois terminé (un quiz abandonné ne l'est pas ; « Rejouer » crée un nouveau résultat). Le duel n'est pas enregistré.

Le marqueur **`isFake`** (faux par défaut) identifie les résultats factices insérés par les tests instrumentés. `deleteFakeResults()` ne supprime que les lignes `isFake = 1` : les vrais résultats ne sont jamais touchés et la base n'est jamais vidée.

## Stack technique

- Kotlin, Jetpack Compose et Material 3
- Navigation Compose (routes typées avec kotlinx.serialization)
- Room (via KSP) pour la persistance locale
- Android `TextToSpeech` (voix japonaise) pour l'écran Apprendre et les quiz avec synthèse vocale. Si la voix japonaise n'est pas installée, l'application propose de la télécharger.

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

### Sous Linux, sans Android Studio

`gradlew` n'est pas exécutable dans Git : le lancer avec `sh`. Il faut un JDK dans le `PATH` et `sdk.dir` dans `local.properties` (ou `ANDROID_HOME`). Sur une machine avec peu de mémoire, compiler Kotlin dans le processus Gradle :

```bash
sh gradlew assembleDebug -Dkotlin.compiler.execution.strategy=in-process
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n fr.enssat.unolingo.mguezenn_mphongsa/.MainActivity
sh gradlew --stop                    # libère la mémoire ensuite
```

`adb install` est plus fiable que `installDebug` sur certains téléphones (MIUI demande de confirmer l'installation USB).

### Tests

- `./gradlew testDebugUnitTest` : tests JVM (logique du quiz, grade, données, ViewModels) et captures d'écran Robolectric dans `app/build/screenshots/`.
- `./gradlew connectedDebugAndroidTest` : tests instrumentés sur un appareil, à lancer sur un téléphone déverrouillé. Ils vérifient le grade et le graphique de progression :
  - `StatsSeedTest` insère 25 résultats factices (`isFake = 1`), vérifie que le grade en base égale la valeur calculée à la main, que le rang s'affiche sur la carte de l'accueil et que le graphique est affiché avec la bonne description ;
  - `StatsCleanupTest` vérifie que le nettoyage ne supprime que les faux résultats (un résultat réel témoin reste intact).

  Chaque test nettoie ses faux résultats avant et après. `gradle.properties` active `android.injected.androidTest.leaveApksInstalledAfterRun` : sans cela, Android Gradle Plugin désinstalle l'application à la fin des tests et efface ses données. Sur un Xiaomi, MIUI demande de confirmer l'installation de l'APK de test à l'écran.

## Organisation du code

```
app/src/main/java/fr/enssat/unolingo/mguezenn_mphongsa/
├── domain/         logique métier en Kotlin pur
│   ├── model/      caractères, jeux de caractères, modes de quiz
│   ├── battle/     règles du duel
│   ├── kana/       conversions kana ↔ rōmaji
│   ├── speech/     alternance de vitesse (normal / lent), lectures à prononcer
│   ├── progress/   grade du joueur (fenêtre glissante, rangs kyū / dan)
│   └── quiz/       réglages, génération des questions, correction, déroulement d'une série
├── data/           chargement des caractères (res/raw), extras.json
│   └── results/    base Room des résultats de quiz
├── speech/         synthèse vocale
├── sound/          sons de bonne et de mauvaise réponse
└── ui/
    ├── navigation/ routes et NavHost
    ├── home/       écran d'accueil et carte de grade
    ├── learn/      écran Apprendre
    ├── quizsetup/  réglages du quiz
    ├── quiz/       écran de quiz
    ├── stats/      progression et graphique (Canvas)
    ├── credits/    crédits
    ├── battle/     mode duel
    ├── common/     composants partagés
    └── theme/      thème Material 3
```

## Données des caractères

Les caractères viennent des JSON fournis par le TP, dans `app/src/main/res/raw/` : `kana_data.json` (104 hiragana, 104 katakana) et `kanji_jouyou.json` (2 136 kanji avec traduction anglaise, grade, lectures on'yomi et kun'yomi). Ils sont lus avec `loadJsonFromRaw` et analysés en Kotlin (`data/`).

`app/src/main/assets/characters/extras.json` ajoute ce que le TP ne fournit pas : les caractères visuellement proches (pour les mauvaises propositions) et les orthographes alternatives acceptées. Il est généré par `tools/gen_characters.py` :

```bash
python tools/gen_characters.py app/src/main/assets/characters
```

## Crédits

- **Données des kanji** : [davidluzgouveia/kanji-data](https://github.com/davidluzgouveia/kanji-data) (licence MIT, copiée dans `app/src/main/assets/licenses/`), citée dans l'écran « À propos · Crédits » de l'application.
- Polices Lexend et Klee One (SIL Open Font License) et icônes Material Symbols (Apache 2.0).

## Sons et direction artistique

- Les sons de bonne et de mauvaise réponse (`app/src/main/res/raw/`) sont des sons originaux générés par `tools/gen_sounds.py` : `python tools/gen_sounds.py app/src/main/res/raw`.
- `docs/prompt-direction-artistique.md` contient le brief de direction artistique (inventaire des écrans et contraintes Material 3), à utiliser avec un outil de design ou à transmettre à un·e graphiste.
- La direction artistique retenue, « Hanamaru » (cahier d’écriture japonais : papier washi, encre sumi, corrections ◯ / ✕, sceaux hanko), est décrite par les maquettes de `docs/ui/`.
- Polices embarquées (licence SIL Open Font License, textes dans `app/src/main/assets/licenses/`) : Lexend pour l’interface, Klee One pour les caractères japonais, réduite aux caractères utilisés par `tools/subset_fonts.py`. L’icône de l’application est générée par `tools/gen_icon.py`.
- Captures des écrans principaux sans appareil : `./gradlew testDebugUnitTest --tests "fr.enssat.unolingo.mguezenn_mphongsa.ui.ScreenshotTest"`, images dans `app/build/screenshots/`.

## Équipe

Projet ENSSAT réalisé par :

- **mguezenn**
- **mphongsa**

Identifiant de l'application (`applicationId`, identique au `namespace` et au paquet Kotlin) : `fr.enssat.unolingo.mguezenn_mphongsa`, selon la règle du TP (`fr.enssat.<projet>.<login1>_<login2>`) qui permet d'installer tous les projets sur un même téléphone.
