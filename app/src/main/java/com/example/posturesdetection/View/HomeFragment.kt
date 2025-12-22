package com.example.posturesdetection.View

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.posturesdetection.View.AnteriorOverlayView
import com.example.posturesdetection.ViewModel.PosturesViewModel
import com.example.posturesdetection.R

class HomeFragment : Fragment() {
    var cameraActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
            if (it.resultCode == Activity.RESULT_OK) {
//                imageView.setImageURI(image_Uri)
                val uri: Uri = image_Uri ?: Uri.EMPTY
                val imageBmp = uriToBitmap(uri)
                viewModel.processImage(imageBmp)
            }
        }
    )

    private lateinit var imageView: ImageView
    private lateinit var cameraBtn: Button
    private lateinit var sidePoseBtn: Button
    private lateinit var resultTv: TextView
    private lateinit var viewModel: PosturesViewModel
    private lateinit var anteriorOverlayView: AnteriorOverlayView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (requireActivity() as MainActivity).viewModel

        imageView = view.findViewById(R.id.anteriorViewIV)
        cameraBtn = view.findViewById(R.id.cameraBtn)
        sidePoseBtn = view.findViewById(R.id.sidePoseAnalysisBtn)
        resultTv = view.findViewById(R.id.resultTv)
        anteriorOverlayView = view.findViewById(R.id.anteriorViewOverlay)

        checkAndRequestpermission()
        cameraBtn.setOnClickListener {
            if(ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                openCamera()
            } else
                requestPermissions(arrayOf(Manifest.permission.CAMERA),2111)
        }
        observePostures()
        sidePoseBtn.setOnClickListener {
            Log.d("NAVIGATION", "SidePose button clicked")
                findNavController().navigate(R.id.action_homeFragment_to_sidePoseFragment)
        }
    }
    fun checkAndRequestpermission(){
        val permissionToRequest = mutableListOf<String>()
        if(ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissionToRequest.add(Manifest.permission.CAMERA)

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            )
                permissionToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)

        } else{
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissionToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        }
        if(permissionToRequest.isNotEmpty())
            requestPermissions(permissionToRequest.toTypedArray(),2111)

    }
    var image_Uri: Uri? = null
    fun openCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION,"From the camera")
        image_Uri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_Uri)
        cameraActivityResultLauncher.launch(cameraIntent)
    }
    fun observePostures() {
        viewModel.imageBmp.observe(viewLifecycleOwner, Observer {
            imageView.setImageBitmap(it)
        })
        viewModel.landmarkForOverlay.observe(viewLifecycleOwner) { joints ->
            val drawable = imageView.drawable ?: return@observe

            anteriorOverlayView.setImageSize(
                drawable.intrinsicWidth.toFloat(),
                drawable.intrinsicHeight.toFloat()
            )
            anteriorOverlayView.setJoints(joints)
        }

        viewModel.postures.observe(viewLifecycleOwner) { posture ->

            val status = StringBuilder()
            Log.d("Is Sitting","${posture.isSitting}")
            if (posture.isSitting) {
                status.append("1. Sitting\n")
            }
            else if (posture.isStanding) {
                status.append("1. Standing\n")
            }

            val neckTilt = posture.neckTilt
            status.append("2. $neckTilt\n")

            val shoulderDrop = posture.shoulderDrop
            status.append("3. $shoulderDrop\n")

            val isVarusOrValgus = posture.isVarusOrValgus
            status.append("4. $isVarusOrValgus\n")

            val hipHike = posture.hipHike
            status.append("5. $hipHike")

            resultTv.text = if (status.isNotEmpty()) status.toString()
                else "Unknown"
        }
    }
    fun uriToBitmap(uri: Uri): Bitmap {
        val imageBmp = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        return imageBmp
    }
}