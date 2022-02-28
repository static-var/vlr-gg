package dev.unusedvariable.vlr.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.unusedvariable.vlr.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @Singleton
  fun provideJsonParser(): Json {
    return Json {
      ignoreUnknownKeys = true
      isLenient = true
      prettyPrint = true
    }
  }

  @Provides
  @Singleton
  fun provideKtorHttpClient(json: Json) =
      HttpClient(Android) {
        defaultRequest {
          host = "vlr-scraper.akhilnarang.dev/api/v1"
          url { protocol = URLProtocol.HTTPS }
        }
        install(JsonFeature) { serializer = KotlinxSerializer(json) }

        install(Logging) {
          level = LogLevel.ALL
          logger = Logger.SIMPLE
        }

        install(DefaultRequest) {
          headers {
            append(HttpHeaders.AcceptEncoding, "gzip")
            append(HttpHeaders.Authorization, BuildConfig.TOKEN)
          }
        }

        install(ContentEncoding) { gzip() }
      }
}
