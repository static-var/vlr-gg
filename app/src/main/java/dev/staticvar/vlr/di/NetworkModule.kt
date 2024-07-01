package dev.staticvar.vlr.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.staticvar.vlr.BuildConfig
import dev.staticvar.vlr.utils.Constants
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
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
  @Singleton
  @IntoSet
  fun provideHttpLoggingInterceptor(): Interceptor {
    return HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
  }

  @Provides
  @Singleton
  @Named("vlrClient")
  fun provideKtorHttpClient(
    json: Json,
    interceptors: Set<@JvmSuppressWildcards Interceptor>,
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
      }

      engine {
        interceptors.forEach(::addInterceptor)
      }
    }
}
