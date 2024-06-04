package com.dacs.vku.api

import com.dacs.vku.models.Alarm
import com.dacs.vku.models.Notification
import com.dacs.vku.models.NotificationResponse
import com.dacs.vku.models.Schedule
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationAPI {
    @GET("dao-tao")
    suspend fun getNotificationDaoTao(): Response<MutableList<Notification>>

    @GET("ctsv")
    suspend fun getNotificationCTSV(): Response<MutableList<Notification>>

    @GET("khtc")
    suspend fun getNotificationKHTC(): Response<MutableList<Notification>>

    @GET("ktdbcl")
    suspend fun getNotificationKTDBCL(): Response<MutableList<Notification>>

    @GET("search")
    suspend fun searchForNotification(
        @Query("title")
        searchQuery: String
    ): Response<MutableList<Notification>>

    @POST("verify-user")
    fun verifyUser(@Body userData: UserData): Call<ResponseBody>

    @POST("add-schedule")
    fun addCalendar(@Body schedule: Schedule): Call<ResponseBody>

    @POST("delete-schedule")
    fun deleteSchedule(@Body schedule: Map<String, String>): Call<ResponseBody>
    @POST("update-schedule")
    fun updateSchedule(@Body schedule: Schedule): Call<ResponseBody>
    @GET("get-all-schedules/{userId}")
    fun getAllSchedules(@Path("userId") userId: String): Call<List<Schedule>>
    @GET("add-alarm")
    fun addAlarm(@Body alarm: Alarm): Call<ResponseBody>

}

