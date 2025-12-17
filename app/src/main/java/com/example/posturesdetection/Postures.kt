package com.example.posturesdetection

data class Postures(
    var isSitting: Boolean,
    var isStanding: Boolean,
    var neckTilt: NeckPosture)
