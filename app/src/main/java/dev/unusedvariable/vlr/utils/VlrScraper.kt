package dev.unusedvariable.vlr.utils

import com.github.ajalt.timberkt.e
import dev.unusedvariable.vlr.data.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object VlrScraper {


    fun getMatches(upcoming: Boolean): List<MatchData> {
        val page = MatchPage()
        val request =
            Jsoup.connect(if (upcoming) "https://www.vlr.gg/matches" else "https://www.vlr.gg/matches/results")
                .execute()

        page.status.code = request.statusCode()
        page.status.message = request.statusMessage()

        val days = mutableListOf<Day>()

        val doc = request.parse()
        doc.select("div.wf-label.mod-large").forEachIndexed { index, docElement ->
            val day = Day()

            day.date = docElement.ownText()

            val matchGroup = docElement.siblingElements().filter {
                it.className() == "wf-card"
            }[index]

            val matchList =
                matchGroup.select("a.wf-module-item.match-item.mod-color.mod-left")

            day.matches = matchList

            days.add(day)
        }
        page.days = days

        return if (upcoming)
            page.upcomingMatches
        else
            page.completedMatches
    }

    fun matchDetailsJsoup(matchUrl: String): MatchDetails {
        return try {
            val details = MatchDetails()
            details.matchId = matchUrl.split("/").last()
            val request =
                Jsoup.connect(matchUrl)
                    .execute()
            details.status.code = request.statusCode()
            details.status.message = request.statusMessage()

            val response = request.parse()
            val headerCard = requireNotNull(response.selectFirst("div.wf-card.match-header"))

            details.live = try {
                headerCard.selectFirst("span.match-header-vs-note.mod-live")?.ownText() == "live"
            } catch (e: Exception) {
                false
            }

            details.eta = try {
                headerCard.selectFirst("span.match-header-vs-note.mod-upcoming")?.ownText()
            } catch (e: Exception) {
                null
            }

            details.pickAndBans = try {
                headerCard.selectFirst("div.match-header-note")?.ownText()
            } catch (e: Exception) {
                null
            }


            headerCard.selectFirst("div.wf-title-med")?.ownText()?.let {
                details.team1 = it
            }
            headerCard.select("div.wf-title-med")[1]?.ownText()?.let {
                details.team2 = it
            }

            details.scoreLine = try {
                headerCard.select("div.js-spoiler")[0].select("span")
                    .joinToString(separator = "") { it.ownText() }
            } catch (e: Exception) {
                "-"
            }

            details.matchNotes =
                headerCard.select("div.match-header-vs-note")[1].ownText()

            headerCard.select("a.match-header-link.wf-link-hover")[0].select("img").attr("src")
                .let {
                    details.team1Url =
                        "https:" + if (it.startsWith("//")) it else "//www.vlr.gg$it"
                }

            headerCard.select("a.match-header-link.wf-link-hover")[1].select("img").attr("src")
                .let {
                    details.team2Url =
                        "https:" + if (it.startsWith("//")) it else "//www.vlr.gg$it"
                }


            details.date = headerCard.select("div.moment-tz-convert")[0].ownText()
            details.time = headerCard.select("div.moment-tz-convert")[1].ownText()

            details.patchInfo = try {
                headerCard.select("div.moment-tz-convert")[1].lastElementSibling()
                    .children()[0].ownText()
            } catch (e: Exception) {
                null
            }

            // Stream info

            details.streams = try {
                val streams = response.select("div.match-streams-container")[0]
                val channelNames = streams.select("span").map { it.ownText() }
                val links = streams.select("a").map { it.attr("href") }

                channelNames.mapIndexed { index, s -> Pair(s, links[index] ?: "") }
            } catch (e: Exception) {
                null
            }


            // VOD info
            details.vod = try {
                val vods = response.select("div.match-streams-container")[1]
                val info = vods.select("a")[0]
                Pair(info.ownText(), info.attr("href"))
            } catch (e: Exception) {
                null
            }

            // Map names and links
            details.mapInfo = try {
                response.select("div.vm-stats")[0].select("div.vm-stats-gamesnav-item")[0].siblingElements()
                    .map {
                        val link =
                            it.attr("data-game-id")
                        val mapName = it.children()[0].ownText()
                        Pair(mapName, link)
                    }
            } catch (e: Exception) {
                null
            }

            val statsContainer = response.select("div.vm-stats-container")[0]

            details.mapData = statsContainer.children()
                .filter { page ->
                    details.mapInfo!!.map { it.second }
                        .contains(page.attr("data-game-id"))
                }.mapIndexed { index, statBox ->
                    val mapData = MapData()
                    mapData.mapName = details.mapInfo?.get(index)?.first ?: ""
                    val gameHeader =
                        statBox.select("div.vm-stats-game-header")[0]
                    mapData.team1Score =
                        gameHeader.select("div.score")[0].ownText()
                    mapData.team2Score =
                        gameHeader.select("div.score")[1].ownText()
                    mapData.team1 =
                        gameHeader.select("div.team-name")[0].ownText()
                    mapData.team2 =
                        gameHeader.select("div.team-name")[1].ownText()
                    mapData.isMapComplete =
                        gameHeader.select("div.map-duration.ge-text-light")[0].ownText() != "-"
//
//                    println(details.toString())

                    if (mapData.isMapComplete) {
                        mapData.team1Players =
                            statBox.select("table.wf-table-inset.mod-overview")[0]
                                .select("tbody")[0].select("tr")
                                .map { parsePlayers(it) }
                        mapData.team2Players =
                            statBox.select("table.wf-table-inset.mod-overview")[1]
                                .select("tbody")[0].select("tr")
                                .map { parsePlayers(it) }
                    }
                    mapData
                }
            details
        } catch (e: Exception) {
            e { "${e.printStackTrace()}" }
            throw e
        }
    }

    /*suspend fun matchDetails(matchUrl: String): MatchDetails {
        try {
            val scrapper = skrape(fetcher = scraper) {
                request {
                    url = matchUrl
                    method = Method.GET
                    timeout = 10000
                    headers = Constants.headerMap
                }
                response {
                    val details = MatchDetails()
                    details.matchId = matchUrl.split("/").last()
                    status {
                        e { "$code $message" }
                        details.status.code = code
                        details.status.message = message
                    }
                    htmlDocument {
                        div {
                            // Basic match info
                            val headerCard = findFirst("div.wf-card.match-header")
                            details.live = try {
                                headerCard.findFirst("span.match-header-vs-note.mod-live").ownText == "live"
                            } catch (e: ElementNotFoundException) {
                                false
                            }

                            details.eta = try {
                                headerCard.findFirst("span.match-header-vs-note.mod-upcoming").ownText
                            } catch (e: ElementNotFoundException) {
                                null
                            }

                            details.pickAndBans = try {
                                headerCard.findFirst("div.match-header-note").ownText
                            } catch (e: ElementNotFoundException) {
                                null
                            }

                            details.team1 = headerCard.findFirst("div.wf-title-med").ownText
                            details.team2 = headerCard.findSecond("div.wf-title-med").ownText

                            details.scoreLine = try {
                                headerCard.findAll("div.js-spoiler")[0].findAll("span")
                                    .joinToString(separator = "") { it.ownText }
                            } catch (e: ElementNotFoundException) {
                                "-"
                            }

                            details.matchNotes =
                                headerCard.findSecond("div.match-header-vs-note").ownText

                            headerCard.findFirst("a.match-header-link.wf-link-hover").eachImage.values.first()
                                .let {
                                    details.team1Url =
                                        "https:" + if (it.startsWith("//")) it else "//www.vlr.gg$it"
                                }

                            headerCard.findSecond("a.match-header-link.wf-link-hover").eachImage.values.first()
                                .let {
                                    details.team2Url =
                                        "https:" + if (it.startsWith("//")) it else "//www.vlr.gg$it"
                                }

                            details.date = headerCard.findFirst("div.moment-tz-convert").ownText
                            details.time = headerCard.findSecond("div.moment-tz-convert").ownText

                            details.patchInfo = try {
                                headerCard.findSecond("div.moment-tz-convert").siblings.last().children.first().ownText
                            } catch (e: Exception) {
                                null
                            }

                            // Stream info

                            details.streams = try {
                                val streams = findFirst("div.match-streams-container")
                                val channelNames = streams.findAll("span").map { it.ownText }
                                val links = streams.findAll("a").map { it.attributes["href"] }

                                channelNames.mapIndexed { index, s -> Pair(s, links[index] ?: "") }
                            } catch (e: ElementNotFoundException) {
                                null
                            }


                            // VOD info
                            details.vod = try {
                                val vods = findSecond("div.match-streams-container")
                                val info = vods.findAll("a").first()
                                Pair(info.ownText, info.attribute("href"))
                            } catch (e: ElementNotFoundException) {
                                null
                            }

                            // Map names and links
                            details.mapInfo = try {
                                findFirst("div.vm-stats").findFirst("div.vm-stats-gamesnav-item").siblings.map {
                                    val link =
                                        it.attribute("data-game-id")
                                    val mapName = it.children.first().ownText
                                    Pair(mapName, link)
                                }
                            } catch (e: Exception) {
                                null
                            }

                            val statsContainer = findFirst("div.vm-stats-container")
                            statsContainer.children.forEach { e { "properties ${it.dataAttributes}" } }
                            details.mapData = statsContainer.children
                                .filter { page ->
                                    details.mapInfo!!.map { it.second }
                                        .contains(page.attribute("data-game-id"))
                                }
                                .mapIndexed { index, statBox ->
                                    val mapData = MapData()
                                    mapData.mapName = details.mapInfo?.get(index)?.first ?: ""
                                    val gameHeader =
                                        statBox.findFirst("div.vm-stats-game-header")
                                    mapData.team1Score =
                                        gameHeader.findFirst("div.score").ownText
                                    mapData.team2Score =
                                        gameHeader.findSecond("div.score").ownText
                                    mapData.team1 =
                                        gameHeader.findFirst("div.team-name").ownText
                                    mapData.team2 =
                                        gameHeader.findSecond("div.team-name").ownText
                                    mapData.isMapComplete =
                                        gameHeader.findFirst("div.map-duration.ge-text-light").ownText != "-"

                                    if (mapData.isMapComplete) {
                                        mapData.team1Players =
                                            statBox.findFirst("table.wf-table-inset.mod-overview")
                                                .findFirst("tbody").findAll("tr")
                                                .map { parsePlayers(it) }
                                        mapData.team2Players =
                                            statBox.findSecond("table.wf-table-inset.mod-overview")
                                                .findFirst("tbody").findAll("tr")
                                                .map { parsePlayers(it) }
                                    }
                                    mapData
                                }
                        }

                    }
                    details
                }
            }
            e { scrapper.toString() }
            return scrapper
        } catch (e: ElementNotFoundException) {
            e.printStackTrace()
            throw e
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }*/

    private fun parsePlayers(doc: Element): PlayerData {
        val player = doc.select("td.mod-player")[0].select("a")[0].select("div")[0].ownText()
        val org = doc.select("td.mod-player")[0].select("a")[0].select("div")[1].ownText()
        val agent = doc.select("td.mod-agents").select("img").attr("src")

        val combatStats = doc.select("td.mod-stat").map { doc ->
            try {
                if (doc.children().first()!!.ownText().isNotBlank()) {
                    doc.children().first()!!.ownText()
                } else
                    doc.children().first()!!.children()[1].ownText()
            } catch (e: Exception) {
                ""
            }
        }

        return PlayerData(
            player,
            org,
            Constants.BASE_URL + agent.removePrefix("/"),
            CombatStats(
                combatStats[0],
                combatStats[1],
                combatStats[2],
                combatStats[3],
                combatStats[4],
                combatStats[5],
                combatStats[6]
            )
        )
    }
}