package com.dacs.vku.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dacs.vku.models.Notification

@Dao
interface DAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationDaoTa(notification: Notification)

    @Query("SELECT * FROM Notification")
    fun getAllSavedNotificationDaoTao(): LiveData<List<Notification>>

    @Delete
    suspend fun deleteNotificationDaoTao(notification: Notification)

    @Update
    suspend fun updateNotification(notification: Notification)
}