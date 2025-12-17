package com.example.posturesdetection

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.Observer

class SidePoseFragment : Fragment() {
    private lateinit var viewModel: PosturesViewModel
    private lateinit var imageView: ImageView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_side_pose, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (requireActivity() as MainActivity).viewModel
        imageView = view.findViewById(R.id.sidePoseAnalysisIV)

        observePostures()
    }
    fun observePostures() {
        viewModel.bmpForSidePose.observe(viewLifecycleOwner, Observer{
            imageView.setImageBitmap(it)
        })
    }
}