package com.example.posturesdetection

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PosturesViewModel(private  val repository: PostureRepository): ViewModel() {
    private val mutablePostures = MutableLiveData<Postures>()
    val postures: LiveData<Postures> = mutablePostures
    fun processImage(imageBmp: Bitmap){
        repository.detectPose(imageBmp){results ->
            mutablePostures.postValue(results)
        }
    }
}