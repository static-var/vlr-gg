package dev.unusedvariable.vlr.utils

object Constants {
    const val BASE_URL = "https://www.vlr.gg/"

    const val DB_NAME = "vlr"

    const val KEY_UPCOMING = "upcoming"
    const val KEY_COMPLETED = "completed"
    private const val KEY_MATCH = "match"

    fun matchDetailKey(detail: String) = KEY_MATCH + detail

    const val KEY_NEWS = "news"
    const val KEY_UPCOMING_MATCH = "upcoming_match"
    const val KEY_COMPLETED_MATCH = "completed_match"
    const val KEY_UPCOMING_TOURNAMENT = "upcoming_tournament"
    const val KEY_COMPLETED_TOURNAMENT = "completed_tournament"
    const val KEY_TOURNAMENT_ALL = "tournament_all"
    const val KEY_MATCH_ALL = "match_all"
    const val KEY_MATCH_DETAILS = "match_details_"
    const val KEY_TOURNAMENT_DETAILS = "tournament_details_"
}