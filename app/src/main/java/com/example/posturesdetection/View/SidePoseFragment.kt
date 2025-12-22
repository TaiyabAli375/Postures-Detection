package com.example.posturesdetection.View

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.posturesdetection.ViewModel.PosturesViewModel
import com.example.posturesdetection.R

class SidePoseFragment : Fragment() {
    private lateinit var viewModel: PosturesViewModel
    private lateinit var imageView: ImageView

    private lateinit var sidePoseOverlay: SidePoseOverlayView
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
        sidePoseOverlay = view.findViewById(R.id.sidePoseOverlay)

        observePostures()
    }
    fun observePostures() {
        viewModel.imageBmp.observe(viewLifecycleOwner, Observer {
            imageView.setImageBitmap(it)
        })
        viewModel.landmarkForOverlay.observe(viewLifecycleOwner) { joints ->
            val drawable = imageView.drawable ?: return@observe

            sidePoseOverlay.setImageSize(
                drawable.intrinsicWidth.toFloat(),
                drawable.intrinsicHeight.toFloat()
            )
            sidePoseOverlay.setJoints(joints)
        }
    }
}