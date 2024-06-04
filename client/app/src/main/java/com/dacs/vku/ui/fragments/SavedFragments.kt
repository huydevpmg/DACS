package com.dacs.vku.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dacs.vku.R
import com.dacs.vku.adapters.NotificationAdapter
import com.dacs.vku.databinding.FragmentSavedFragmentsBinding
import com.dacs.vku.ui.VkuActivity
import com.dacs.vku.ui.viewModels.NotificationDaoTaoViewModel
import com.google.android.material.snackbar.Snackbar

class SavedFragments : Fragment(R.layout.fragment_saved_fragments) {
    lateinit var notificationDaoTaoViewModel: NotificationDaoTaoViewModel
    lateinit var notificationAdapter: NotificationAdapter
    lateinit var binding: FragmentSavedFragmentsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSavedFragmentsBinding.bind(view)

        notificationDaoTaoViewModel = (activity as VkuActivity).notificationViewModel
        setupSavedRecycler()

        notificationAdapter.setOnItemClickListener { notification ->
            val bundle = Bundle().apply {
                putSerializable("notification", notification)
            }
            try {
                findNavController().navigate(R.id.action_savedFragment_to_articleFragment, bundle)
            } catch (e: IllegalArgumentException) {
                // Handle the case where navigation fails due to missing action or destination
                Log.e("NavigationError", "Navigation failed: ${e.message}")
            }

        }
        val itemTouchHelperCallBack = object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT or  ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notification = notificationAdapter.differ.currentList[position]
                notificationDaoTaoViewModel.deleteSaved(notification)
                Snackbar.make(view, "Remove from Saved", Snackbar.LENGTH_LONG).apply {
                    setAction("Undo"){
                        notificationDaoTaoViewModel.addToSaved(notification)
                    }
                }.show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallBack).apply {
            attachToRecyclerView(binding.recycleSaved)
        }
        notificationDaoTaoViewModel.getSaved().observe(viewLifecycleOwner, Observer { notification->
            notificationAdapter.differ.submitList(notification)
        })
    }
    private fun setupSavedRecycler(){
        notificationAdapter = NotificationAdapter()
        binding.recycleSaved.apply {
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(activity)
        }    }

}