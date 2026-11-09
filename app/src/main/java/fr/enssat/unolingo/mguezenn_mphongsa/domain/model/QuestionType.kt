package fr.enssat.unolingo.mguezenn_mphongsa.domain.model

/**
 * What a question asks and answers with. For kana a "reading" is the romaji; for kanji it is the hiragana reading
 * (romaji only appears as help), except in the audio types where the reading is pronounced.
 */
enum class QuestionType {
    /**
     * Typed input. Gone from the quiz (every question is multiple choice, TP2 decision); the constant is only kept
     * because `BattleGameTest` still refers to it. The generator turns it into [CHARACTER_TO_ROMAJI_CHOICE].
     */
    CHARACTER_TO_ROMAJI,

    /** Show a character, the user picks its reading among 4 choices (romaji for kana, hiragana for kanji). */
    CHARACTER_TO_ROMAJI_CHOICE,

    /** Show a reading (romaji for kana, hiragana for kanji), the user picks the matching character. */
    ROMAJI_TO_CHARACTER,

    /** The app pronounces a reading (TTS), the user picks the character. */
    SOUND_TO_CHARACTER,

    /** Show a character, the user listens to several pronunciations (TTS) and picks the right one. */
    CHARACTER_TO_SOUND,

    /** Kanji only: show a character, the user picks its English meaning. */
    CHARACTER_TO_MEANING,

    /** Kanji only: show an English meaning, the user picks the matching character. */
    MEANING_TO_CHARACTER,
}
