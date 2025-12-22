package com.example.posturesdetection.Model

data class Postures(
    var isSitting: Boolean,
    var isStanding: Boolean,
    var neckTilt: String,
    var shoulderDrop: String,
    var isVarusOrValgus: String,
    var hipHike: String
)