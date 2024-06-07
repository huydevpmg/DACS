package com.dacs.vku.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dacs.vku.R
import com.dacs.vku.databinding.ActivityVkuBinding
import com.dacs.vku.db.NotificationDatabase
import com.dacs.vku.repository.NotificationRepository
import com.dacs.vku.ui.viewModels.NotificationDaoTaoViewModel
import com.dacs.vku.ui.viewModels.NotificationDaoTaoViewModelproviderFactory
import com.dacs.vku.util.Constants.Companion.CREATE_FILE

class VkuActivity : AppCompatActivity() {
    lateinit var notificationViewModel: NotificationDaoTaoViewModel
    lateinit var binding: ActivityVkuBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(1000)
        installSplashScreen()

        binding = ActivityVkuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val notificationRepository = NotificationRepository(NotificationDatabase(this))
        val viewModelproviderFactory= NotificationDaoTaoViewModelproviderFactory(application, notificationRepository)

        notificationViewModel = ViewModelProvider(this, viewModelproviderFactory)[NotificationDaoTaoViewModel::class.java]

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.notificationNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)
    }


}