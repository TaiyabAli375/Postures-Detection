package com.example.posturesdetection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PosturesViewModelFactory(private val repository: PostureRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosturesViewModel::class.java)) {
            return PosturesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}