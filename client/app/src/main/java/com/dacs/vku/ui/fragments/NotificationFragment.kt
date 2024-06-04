package com.dacs.vku.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.dacs.vku.R
import com.dacs.vku.databinding.FragmentNotificationBinding
import com.dacs.vku.ui.VkuActivity
import com.dacs.vku.ui.viewModels.NotificationDaoTaoViewModel
import com.google.android.material.snackbar.Snackbar

class NotificationFragment : Fragment(R.layout.fragment_notification) {

    private lateinit var notificationDaoTaoViewModel: NotificationDaoTaoViewModel
    private val args: NotificationFragmentArgs by navArgs()
    private lateinit var binding: FragmentNotificationBinding
    private lateinit var webView: WebView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNotificationBinding.bind(view)

        notificationDaoTaoViewModel = (activity as VkuActivity).notificationViewModel
        val notification = args.notification

        binding.webView.apply {
            webViewClient = webViewClient
            val link = "https://daotao.vku.udn.vn/" + notification.href
            loadUrl(link)
        }

        binding.fab.setOnClickListener {
            notificationDaoTaoViewModel.addToSaved(notification)
            Snackbar.make(view, "Added to Saved!", Snackbar.LENGTH_SHORT).show()
        }

        binding.fab.setOnLongClickListener {
            val notificationUrl = "https://daotao.vku.udn.vn/${notification.href}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notificationUrl))
            startActivity(browserIntent)
            true // consume long click event
        }

    }
}
