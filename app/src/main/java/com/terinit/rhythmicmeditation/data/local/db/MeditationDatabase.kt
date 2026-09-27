package com.terinit.rhythmicmeditation.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.terinit.rhythmicmeditation.data.local.dao.EveningMeditationDao
import com.terinit.rhythmicmeditation.data.local.dao.MeditationInsightSnapshotDao
import com.terinit.rhythmicmeditation.data.local.dao.MeditationIntervalDao
import com.terinit.rhythmicmeditation.data.local.dao.MeditationSessionDao
import com.terinit.rhythmicmeditation.data.local.dao.SessionInterruptionEventDao
import com.terinit.rhythmicmeditation.data.local.dao.SessionTimeCheckpointDao
import com.terinit.rhythmicmeditation.data.local.entity.EveningMeditationEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationInsightSnapshotEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationIntervalEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationSessionEntity
import com.terinit.rhythmicmeditation.data.local.entity.SessionInterruptionEventEntity
import com.terinit.rhythmicmeditation.data.local.entity.SessionTimeCheckpointEntity

/**
 * Local session ledger. Fully offline: no cloud sync, no remote database.
 */
@Database(
    entities = [
        MeditationSessionEntity::class,
        MeditationIntervalEntity::class,
        SessionInterruptionEventEntity::class,
        MeditationInsightSnapshotEntity::class,
        SessionTimeCheckpointEntity::class,
        EveningMeditationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MeditationDatabase : RoomDatabase() {

    abstract fun meditationSessionDao(): MeditationSessionDao
    abstract fun meditationIntervalDao(): MeditationIntervalDao
    abstract fun sessionInterruptionEventDao(): SessionInterruptionEventDao
    abstract fun meditationInsightSnapshotDao(): MeditationInsightSnapshotDao
    abstract fun sessionTimeCheckpointDao(): SessionTimeCheckpointDao
    abstract fun eveningMeditationDao(): EveningMeditationDao

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
                // Schema history: v2 (Pass 2) added session_time_checkpoints;
                // v3 adds evening_meditation (evening wind-down records). The
                // destructive fallback is kept deliberately until real
                // migrations are needed: the session ledger is re-derivable
                // from Routine recovery requests and local sessions are
                // practice evidence, not irreplaceable data.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
