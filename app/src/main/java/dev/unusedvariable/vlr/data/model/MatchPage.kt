package dev.unusedvariable.vlr.data.model

import dev.unusedvariable.vlr.data.Status
import org.jsoup.nodes.Element

data class MatchPage(
    var status: Status = Status(),
    var days: List<Day> = emptyList()
) {
    val upcomingMatches: List<UpcomingMatch>
        get() = days.map { it.upcomingMatches }.flatten()

    val completedMatches: List<CompletedMatch>
        get() = days.map { it.completedMatches }.flatten()
}

data class Day(
    var date: String = "",
    var matches: List<Element> = emptyList()
) {

    val upcomingMatches: List<UpcomingMatch>
        get() = matches.map { match ->
            val matchData = UpcomingMatch()
            matchData.date = date
            matchData.upcomingId = match.attributes().get("href").split("/")[1]

            val texts = match.select("div.text-of")
            matchData.team1 = texts[0].ownText()
            matchData.team2 = texts[1].ownText()
            matchData.gameExtraInfo = "${texts[2].ownText()} ${texts[3].ownText()}"

            try {
                val score = match.select("div.js-spoiler")
                matchData.team1Score = score[0].ownText()
                matchData.team2Score = score[1].ownText()

                matchData.isLive = match.select("div.ml-status").first()!!.ownText() == "LIVE"
                matchData.eta = match.select("div.ml-eta").first()!!.ownText()
            } catch (e: Exception) {
            }
            matchData
        }


    val completedMatches: List<CompletedMatch>
        get() = matches.map { match ->
            val matchData = CompletedMatch()
            matchData.date = date
            matchData.completedId = match.attributes().get("href").split("/")[1]

            val texts = match.select("div.text-of")
            matchData.team1 = texts[0].ownText()
            matchData.team2 = texts[1].ownText()
            matchData.gameExtraInfo = "${texts[2].ownText()} ${texts[3].ownText()}"

            try {
                val score = match.select("div.js-spoiler")
                matchData.team1Score = score[0].ownText()
                matchData.team2Score = score[1].ownText()

                matchData.isLive = match.select("div.ml-status").first()!!.ownText() == "LIVE"
                matchData.eta = match.select("div.ml-eta").first()!!.ownText()
            } catch (e: Exception) {
            }
            matchData
        }
}
