package com.dacs.vku.ui.fragments.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.dacs.vku.R
import com.dacs.vku.databinding.FragmentViewNoteDialogBinding
import com.dacs.vku.models.Notification

class ViewNoteDialogFragment(
    private val notification: Notification
) : DialogFragment() {

    private lateinit var binding: FragmentViewNoteDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewNoteDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvNote.text = notification.note

        binding.btnViewInWeb.setOnClickListener {
            val notificationUrl = "https://daotao.vku.udn.vn/${notification.href}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notificationUrl))
            startActivity(browserIntent)
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}