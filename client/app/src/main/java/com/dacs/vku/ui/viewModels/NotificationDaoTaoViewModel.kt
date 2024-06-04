package com.dacs.vku.ui.viewModels

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dacs.vku.models.Notification
import com.dacs.vku.repository.NotificationRepository
import com.dacs.vku.util.Resources
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import kotlin.reflect.KMutableProperty0

class NotificationDaoTaoViewModel(
    app: Application,
    private val notificationRepository: NotificationRepository,
) : AndroidViewModel(app) {


    val notificationDaotao: MutableLiveData<Resources<MutableList<Notification>>> =
        MutableLiveData()
    var notificationDaoTaoPage = 1
    var notificationDaoTaoResponse: MutableList<Notification>? = null

    val notificationCTSV: MutableLiveData<Resources<MutableList<Notification>>> = MutableLiveData()
    var notificationCTSVPage = 1
    var notificationCTSVResponse: MutableList<Notification>? = null

    val notificationKHTC: MutableLiveData<Resources<MutableList<Notification>>> = MutableLiveData()
    var notificationKHTCPage = 1
    var notificationKHTCResponse: MutableList<Notification>? = null

    val notificationKTDBCL: MutableLiveData<Resources<MutableList<Notification>>> =
        MutableLiveData()
    var notificationKTDBCLPage = 1
    var notificationKTDBCLResponse: MutableList<Notification>? = null

    val searchNotification: MutableLiveData<Resources<MutableList<Notification>>> =
        MutableLiveData()
    var searchNotificationPage = 1
    var searchNotificationResponse: MutableList<Notification>? = null
    var notificationSearchQuery: String? = null
    var oldSearchQuery: String? = null
    var newSearchQuery: String? = null

    init {
        getNotificationDaoTao()
        getNotificationCTSV()
        getNotificationKTDBCL()
        getNotificationKHTC()

    }

    // Dao Tao Notifications
    fun getNotificationDaoTao() = viewModelScope.launch {
        notificationDaoTaoInternet()
    }

    suspend fun notificationDaoTaoInternet() {
        notificationDaotao.postValue(Resources.Loading())
        try {
            if (internetConnection(this.getApplication())) {
                val response = notificationRepository.getNotificationDaoTao()
                notificationDaotao.postValue(
                    handleNotificationResponse(
                        response,
                        ::notificationDaoTaoPage,
                        ::notificationDaoTaoResponse
                    )
                )
            } else {
                notificationDaotao.postValue(Resources.Error("No Internet Connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> notificationDaotao.postValue(Resources.Error("Unable to connect"))
                else -> notificationDaotao.postValue(Resources.Error("Unknown error"))
            }
        }
    }

    // CTSV Notifications
    fun getNotificationCTSV() = viewModelScope.launch {
        notificationCTSVInternet()
    }

    suspend fun notificationCTSVInternet() {
        notificationCTSV.postValue(Resources.Loading())
        try {
            if (internetConnection(this.getApplication())) {
                val response = notificationRepository.getNotificationCTSV()
                notificationCTSV.postValue(
                    handleNotificationResponse(
                        response,
                        ::notificationCTSVPage,
                        ::notificationCTSVResponse
                    )
                )
            } else {
                notificationCTSV.postValue(Resources.Error("No Internet Connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> notificationCTSV.postValue(Resources.Error("Unable to connect"))
                else -> notificationCTSV.postValue(Resources.Error("Unknown error"))
            }
        }
    }

    // KHTC Notifications
    fun getNotificationKHTC() = viewModelScope.launch {
        notificationKHTCInternet()
    }

    suspend fun notificationKHTCInternet() {
        notificationKHTC.postValue(Resources.Loading())
        try {
            if (internetConnection(this.getApplication())) {
                val response = notificationRepository.getNotificationKHTC()
                notificationKHTC.postValue(
                    handleNotificationResponse(
                        response,
                        ::notificationKHTCPage,
                        ::notificationKHTCResponse
                    )
                )
            } else {
                notificationKHTC.postValue(Resources.Error("No Internet Connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> notificationKHTC.postValue(Resources.Error("Unable to connect"))
                else -> notificationKHTC.postValue(Resources.Error("Unknown error"))
            }
        }
    }

    // KTDBCL Notifications
    fun getNotificationKTDBCL() = viewModelScope.launch {
        notificationKTDBCLInternet()
    }

    suspend fun notificationKTDBCLInternet() {
        notificationKTDBCL.postValue(Resources.Loading())
        try {
            if (internetConnection(this.getApplication())) {
                val response = notificationRepository.getNotificationKTDBCL()
                notificationKTDBCL.postValue(
                    handleNotificationResponse(
                        response,
                        ::notificationKTDBCLPage,
                        ::notificationKTDBCLResponse
                    )
                )
            } else {
                notificationKTDBCL.postValue(Resources.Error("No Internet Connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> notificationKTDBCL.postValue(Resources.Error("Unable to connect"))
                else -> notificationKTDBCL.postValue(Resources.Error("Unknown error"))
            }
        }
    }
    private var searchJob: Job? = null // Track the current search job to cancel if a new search is triggered

    // Search Notifications
    fun searchNotification(searchQuery: String) {
        if (searchQuery.isBlank()) {
            // Ignore empty search queries
            return
        }

        // Cancel the previous search job if it's still running
        searchJob?.cancel()

        // Start a new search job
        searchJob = viewModelScope.launch {
            searchNotificationDaoTaoInternet(searchQuery)
        }
    }
    suspend fun searchNotificationDaoTaoInternet(searchQuery: String) {
        newSearchQuery = searchQuery
        searchNotification.postValue(Resources.Loading())

        try {
            if (internetConnection((this.getApplication()))) {
                val response = notificationRepository.searchNotification(searchQuery)
                searchNotification.postValue(handleSearchNotification(response))
            } else {
                searchNotification.postValue(Resources.Error("No Internet"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> searchNotification.postValue(Resources.Error("Unable To Connect"))
                else -> searchNotification.postValue(Resources.Error("Unknown error"))
            }
        }
    }

    private fun handleSearchNotification(response: Response<MutableList<Notification>>): Resources<MutableList<Notification>> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                if (searchNotificationResponse == null || newSearchQuery != oldSearchQuery) {
                    searchNotificationPage = 1
                    oldSearchQuery = newSearchQuery
                    searchNotificationResponse = resultResponse
                } else {
                    searchNotificationPage++
                    searchNotificationResponse?.addAll(resultResponse)
                }
                return Resources.Success(searchNotificationResponse ?: resultResponse)
            }
        }
        return Resources.Error(response.message())
    }

    // Insert or update a notification in the local database
    fun addToSaved(notification: Notification) = viewModelScope.launch {
        notificationRepository.upsert(notification)
    }

    // Get all saved notifications from the local database
    fun getSaved() = notificationRepository.getSavedNotification()

    // Delete a notification from the local database
    fun deleteSaved(notification: Notification) = viewModelScope.launch {
        notificationRepository.deleteNotification(notification)
    }

    // Check for internet connection
    fun internetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
            when {
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } ?: false
    }
    private fun handleNotificationResponse(
        response: Response<MutableList<Notification>>,
        page: KMutableProperty0<Int>,
        responseList: KMutableProperty0<MutableList<Notification>?>
    ): Resources<MutableList<Notification>> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                page.set(page.get() + 1)
                if (responseList.get() == null) {
                    responseList.set(resultResponse)
                } else {
                    responseList.get()?.addAll(resultResponse)
                }
                return Resources.Success(responseList.get() ?: resultResponse)
            }
        }
        return Resources.Error(response.message())
    }


}

