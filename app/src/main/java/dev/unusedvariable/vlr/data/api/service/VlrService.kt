package dev.unusedvariable.vlr.data.api.service

import com.skydoves.sandwich.ApiResponse
import dev.unusedvariable.vlr.data.api.response.*
import retrofit2.http.GET
import retrofit2.http.Path

interface VlrService {
    @GET("news")
    suspend fun getNews(): ApiResponse<List<NewsResponseItem>>

    @GET("matches")
    suspend fun getAllMatches(): ApiResponse<List<MatchPreviewInfo>>

    @GET("match/{id}")
    suspend fun getMatchDetails(@Path(value = "id") id: String): ApiResponse<MatchInfo>

    @GET("events")
    suspend fun getTournamentInfo(): ApiResponse<List<TournamentPreview>>

    @GET("events/{id}")
    suspend fun getTournamentDetails(@Path(value = "id") id: String): ApiResponse<TournamentDetails>
}