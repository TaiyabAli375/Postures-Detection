 package com.example.posturesdetection

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.pose.Pose

 class MainActivity : AppCompatActivity() {
     var cameraActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
         ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
             if (it.resultCode == RESULT_OK) {
                 imageView.setImageURI(image_Uri)
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
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         enableEdgeToEdge()
         setContentView(R.layout.activity_main)
         ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
             val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
             v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
             insets
         }
         val repository = PostureRepository()
         val viewModelFactory = PosturesViewModelFactory(repository)
         viewModel = ViewModelProvider(this,viewModelFactory).get(PosturesViewModel::class.java)

         imageView = findViewById(R.id.imageView)
         cameraBtn = findViewById(R.id.cameraBtn)
         sidePoseBtn = findViewById(R.id.sidePoseAnalysisBtn)
         resultTv = findViewById(R.id.resultTv)

         checkAndRequestpermission()
         cameraBtn.setOnClickListener {
             if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                 openCamera()
             } else
                 requestPermissions(arrayOf(Manifest.permission.CAMERA),2111)
         }
         observePostures()
     }
     fun checkAndRequestpermission(){
         val permissionToRequest = mutableListOf<String>()
         if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
             permissionToRequest.add(Manifest.permission.CAMERA)

         if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU) {
             if (ContextCompat.checkSelfPermission(
                     this,
                     Manifest.permission.READ_MEDIA_IMAGES
                 ) != PackageManager.PERMISSION_GRANTED
             )
                 permissionToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)

         } else{
             if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
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
         image_Uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
         val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
         cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_Uri)
         cameraActivityResultLauncher.launch(cameraIntent)
     }
fun observePostures() {
    viewModel.bitmapWithSkeleton.observe(this, Observer{
        imageView.setImageBitmap(it)
    })
    viewModel.postures.observe(this) { posture ->

        val status = StringBuilder()
        Log.d("Is Sitting","${posture.isSitting}")
        if (posture.isSitting) {
            status.append("Person is sitting with ")
        }
        else if (posture.isStanding) {
            status.append("Person is standing with ")
        }

        when (posture.neckTilt) {
            NeckPosture.TILTED_LEFT ->
                status.append("neck tilted left")

            NeckPosture.TILTED_RIGHT ->
                status.append("neck tilted right")

            NeckPosture.STRAIGHT ->
                status.append("neck straight")

            else -> status.append("Unknown neck posture")
        }

        resultTv.text =
            if (status.isNotEmpty()) status.toString()
            else "Unknown"
    }
}
     fun uriToBitmap(uri: Uri): Bitmap{
         val imageBmp = MediaStore.Images.Media.getBitmap(contentResolver, uri)
         return imageBmp
     }
 }