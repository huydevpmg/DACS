package com.dacs.vku.repository

import com.dacs.vku.api.RetrofitInstance
import com.dacs.vku.db.NotificationDatabase
import com.dacs.vku.models.Notification
import retrofit2.http.Query

class NotificationRepository(val DB: NotificationDatabase) {
    suspend fun getNotificationDaoTao() = RetrofitInstance.api.getNotificationDaoTao()
    suspend fun getNotificationCTSV() = RetrofitInstance.api.getNotificationCTSV()
    suspend fun getNotificationKHTC() = RetrofitInstance.api.getNotificationKHTC()
    suspend fun getNotificationKTDBCL() = RetrofitInstance.api.getNotificationKTDBCL()

   suspend fun searchNotification(searchQuery: String) = RetrofitInstance.api.searchForNotification(searchQuery)
//







    //Insert the fav noti
    suspend fun upsert(notification: Notification) = DB.getNotificationDaoTaoDao().insertNotificationDaoTa(notification)


//Get fav noti
    fun getSavedNotification() = DB.getNotificationDaoTaoDao().getAllSavedNotificationDaoTao()

    suspend fun deleteNotification(notification: Notification) = DB.getNotificationDaoTaoDao().deleteNotificationDaoTao(notification)




}