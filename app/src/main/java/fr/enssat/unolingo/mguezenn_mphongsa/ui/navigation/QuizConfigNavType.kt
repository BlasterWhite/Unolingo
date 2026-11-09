package fr.enssat.unolingo.mguezenn_mphongsa.ui.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

/** Type-safe routes only know primitives and enums: a [QuizConfig] travels as its JSON. */
object QuizConfigNavType : NavType<QuizConfig>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): QuizConfig? =
        bundle.getString(key)?.let(::parseValue)

    override fun put(bundle: Bundle, key: String, value: QuizConfig) = bundle.putString(key, serializeAsValue(value))

    override fun parseValue(value: String): QuizConfig = Json.decodeFromString(Uri.decode(value))

    override fun serializeAsValue(value: QuizConfig): String = Uri.encode(Json.encodeToString(value))
}

val QuizRouteTypeMap = mapOf(typeOf<QuizConfig>() to QuizConfigNavType)
