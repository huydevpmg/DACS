package com.dacs.vku.ui.fragments.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.dacs.vku.R
import com.dacs.vku.databinding.FragmentAddNoteDialogBinding
import com.dacs.vku.models.Notification

class AddNoteDialogFragment(
    private val notification: Notification,
    private val onNoteAdded: (String) -> Unit
) : DialogFragment() {

    private lateinit var binding: FragmentAddNoteDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddNoteDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSave.setOnClickListener {
            val note = binding.etNote.text.toString()
            if (note.isNotEmpty()) {
                onNoteAdded(note)
                dismiss()
            } else {
                binding.etNote.error = "Note cannot be empty"
            }
        }

        binding.btnCancel.setOnClickListener {
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