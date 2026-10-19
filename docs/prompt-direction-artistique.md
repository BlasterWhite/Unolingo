# Prompt : direction artistique d'Unolingo

À copier tel quel dans un outil de génération de design (Claude, ChatGPT, Figma Make…) ou à transmettre à un·e graphiste. Il décrit l'application, ses contraintes techniques et l'inventaire complet des écrans et états à habiller.

---

```text
Tu es directeur·rice artistique et designer UI spécialisé·e dans les applications mobiles éducatives et ludiques. Conçois la direction artistique complète d'une application Android, puis applique-la à tous ses écrans.

## Le produit

Unolingo est une application Android pour apprendre les caractères japonais par des quiz courts, dans l'esprit de Duolingo (sessions rapides, retour immédiat, ton encourageant), mais avec sa propre identité. C'est un projet étudiant de l'ENSSAT (école d'ingénieurs à Lannion, Bretagne).

- Public : francophones débutants à intermédiaires, surtout étudiant·es de 18 à 25 ans, qui apprennent les hiragana, les katakana puis les kanji.
- Langue de l'interface : français. Les caractères japonais sont le contenu principal.
- Trois jeux de caractères : Hiragana (104), Katakana (104), Kanji (49, avec leur sens en français).
- Trois façons de jouer :
  1. Mode affichage : 10 questions, lire un caractère (saisie en rōmaji) ou retrouver un caractère à partir de sa lecture (4 choix).
  2. Mode audio : 20 questions avec synthèse vocale japonaise, reconnaître un caractère entendu ou choisir la bonne prononciation parmi 4 sons.
  3. Mode duel : 1 contre 1 sur le même téléphone posé à plat entre deux joueurs face à face, écran partagé en deux moitiés (celle du haut est retournée à 180°), 10 manches, le premier qui répond juste marque le point.
- Des sons de bonne et de mauvaise réponse existent déjà (« ding-ding » cristallin montant, et deux notes graves et douces descendantes). L'identité visuelle doit être cohérente avec ce registre : vif, positif, jamais punitif.

## Personnalité attendue

Joyeuse, claire et encourageante, avec une inspiration japonaise subtile et moderne : papier washi, encre sumi, formes de tampons hanko, ou tout autre motif pertinent. Éviter les clichés (sushis, geishas, sabres, typographies « fausses japonaises » en pinceau pour le texte français) et éviter de copier Duolingo (pas de chouette verte, pas de vert #58CC02 comme couleur principale). Une mascotte est possible mais optionnelle : propose-la seulement si elle sert l'expérience (encouragements, écrans de résultat).

## Contraintes techniques (à respecter impérativement)

- Application native Jetpack Compose avec Material 3. La DA doit se traduire en tokens Material 3 : un color scheme complet (primary, onPrimary, primaryContainer, onPrimaryContainer, secondary…, tertiary…, error…, surface, surfaceVariant, onSurfaceVariant, outline, background) en thème clair ET sombre, une échelle typographique Material 3 (display, headline, title, body, label) et des formes (small, medium, large).
- La couleur dynamique d'Android 12+ (Material You) sera désactivée pour garder l'identité de marque : la palette doit donc fonctionner seule.
- Rôles de couleur déjà utilisés par l'interface et à garder lisibles entre eux :
  - choix neutre : surface ;
  - choix sélectionné avant validation : secondaryContainer ;
  - bonne réponse : primaryContainer ;
  - mauvaise réponse : errorContainer ;
  - choix éteint après correction : onSurface à 38 % d'opacité ;
  - joueur 1 en duel : primary ; joueur 2 : tertiary (les deux doivent se distinguer immédiatement, y compris pour une personne daltonienne).
- Contraste WCAG AA minimum (4,5:1 pour le texte, 3:1 pour les grands caractères et les composants).
- Polices disponibles sur Google Fonts uniquement. Il faut une police pour l'interface en français et une police japonaise qui affiche correctement les kana et les kanji en très grande taille (jusqu'à 96 sp), par exemple de la famille Noto Sans JP / Noto Serif JP, M PLUS, Zen Maru Gothic… Justifie le choix, et précise si la police japonaise doit aussi servir aux rōmaji.
- Téléphones Android 8+ en portrait, affichage bord à bord (sous la barre d'état), zones tactiles d'au moins 48 dp, grille de 4 dp, marges d'écran de 16 dp.
- Icônes : style Material Symbols (déjà utilisées : flèche retour, haut-parleur). Indique le style retenu (outlined, rounded, sharp ; graisse ; remplissage).

## Inventaire des écrans et états à habiller

1. Accueil
   - Barre du haut avec le nom « Unolingo ».
   - « Jeu de caractères » : sélecteur segmenté à 3 options (Hiragana, Katakana, Kanji).
   - « Mode de quiz » : sélecteur segmenté à 2 options (Affichage, Audio), avec le texte « 10 questions par série » ou « 20 questions par série ».
   - Bouton principal « Commencer » ; boutons secondaires « Statistiques » et « Mode duel ».
   - L'accueil est aujourd'hui très austère : propose une version plus engageante (en-tête illustré, cartes pour les jeux de caractères, mise en avant du duel…) sans ajouter de fonctionnalités.

2. Quiz : éléments communs
   - Barre du haut : flèche retour et nom du jeu (« Hiragana »).
   - Ligne « Question 3 / 10 » à gauche et « Score : 2 » à droite, puis une barre de progression linéaire.
   - Consigne en titre (« Quelle est la lecture de ce caractère ? », « Quel caractère se lit ainsi ? », « Quel caractère entendez-vous ? », « Quelle est la bonne prononciation ? »).
   - Pour les kanji, une ligne secondaire « Sens : jour, soleil ».
   - Après la réponse, une carte de correction pleine largeur : « Bonne réponse ! » (couleurs primaryContainer) ou « Raté ! » (errorContainer), avec le détail « 日 se lit : nichi / jitsu / hi / ka ». Puis un bouton « Question suivante » ou « Voir le résultat ».

3. Question « caractère → rōmaji » (mode affichage)
   - Caractère géant (96 sp), champ de texte « Lecture en rōmaji » (clavier ouvert), bouton « Valider ».
   - États : vide, saisi, corrigé (champ désactivé + carte de correction).

4. Question « rōmaji → caractère » (mode affichage)
   - Lecture affichée en très grand (style display), grille 2×2 de boutons contenant chacun un caractère japonais (40 sp, hauteur 88 dp).
   - États de chaque bouton : neutre, bonne réponse, mauvaise réponse choisie, éteint.

5. Question « son → caractère » (mode audio)
   - Grand bouton haut-parleur rond (96 dp) « Réécouter » (le son est joué automatiquement à l'affichage), grille 2×2 de caractères comme au point 4.
   - Prévoir un retour visuel pendant la lecture du son (animation légère).

6. Question « caractère → son » (mode audio)
   - Caractère géant, grille 2×2 de boutons « Son 1 » à « Son 4 » avec icône haut-parleur (hauteur 72 dp). Un appui joue le son et sélectionne le bouton (état sélectionné), puis « Valider ».
   - Après correction, chaque bouton affiche sa lecture (« Son 2 · ka ») et garde la couleur juste ou fausse.

7. Écrans d'erreur audio
   - « Voix japonaise manquante » : message, bouton « Installer la voix japonaise » et bouton « Réessayer ».
   - « Synthèse vocale indisponible » : message et bouton « Réessayer ».

8. Résultat d'une série
   - « Série terminée », score géant « 7 / 10 ».
   - Soit « Sans faute, bravo ! » (moment de célébration à concevoir), soit une liste « À revoir » de cartes : caractère (36 sp), lectures attendues, « Réponse donnée : … ».
   - Boutons « Rejouer » et « Retour à l'accueil ».

9. Chargement et erreur de chargement (indicateur circulaire ; message « Impossible de charger les caractères. »).

10. Duel : préparation
    - Barre du haut « Mode duel », texte des règles, « 10 manches », sélecteur de jeu de caractères, bouton « Commencer le duel ».

11. Duel : partie en écran partagé (l'écran le plus original, à soigner particulièrement)
    - Deux moitiés identiques : celle du joueur 2 en haut, tournée à 180°, et celle du joueur 1 en bas.
    - Dans chaque moitié : nom du joueur dans sa couleur (« Joueur 1 », « Joueur 2 ») et score vu de son côté (« 3 – 2 »), énoncé (caractère à 56 sp ou rōmaji), grille 2×2 compacte (boutons de 56 dp, caractères à 28 sp), ligne d'état (« Point pour toi ! », « Point pour ton adversaire », « Raté ! Attends ton adversaire… », « Personne ne marque »).
    - Une barre centrale séparant les moitiés, avec « Manche 3 / 10 » affiché deux fois (une fois à l'envers pour le joueur d'en face).
    - Fin de partie : « Victoire ! », « Défaite » ou « Égalité » avec le score dans chaque moitié, et boutons « Revanche » et « Quitter » dans la barre centrale.
    - Boîte de dialogue « Quitter le duel ? » (« La partie en cours sera perdue. », « Continuer » / « Quitter »).
    - Chaque joueur doit savoir instantanément quelle moitié est la sienne : couleur, motif, éventuellement un avatar.

12. Statistiques : aujourd'hui un simple « Bientôt disponible ». Propose une maquette future : historique des séries, taux de réussite par jeu de caractères, caractères les plus ratés.

13. Icône de l'application : icône adaptative Android (premier plan, arrière-plan, version monochrome pour les icônes thématiques).

## Livrables attendus

1. Le concept en quelques lignes (nom de la DA, intention, 3 mots-clés), avec une planche d'ambiance décrite.
2. La palette : valeurs hexadécimales pour chaque rôle Material 3 en clair et en sombre, avec les ratios de contraste des paires texte/fond clés, plus les couleurs des joueurs 1 et 2.
3. La typographie : polices, graisses, et pour chaque style Material 3 utilisé la taille, l'interligne et l'espacement, avec un style dédié aux caractères japonais géants.
4. Les formes et l'élévation : rayons des coins (boutons, cartes, boutons de choix, sélecteurs), ombres ou contours.
5. L'iconographie et l'illustration : style d'icônes, mascotte éventuelle, motifs décoratifs et où les utiliser (et où ne pas les utiliser).
6. Le mouvement : animations clés (bonne réponse, mauvaise réponse, passage à la question suivante, point marqué en duel, victoire), avec durées et courbes, en restant sobre (moins de 300 ms pour les retours de réponse).
7. Les maquettes de chaque écran et état listés ci-dessus, en clair et en sombre pour au moins l'accueil, une question et le duel.
8. Une liste de choses à faire et à ne pas faire pour garder la cohérence.
9. Les tokens prêts à intégrer : un bloc Kotlin pour Jetpack Compose (`lightColorScheme(...)`, `darkColorScheme(...)`, `Typography(...)`, `Shapes(...)`) et la liste des polices Google Fonts à ajouter.

Présente d'abord le concept et la palette, puis le reste dans l'ordre des livrables. Quand une contrainte ci-dessus entre en conflit avec une idée esthétique, la contrainte l'emporte : explique le compromis choisi.
```
