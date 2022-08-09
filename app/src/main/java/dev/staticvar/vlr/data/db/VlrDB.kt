package dev.staticvar.vlr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
  version = 7
)
@TypeConverters(VlrTypeConverter::class)
abstract class VlrDB : RoomDatabase() {
  abstract fun getVlrDao(): VlrDao
}

/**
 * Version change log 3 -> Remove old tables and create new for accommodating API calls
 *
 * 4 -> Add FCM tracker table 5 -> Add more stats to players in match
 * 6 -> Add status in events for MatchDetailsScreen
 * 7 -> Add rounds information for every match
 */
