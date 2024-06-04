package com.dacs.vku.ui.fragments.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AbsListView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.adapters.NotificationAdapter
import com.dacs.vku.databinding.FragmentKtdbclBinding
import com.dacs.vku.ui.VkuActivity
import com.dacs.vku.ui.viewModels.NotificationDaoTaoViewModel
import com.dacs.vku.util.Resources

class KTDBCL : Fragment(R.layout.fragment_ktdbcl) {

    lateinit var notificationDaoTaoViewModel: NotificationDaoTaoViewModel
    lateinit var notificationAdapter: NotificationAdapter
    lateinit var retryButton: Button
    lateinit var errorText: TextView
    lateinit var itemNotificationDaoTaoError: CardView
    lateinit var binding: FragmentKtdbclBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentKtdbclBinding.bind(view)

        itemNotificationDaoTaoError = view.findViewById(R.id.itemNotificationError)
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.item_error, null)

        retryButton = view.findViewById(R.id.retryButton)
        errorText = view.findViewById(R.id.errorText)

        notificationDaoTaoViewModel = (activity as VkuActivity).notificationViewModel
        setupNotificationKTDBCLRecycler()

        notificationAdapter.setOnItemClickListener { notification ->
            val bundle = Bundle().apply {
                putSerializable("notification", notification)
            }
            try {
                findNavController().navigate(R.id.action_daotaoFragment_to_articleFragment, bundle)
            } catch (e: IllegalArgumentException) {
                Log.e("NavigationError", "Navigation failed: ${e.message}")
            }
        }

        notificationDaoTaoViewModel.notificationKTDBCL.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resources.Error<*> -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        Toast.makeText(activity, "Sorry error: $message", Toast.LENGTH_LONG).show()
                        showErrorMessage(message)
                    }
                }
                is Resources.Loading<*> -> {
                    showProgressBar()
                }
                is Resources.Success<*> -> {
                    hideErrorMessage()
                    hideProgressBar()
                    response.data?.let { notificationResponse ->
                        notificationAdapter.differ.submitList(notificationResponse)
                    }
                }
            }
        })
    }

    var isError = false
    var isLoading = false
    var isLastPage = false
    var isScolling = false

    private fun hideProgressBar() {
        binding.paginationProgressBar.visibility = View.INVISIBLE
        isLoading = false
    }

    private fun showProgressBar() {
        binding.paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    private fun hideErrorMessage() {
        itemNotificationDaoTaoError.visibility = View.INVISIBLE
        isError = false
    }

    private fun showErrorMessage(message: String) {
        itemNotificationDaoTaoError.visibility = View.VISIBLE
        errorText.text = message
        isError = true
    }

    var scollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNoErrors = !isError
            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val shouldPaginate =
                isNoErrors && isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning && isScolling
            if (shouldPaginate) {
                notificationDaoTaoViewModel.getNotificationKHTC()
                isScolling = false
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                isScolling = true
            }
        }
    }

    private fun setupNotificationKTDBCLRecycler() {
        notificationAdapter = NotificationAdapter()
        binding.recycleKTDBCL.apply {
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@KTDBCL.scollListener)
        }
    }

}