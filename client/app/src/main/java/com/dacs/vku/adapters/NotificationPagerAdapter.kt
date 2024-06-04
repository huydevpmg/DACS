package com.dacs.vku.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dacs.vku.ui.fragments.main.CTSV
import com.dacs.vku.ui.fragments.main.DaoTaoFragment
import com.dacs.vku.ui.fragments.main.KHTC
import com.dacs.vku.ui.fragments.main.KTDBCL

class NotificationPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragmentList = listOf(
        DaoTaoFragment(),
        CTSV(),
        KHTC(),
        KTDBCL()
    )

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]
}
