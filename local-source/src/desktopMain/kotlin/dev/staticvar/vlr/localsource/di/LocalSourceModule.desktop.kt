package dev.staticvar.vlr.localsource.di

import dev.staticvar.vlr.localsource.database.DatabaseDriverFactory
import org.koin.dsl.module

/**
 * Desktop (JVM)-specific Koin module for database driver factory.
 */
actual val platformLocalSourceModule = module {
    single { DatabaseDriverFactory() }
}
