package dev.staticvar.vlr.data.di

import dev.staticvar.vlr.data.repository.MatchRepositoryImpl
import dev.staticvar.vlr.domain.repository.MatchRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for data layer.
 * Provides repositories and mappers.
 */
fun dataModule(): Module = module {
  single<MatchRepository> {
    MatchRepositoryImpl(
      matchDataSource = get(),
      database = get(),
      dispatchers = get()
    )
  }
}
