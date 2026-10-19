package bzh.enssat.unolingo.data

import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Parses the character files in `assets/characters/`. */
object CharacterDataParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<JapaneseCharacter> =
        json.decodeFromString<CharacterFileDto>(text).characters.map { it.toDomain() }

    @Serializable
    private data class CharacterFileDto(val characters: List<CharacterDto>)

    @Serializable
    private data class CharacterDto(
        val symbol: String,
        val readings: List<String>,
        val alternatives: List<String> = emptyList(),
        val meaning: String? = null,
        val similar: List<String> = emptyList(),
    ) {
        fun toDomain() = JapaneseCharacter(symbol, readings, alternatives, meaning, similar.toSet())
    }
}
