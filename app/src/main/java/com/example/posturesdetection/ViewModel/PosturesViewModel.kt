package com.example.posturesdetection.ViewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.posturesdetection.Model.PostureRepository
import com.example.posturesdetection.Model.Postures
import com.example.posturesdetection.View.Joint

class PosturesViewModel(private  val repository: PostureRepository): ViewModel() {
    private val mutablePostures = MutableLiveData<Postures>()
    val postures: LiveData<Postures> = mutablePostures
//    private val mutableBitmapWithSkeleton = MutableLiveData<Bitmap>()
//    val bitmapWithSkeleton: LiveData<Bitmap> = mutableBitmapWithSkeleton
    private val mutableImageBmp = MutableLiveData<Bitmap>()
    val imageBmp: LiveData<Bitmap> = mutableImageBmp
    private val mutableLandmarkForOverlay = MutableLiveData<List<Joint>>()
    val landmarkForOverlay: LiveData<List<Joint>> = mutableLandmarkForOverlay


    fun processImage(imageBmp: Bitmap){
        repository.detectPose(imageBmp){postures, joints ->
            mutablePostures.postValue(postures)
//            mutableBitmapWithSkeleton.postValue(bitmapWithSkeleton)
            mutableLandmarkForOverlay.postValue(joints)
        }
        mutableImageBmp.postValue(imageBmp)
    }
}