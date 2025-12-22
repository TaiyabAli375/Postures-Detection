package com.example.posturesdetection.View

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.posturesdetection.Model.PostureRepository
import com.example.posturesdetection.ViewModel.PosturesViewModel
import com.example.posturesdetection.ViewModel.PosturesViewModelFactory
import com.example.posturesdetection.R

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: PosturesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val repository = PostureRepository()
        val viewModelFactory = PosturesViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(PosturesViewModel::class.java)
    }
}