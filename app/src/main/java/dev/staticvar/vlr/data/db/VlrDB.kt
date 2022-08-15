package dev.staticvar.vlr.data.db

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
    ],
  exportSchema = false,
  version = 8
)
@TypeConverters(VlrTypeConverter::class)
abstract class VlrDB : RoomDatabase() {
  abstract fun getVlrDao(): VlrDao
}

val Migration_7_8 =
  object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
      val time = System.currentTimeMillis().toString()
      database.execSQL("ALTER TABLE MatchInfo ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
      database.execSQL("ALTER TABLE TeamDetails ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
      database.execSQL("ALTER TABLE TournamentDetails ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")

      database.execSQL("UPDATE MatchInfo set createdAt = $time where createdAt = 0")
      database.execSQL("UPDATE TeamDetails set createdAt = $time where createdAt = 0")
      database.execSQL("UPDATE TournamentDetails set createdAt = $time where createdAt = 0")
    }
  }

/**
 * Version change log 3 -> Remove old tables and create new for accommodating API calls
 *
 * 4 -> Add FCM tracker table
 * 5 -> Add more stats to players in match
 * 6 -> Add status in events for MatchDetailsScreen
 * 7 -> Add rounds information for every match
 */
