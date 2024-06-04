package com.dacs.vku.ui.viewModels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dacs.vku.repository.NotificationRepository

class NotificationDaoTaoViewModelproviderFactory(
    val app: Application,
    private val notificationRepository: NotificationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotificationDaoTaoViewModel(app, notificationRepository) as T
    }
}