package com.dacs.vku.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.adapters.NotificationAdapter
import com.dacs.vku.databinding.FragmentDaoTaoBinding
import com.dacs.vku.databinding.FragmentSearchBinding
import com.dacs.vku.ui.VkuActivity
import com.dacs.vku.ui.viewModels.NotificationDaoTaoViewModel
import com.dacs.vku.util.Constants.Companion.SEARCH_NEWS_TIME_DELAY
import com.dacs.vku.util.Resources
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SearchFragment : Fragment(R.layout.fragment_search) {

    lateinit var notificationDaoTaoViewModel: NotificationDaoTaoViewModel
    lateinit var notificationAdapter: NotificationAdapter
    lateinit var retryButton: Button
    lateinit var errorText: TextView
    lateinit var itemNotificationDaoTaoError: CardView
    lateinit var binding: FragmentSearchBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSearchBinding.bind(view)

        itemNotificationDaoTaoError = view.findViewById(R.id.itemSearchError)
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.item_error, null)

        retryButton = view.findViewById(R.id.retryButton)
        errorText = view.findViewById(R.id.errorText)

        notificationDaoTaoViewModel = (activity as VkuActivity).notificationViewModel
        setupSearchRecycler()

        notificationAdapter.setOnItemClickListener { notification ->
            val bundle = Bundle().apply {
                putSerializable("notification", notification)
            }
            try {
                findNavController().navigate(R.id.action_searchFragment_to_articleFragment, bundle)
            } catch (e: IllegalArgumentException) {
                // Handle the case where navigation fails due to missing action or destination
                Log.e("NavigationError", "Navigation failed: ${e.message}")
            }

        }

        var job: Job? = null
        binding.searchEdit.addTextChangedListener() {

                editable ->
            job?.cancel()
            job = MainScope().launch{
                editable?.let {
                    if (editable.toString().isNotEmpty()) {
                        notificationDaoTaoViewModel.searchNotification(editable.toString())

                    }
                }
            }
        }
        notificationDaoTaoViewModel.searchNotification.observe(viewLifecycleOwner, Observer { response ->
            when(response) {
                is Resources.Error<*> -> {
                    hideProgressBar()
                    response.message?.let {
                            message-> Toast.makeText(activity, "Sorry error: $message", Toast.LENGTH_LONG).show()
                        showErrorMessage(message)
                    }
                }
                is Resources.Loading<*> -> {
                    showProgressBar()
                }
                is Resources.Success<*> -> {
                    hideErrorMessage()
                    hideProgressBar()
                    response.data?.let{
                            notificationResponse ->
                        notificationAdapter.differ.submitList(notificationResponse)

                    }
                }
            }
        })
        retryButton.setOnClickListener{
            if (binding.searchEdit.text.toString().isNotEmpty()) {
                notificationDaoTaoViewModel.searchNotification(binding.searchEdit.text.toString())

            } else {
                hideErrorMessage()
            }
        }

    }


        var isError = false
        var isLoading = false
        var isLastPage = false
        var isScolling = false

        fun hideProgressBar() {
            binding.paginationProgressBar.visibility = View.INVISIBLE
            isLoading = false
        }

        fun showProgressBar() {
            binding.paginationProgressBar.visibility = View.INVISIBLE
            isLoading = true
        }

        fun hideErrorMessage() {
            itemNotificationDaoTaoError.visibility = View.INVISIBLE
            isError = false
        }

        fun showErrorMessage(message: String) {
            itemNotificationDaoTaoError.visibility = View.INVISIBLE
            errorText.text = message
            isError = false

        }

        var scollListener = object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisableItemPosition = layoutManager.findFirstVisibleItemPosition()
                val visableItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount

                val isNoErrors = !isError
                val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
                val isAtLastItem = firstVisableItemPosition + visableItemCount >= totalItemCount
                val isNotAtBeginning = firstVisableItemPosition >= 0
                val shouldPaginate =
                    isNoErrors && isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning && isScolling
                if (shouldPaginate) {
                    notificationDaoTaoViewModel.searchNotification(binding.searchEdit.text.toString())
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

        fun setupSearchRecycler() {
            notificationAdapter = NotificationAdapter()
            binding.recyclerSearch.apply {
                adapter = notificationAdapter
                layoutManager = LinearLayoutManager(activity)
                addOnScrollListener(this@SearchFragment.scollListener)
            }
        }

    }
