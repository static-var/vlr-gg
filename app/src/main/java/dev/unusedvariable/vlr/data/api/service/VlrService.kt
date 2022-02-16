package dev.unusedvariable.vlr.data.api.service

import com.skydoves.sandwich.ApiResponse
import dev.unusedvariable.vlr.data.api.response.*
import retrofit2.http.GET
import retrofit2.http.Path

interface VlrService {
    @GET("/news/")
    suspend fun getNews(): ApiResponse<List<NewsResponseItem>>

    @GET("/matches/schedule")
    suspend fun getUpcomingMatches(): ApiResponse<List<MatchPreviewInfo>>

    @GET("/matches/results")
    suspend fun getCompletedMatches(): ApiResponse<List<MatchPreviewInfo>>

    @GET("/events")
    suspend fun getTournamentInfo(): ApiResponse<TournamentInfo>

    @GET("/events/{id}")
    suspend fun getTournamentDetails(@Path(value = "id") id: String): ApiResponse<TournamentDetails>

    @GET("/match/{id}")
    suspend fun getMatchDetails(@Path(value = "id") id: String): ApiResponse<MatchInfo>
}