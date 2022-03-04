package dev.staticvar.vlr.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.staticvar.vlr.data.VlrRepository
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.db.VlrDB
import dev.staticvar.vlr.data.db.VlrTypeConverter
import dev.staticvar.vlr.utils.Constants
import io.ktor.client.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

  @Provides @Singleton fun typeConverter(json: Json) = VlrTypeConverter(json)

  @Provides
  @Singleton
  fun db(application: Application, converter: VlrTypeConverter) =
      Room.databaseBuilder(application, VlrDB::class.java, Constants.DB_NAME)
          .fallbackToDestructiveMigration()
          .addTypeConverter(converter)
          .build()

  @Provides @Singleton fun getVlrDao(db: VlrDB) = db.getVlrDao()

  @Provides
  @Singleton
  fun getVlrRepository(vlrDao: VlrDao, ktorHttpClient: HttpClient) =
      VlrRepository(vlrDao, ktorHttpClient)
}
