package com.dacs.vku.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dacs.vku.models.Notification

@Database(
    entities = [Notification::class],
    version = 1
)
abstract class NotificationDatabase: RoomDatabase() {
    abstract fun getNotificationDaoTaoDao(): DAO

    companion object {
        @Volatile
        private var instance: NotificationDatabase? = null
        private val LOCK = Any()


        operator fun invoke(context: Context) = instance ?: synchronized(LOCK){
            instance ?: createDatabase(context).also { instance = it }
        }


        private fun createDatabase(context: Context): NotificationDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NotificationDatabase::class.java,
                "notification_db.db"
            ).build()
        }


    }

}