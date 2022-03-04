package dev.staticvar.vlr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.staticvar.vlr.data.api.response.*
import dev.staticvar.vlr.data.dao.VlrDao

@Database(
    entities =
        [
            NewsResponseItem::class,
            MatchPreviewInfo::class,
            MatchInfo::class,
            TournamentPreview::class,
            TournamentDetails::class],
    exportSchema = false,
    version = 3)
@TypeConverters(VlrTypeConverter::class)
abstract class VlrDB : RoomDatabase() {
  abstract fun getVlrDao(): VlrDao
}
