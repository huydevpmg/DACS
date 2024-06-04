package com.dacs.vku.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dacs.vku.models.Notification

@Dao
interface DAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationDaoTa(notification: Notification): Long

    @Query("SELECT * FROM notificationDaoTao")
    fun getAllSavedNotificationDaoTao(): LiveData<List<Notification>>

    @Delete
    suspend fun deleteNotificationDaoTao(notification: Notification)
}