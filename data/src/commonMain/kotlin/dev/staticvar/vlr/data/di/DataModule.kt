package dev.staticvar.vlr.data.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for data layer.
 * Provides repositories and mappers.
 */
fun dataModule(): Module = module {
  // Mapping-only module for now. Repository wiring for matches removed pending new mapping implementation.
}
