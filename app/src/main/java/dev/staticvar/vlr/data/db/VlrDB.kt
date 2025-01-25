package dev.staticvar.vlr.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.staticvar.vlr.data.api.response.MatchInfo
import dev.staticvar.vlr.data.api.response.MatchPreviewInfo
import dev.staticvar.vlr.data.api.response.NewsResponseItem
import dev.staticvar.vlr.data.api.response.PlayerData
import dev.staticvar.vlr.data.api.response.TeamDetails
import dev.staticvar.vlr.data.api.response.TournamentDetails
import dev.staticvar.vlr.data.api.response.TournamentPreview
import dev.staticvar.vlr.data.dao.EventFavDao
import dev.staticvar.vlr.data.dao.MatchFavDao
import dev.staticvar.vlr.data.dao.TeamFavDao
import dev.staticvar.vlr.data.dao.VlrDao
import dev.staticvar.vlr.data.model.EventFav
import dev.staticvar.vlr.data.model.MatchFav
import dev.staticvar.vlr.data.model.TeamFav
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
      PlayerData::class,
      TeamFav::class,
      MatchFav::class,
      EventFav::class
    ],
  exportSchema = true,
  version = 14,
  autoMigrations = [AutoMigration(9, 10), AutoMigration(12, 13), AutoMigration(13, 14)]
)
@TypeConverters(VlrTypeConverter::class)
abstract class VlrDB : RoomDatabase() {
  abstract fun getVlrDao(): VlrDao
  abstract fun getEventFavDao(): EventFavDao
  abstract fun getMatchFavDao(): MatchFavDao
  abstract fun getTeamFavDao(): TeamFavDao
}

val Migration_7_8 =
  object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
      val time = System.currentTimeMillis().toString()
      db.execSQL("ALTER TABLE MatchInfo ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $time")
      db.execSQL(
        "ALTER TABLE TeamDetails ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $time"
      )
      db.execSQL(
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
 * 11 -> Remove redundant brackets info from TournamentDetails .....................................
 */
