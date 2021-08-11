package dev.unusedvariable.vlr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.unusedvariable.vlr.data.dao.CompletedMatchDao
import dev.unusedvariable.vlr.data.dao.MatchDetailsDao
import dev.unusedvariable.vlr.data.dao.UpcomingMatchDao
import dev.unusedvariable.vlr.data.model.CompletedMatch
import dev.unusedvariable.vlr.data.model.MatchDetails
import dev.unusedvariable.vlr.data.model.UpcomingMatch

@Database(
    entities = [UpcomingMatch::class, CompletedMatch::class, MatchDetails::class],
    exportSchema = false,
    version = 1
)
@TypeConverters(VlrTypeConverter::class)
abstract class VlrDB : RoomDatabase() {

    abstract fun getCompletedMatchDao(): CompletedMatchDao
    abstract fun getUpcomingMatchDao(): UpcomingMatchDao
    abstract fun getMatchDetailsDao(): MatchDetailsDao

}