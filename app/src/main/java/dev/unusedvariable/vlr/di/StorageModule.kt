package dev.unusedvariable.vlr.di

import android.app.Application
import androidx.room.Room
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.unusedvariable.vlr.data.VlrRepository
import dev.unusedvariable.vlr.data.api.service.VlrService
import dev.unusedvariable.vlr.data.dao.CompletedMatchDao
import dev.unusedvariable.vlr.data.dao.MatchDetailsDao
import dev.unusedvariable.vlr.data.dao.UpcomingMatchDao
import dev.unusedvariable.vlr.data.dao.VlrDao
import dev.unusedvariable.vlr.data.db.VlrDB
import dev.unusedvariable.vlr.data.db.VlrTypeConverter
import dev.unusedvariable.vlr.utils.Constants
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun typeConverter(moshi: Moshi, json: Json) = VlrTypeConverter(moshi, json)

    @Provides
    @Singleton
    fun db(application: Application, converter: VlrTypeConverter) =
        Room.databaseBuilder(
            application, VlrDB::class.java, Constants.DB_NAME
        ).fallbackToDestructiveMigration().addTypeConverter(converter).build()

    @Provides
    @Singleton
    fun getCompletedMatchDao(db: VlrDB) = db.getCompletedMatchDao()

    @Provides
    @Singleton
    fun getUpcomingMatchDao(db: VlrDB) = db.getUpcomingMatchDao()

    @Provides
    @Singleton
    fun getMatchDetailsDao(db: VlrDB) = db.getMatchDetailsDao()

    @Provides
    @Singleton
    fun getVlrDao(db: VlrDB) = db.getVlrDao()

    @Provides
    @Singleton
    fun getVlrRepository(
        upcomingMatchDao: UpcomingMatchDao,
        completedMatchDao: CompletedMatchDao,
        matchDetailsDao: MatchDetailsDao,
        vlrService: VlrService,
        vlrDao: VlrDao,
    ) = VlrRepository(
        completedMatchDao, upcomingMatchDao, matchDetailsDao, vlrService, vlrDao
    )
}