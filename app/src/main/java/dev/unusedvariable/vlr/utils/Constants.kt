package dev.unusedvariable.vlr.utils

object Constants {
    const val BASE_URL = "https://www.vlr.gg/"

    const val DB_NAME = "vlr"

    const val KEY_UPCOMING = "upcoming"
    const val KEY_COMPLETED = "completed"
    private const val KEY_MATCH = "match"

    fun matchDetailKey(detail: String) = KEY_MATCH + detail

    val headerMap = mapOf<String, String>(
    )
}