package com.terinit.rhythmicmeditation.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.terinit.rhythmicmeditation.data.local.dao.MeditationInsightSnapshotDao
import com.terinit.rhythmicmeditation.data.local.dao.MeditationIntervalDao
import com.terinit.rhythmicmeditation.data.local.dao.MeditationSessionDao
import com.terinit.rhythmicmeditation.data.local.dao.SessionInterruptionEventDao
import com.terinit.rhythmicmeditation.data.local.entity.MeditationInsightSnapshotEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationIntervalEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationSessionEntity
import com.terinit.rhythmicmeditation.data.local.entity.SessionInterruptionEventEntity

/**
 * Local session ledger. Fully offline: no cloud sync, no remote database.
 */
@Database(
    entities = [
        MeditationSessionEntity::class,
        MeditationIntervalEntity::class,
        SessionInterruptionEventEntity::class,
        MeditationInsightSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeditationDatabase : RoomDatabase() {

    abstract fun meditationSessionDao(): MeditationSessionDao
    abstract fun meditationIntervalDao(): MeditationIntervalDao
    abstract fun sessionInterruptionEventDao(): SessionInterruptionEventDao
    abstract fun meditationInsightSnapshotDao(): MeditationInsightSnapshotDao

    companion object {
        const val DATABASE_NAME = "rhythmic_meditation.db"

        @Volatile
        private var instance: MeditationDatabase? = null

        fun getInstance(context: Context): MeditationDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): MeditationDatabase =
            Room.databaseBuilder(context, MeditationDatabase::class.java, DATABASE_NAME)
                // Schema is version 1 in Pass 1. Destructive fallback keeps local
                // recovery simple until real migrations are needed; the session
                // ledger is re-derivable from Routine recovery requests.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
