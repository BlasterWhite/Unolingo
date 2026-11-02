package fr.enssat.unolingo.mguezenn_mphongsa.domain.kana

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KanaToRomajiTest {
    private fun romaji(reading: String) = KanaToRomaji.toRomaji(reading)

    @Test
    fun basicKana_useHepburn() {
        assertEquals("shi", romaji("し"))
        assertEquals("tsu", romaji("つ"))
        assertEquals("fu", romaji("ふ"))
        assertEquals("ji", romaji("ぢ"))
        assertEquals("zu", romaji("づ"))
        assertEquals("wo", romaji("を"))
        assertEquals("nihon", romaji("にほん"))
    }

    @Test
    fun longVowels_staySpelledLikeTheKana() {
        assertEquals("kou", romaji("こう"))
        assertEquals("juu", romaji("じゅう"))
        assertEquals("kuu", romaji("くう"))
    }

    @Test
    fun yoon_isMatchedAsTwoCharacters() {
        assertEquals("kya", romaji("きゃ"))
        assertEquals("sho", romaji("しょ"))
        assertEquals("cha", romaji("ちゃ"))
        assertEquals("ja", romaji("じゃ"))
        assertEquals("ryuu", romaji("りゅう"))
        assertEquals("shokuji", romaji("しょくじ"))
    }

    @Test
    fun smallTsu_doublesTheNextConsonant() {
        assertEquals("kakki", romaji("かっき"))
        assertEquals("zasshi", romaji("ざっし"))
        assertEquals("kitte", romaji("きって"))
        assertEquals("sappari", romaji("さっぱり"))
        assertEquals("kokkyou", romaji("こっきょう"))
    }

    @Test
    fun smallTsuBeforeChi_givesTch() {
        assertEquals("matchi", romaji("まっち"))
        assertEquals("matcha", romaji("まっちゃ"))
    }

    @Test
    fun n_beforeVowelOrY_getsAnApostrophe() {
        assertEquals("iwan'ya", romaji("いわんや"))
        assertEquals("shin'ai", romaji("しんあい"))
        assertEquals("kin'en", romaji("きんえん"))
    }

    @Test
    fun n_beforeConsonantOrAtTheEnd_hasNoApostrophe() {
        assertEquals("kanji", romaji("かんじ"))
        assertEquals("onna", romaji("おんな"))
        assertEquals("hon", romaji("ほん"))
        assertEquals("nn", romaji("んん"))
    }

    @Test
    fun longMark_repeatsThePreviousVowel() {
        assertEquals("deshimeetoru", romaji("でしめーとる"))
        assertEquals("raamen", romaji("ラーメン"))
        assertEquals("kyuu", romaji("きゅー"))
    }

    @Test
    fun katakana_isConverted() {
        assertEquals("kou", romaji("コウ"))
        assertEquals("shokku", romaji("ショック"))
        assertEquals("matchi", romaji("マッチ"))
        assertEquals("fan", romaji("ファン"))
        assertEquals("vu", romaji("ヴ"))
    }

    @Test
    fun katakanaToHiragana_shiftsTheCodePoints() {
        assertEquals("かたかな", KanaToRomaji.katakanaToHiragana("カタカナ"))
        assertEquals("ぎゃっぷー", KanaToRomaji.katakanaToHiragana("ギャップー"))
        assertEquals("ひらがな abc 日", KanaToRomaji.katakanaToHiragana("ひらがな abc 日"))
    }

    @Test
    fun unconvertibleInput_returnsNull() {
        assertNull(romaji(""))
        assertNull(romaji("っ"))
        assertNull(romaji("かっ")) // っ at the end
        assertNull(romaji("がっ"))
        assertNull(romaji("じっ"))
        assertNull(romaji("おっ"))
        assertNull(romaji("っこ")) // っ at the start
        assertNull(romaji("あっい")) // っ before a vowel
        assertNull(romaji("かっん")) // っ before ん
        assertNull(romaji("かっっか"))
        assertNull(romaji("ー")) // nothing to repeat
        assertNull(romaji("んー"))
        assertNull(romaji("かっー"))
        assertNull(romaji("ゃ")) // lone small kana
        assertNull(romaji("kana")) // not kana
        assertNull(romaji("日本"))
        assertNull(romaji("か な"))
    }
}
