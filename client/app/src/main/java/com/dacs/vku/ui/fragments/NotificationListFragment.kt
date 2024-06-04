package com.dacs.vku.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.dacs.vku.R
import com.dacs.vku.adapters.NotificationPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class NotificationListFragment : Fragment(R.layout.fragment_notification_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager: ViewPager2 = view.findViewById(R.id.view_pager)
        val tabLayout: TabLayout = view.findViewById(R.id.tab_layout)

        val pagerAdapter = NotificationPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // Thiết lập các tab
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Đào Tạo"
                1 -> "CTSV"
                2 -> "KHTC"
                3 -> "KTDBCL"
                else -> null
            }
        }.attach()
    }
}
