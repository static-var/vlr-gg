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
import dev.unusedvariable.vlr.data.dao.CompletedMatchDao
import dev.unusedvariable.vlr.data.dao.MatchDetailsDao
import dev.unusedvariable.vlr.data.dao.UpcomingMatchDao
import dev.unusedvariable.vlr.data.db.VlrDB
import dev.unusedvariable.vlr.data.db.VlrTypeConverter
import dev.unusedvariable.vlr.utils.Constants
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun typeConverter(moshi: Moshi) = VlrTypeConverter(moshi)

    @Provides
    @Singleton
    fun db(application: Application, converter: VlrTypeConverter) =
        Room.databaseBuilder(
            application, VlrDB::class.java, Constants.DB_NAME
        ).addTypeConverter(converter).build()

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
    fun getVlrRepository(
        upcomingMatchDao: UpcomingMatchDao,
        completedMatchDao: CompletedMatchDao,
        matchDetailsDao: MatchDetailsDao
    ) = VlrRepository(
        completedMatchDao, upcomingMatchDao, matchDetailsDao
    )
}