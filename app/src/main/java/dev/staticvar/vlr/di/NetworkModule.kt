package dev.staticvar.vlr.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.staticvar.vlr.BuildConfig
import dev.staticvar.vlr.utils.BrotliEncoder
import dev.staticvar.vlr.utils.Constants
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @Singleton
  fun provideJsonParser(): Json {
    return Json {
      ignoreUnknownKeys = true
      isLenient = true
    }
  }

  @Provides
  fun provideHttpConnectionPool(): ConnectionPool {
    return ConnectionPool()
  }

  @Provides
  @Singleton
  fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
  }

  @Provides
  @Singleton
  @Named("vlrClient")
  fun provideKtorHttpClient(
    json: Json,
    connectionPool: ConnectionPool,
    httpLoggingInterceptor: HttpLoggingInterceptor
  ) =
    HttpClient(OkHttp) {
      defaultRequest {
        host = Constants.BASE_URL
        url { protocol = URLProtocol.HTTPS }
      }
      install(ContentNegotiation) { json(json) }

      install(DefaultRequest) {
        headers {
          append(HttpHeaders.AcceptEncoding, "gzip")
          append(HttpHeaders.Authorization, BuildConfig.TOKEN)
          append(Constants.APPLICATION_HEADER, BuildConfig.APPLICATION_ID)
          append(Constants.BUILD_TYPE_HEADER, BuildConfig.BUILD_TYPE)
          append(Constants.VERSION_HEADER, BuildConfig.VERSION_NAME)
        }
      }

      install(ContentEncoding) {
        gzip()
        customEncoder(BrotliEncoder)
      }

      engine {
        addInterceptor(httpLoggingInterceptor)
        config { connectionPool(connectionPool) }
      }
    }
}
