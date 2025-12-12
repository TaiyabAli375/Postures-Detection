 package com.example.posturesdetection

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

 class MainActivity : AppCompatActivity() {
    var cameraActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(), ActivityResultCallback{
            if(it.resultCode== RESULT_OK){
                imageView.setImageURI(image_Uri)
                val uri: Uri = image_Uri ?: Uri.EMPTY
                detectPose(uri)
            }
        }
    )
    private lateinit var imageView: ImageView
    private lateinit var cameraBtn: Button
    private lateinit var resultTv: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        imageView = findViewById(R.id.imageView)
        cameraBtn = findViewById(R.id.cameraBtn)
        resultTv = findViewById(R.id.resultTv)

        checkAndRequestpermission()
        cameraBtn.setOnClickListener {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                openCamera()
            } else
                requestPermissions(arrayOf(Manifest.permission.CAMERA),2111)
        }
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
    fun detectPose(uri:Uri){
        val imageBmp = uriToBitmap(uri)
        val image = InputImage.fromBitmap(imageBmp,0)
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
        val poseDetector = PoseDetection.getClient(options)

        poseDetector.process(image)
            .addOnSuccessListener { result ->
                detedctSitting(result)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
     fun uriToBitmap(uri: Uri): Bitmap{
         val imageBmp = MediaStore.Images.Media.getBitmap(contentResolver, uri)
         return imageBmp
     }
     fun detedctSitting(pose: Pose): Boolean{

     }
     fun getAngle(a: PointF3D, b: PointF3D, c: PointF3D): Double {

         val ab = doubleArrayOf(
             a.x.toDouble() - b.x.toDouble(),
             a.y.toDouble() - b.y.toDouble()
         )
         val cb = doubleArrayOf(
             c.x.toDouble() - b.x.toDouble(),
             c.y.toDouble() - b.y.toDouble()
         )

         val dot = ab[0] * cb[0] + ab[1] * cb[1]
         val magAB = Math.sqrt(ab[0]*ab[0] + ab[1]*ab[1])
         val magCB = Math.sqrt(cb[0]*cb[0] + cb[1]*cb[1])

         val cosine = dot / (magAB * magCB)
         return Math.toDegrees(Math.acos(cosine))
     }

 }