package dev.staticvar.vlr.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.staticvar.vlr.data.api.response.*
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.model.TopicTracker

@Database(
  entities =
    [
      NewsResponseItem::class,
      MatchPreviewInfo::class,
      MatchInfo::class,
      TournamentPreview::class,
      TournamentDetails::class,
      TopicTracker::class,
      TeamDetails::class,
      PlayerData::class],
  exportSchema = true,
  version = 10,
  autoMigrations = [AutoMigration(9, 10)]
)
@TypeConverters(VlrTypeConverter::class)
abstract class VlrDB : RoomDatabase() {
  abstract fun getVlrDao(): VlrDao
}

val Migration_7_8 =
  object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
      val time = System.currentTimeMillis().toString()
      database.execSQL("ALTER TABLE MatchInfo ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $time")
      database.execSQL(
        "ALTER TABLE TeamDetails ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $time"
      )
      database.execSQL(
        "ALTER TABLE TournamentDetails ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $time"
      )
    }
  }

/**
 * ************************************** Version change log ***************************************
 * 3 -> Remove old tables and create new for accommodating API call ................................
 * 4 -> Add FCM tracker table ......................................................................
 * 5 -> Add more stats to players in match .........................................................
 * 6 -> Add status in events for MatchDetailsScreen ................................................
 * 7 -> Add rounds information for every match .....................................................
 * 8 -> Add creating time of each record for MatchInfo, TeamDetails, TournamentDetails .............
 * 9 -> Add event status field in TournamentDetails ................................................
 * 10 -> Add PlayerData table to store player records ..............................................
 */
