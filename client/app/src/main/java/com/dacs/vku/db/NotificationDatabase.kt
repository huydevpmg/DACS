package com.dacs.vku.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dacs.vku.models.Notification

@Database(
    entities = [Notification::class],
    version = 2
)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun getNotificationDaoTaoDao(): DAO

    companion object {
        @Volatile
        private var instance: NotificationDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: createDatabase(context).also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Notification ADD COLUMN note TEXT")
            }
        }

        private fun createDatabase(context: Context): NotificationDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NotificationDatabase::class.java,
                "notification_db.db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()  // Add this line for development purposes to reset the database if migration fails
                .build()
        }
    }
}
