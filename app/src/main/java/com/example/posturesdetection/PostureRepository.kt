package com.example.posturesdetection

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

class PostureRepository() {
    private lateinit var poseDetector: PoseDetector
    private var leftShoulder: PointF3D? = null
    private var rightShoulder: PointF3D? = null
    private var leftHip: PointF3D? = null
    private var rightHip: PointF3D? = null
    private var leftKnee: PointF3D? = null
    private var rightKnee: PointF3D? = null
    private var leftAnkle: PointF3D? = null
    private var rightAnkle: PointF3D? = null


    init {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)
    }
    fun detectPose(imageBmp: Bitmap, onResult: (Postures)->Unit){
        val image = InputImage.fromBitmap(imageBmp,0)
        poseDetector.process(image)
            .addOnSuccessListener { pose ->
                leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position3D
                rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D
                leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position3D
                rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position3D
                leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.position3D
                rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.position3D
                leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.position3D
                rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.position3D
                val postures = Postures(
                    isSitting = detectSitting(),
                    isStanding = detectStanding()
                )
                onResult(postures)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
    fun detectSitting(): Boolean{
        val leftHipAngle = getAngle(leftShoulder,leftHip,leftKnee)
        val rightHipAngle = getAngle(rightShoulder,rightHip,rightKnee)
        val leftKneeAngle = getAngle(leftHip,leftKnee,leftAnkle)
        val rightKneeAngle = getAngle(rightHip,rightKnee,rightAnkle)
        Log.d("Right Knee Angle","value: $rightKneeAngle")
        Log.d("Left Knee Angle","value: $leftKneeAngle")

        val isKneeBent =
            (leftKneeAngle in 50.0..110.0) ||
                    (rightKneeAngle in 50.0..110.0)

        val isHipBent =
            (leftHipAngle in 40.0..110.0) &&
                    (rightHipAngle in 40.0..110.0)

        return isKneeBent && isHipBent
    }
    fun detectStanding(): Boolean{
        val leftHipAngle = getAngle(leftShoulder,leftHip,leftKnee)
        val rightHipAngle = getAngle(rightShoulder,rightHip,rightKnee)
        val leftKneeAngle = getAngle(leftHip,leftKnee,leftAnkle)
        val rightKneeAngle = getAngle(rightHip,rightKnee,rightAnkle)
        Log.d("Right Hip Angle","value: $rightHipAngle")
        Log.d("Left Hip Angle","value: $leftHipAngle")

        val isKneeNotBent =
            (leftKneeAngle in 120.0..190.0) ||
                    (rightKneeAngle in 120.0..190.0)

        val isHipNotBent =
            (leftHipAngle in 160.0..190.0) &&
                    (rightHipAngle in 160.0..190.0)

        return isKneeNotBent && isHipNotBent
    }
    fun getAngle(a: PointF3D?, b: PointF3D?, c: PointF3D?): Double {
        if (a == null || b == null || c == null) return 0.0
        val ab = doubleArrayOf(
            a.x.toDouble() - b.x.toDouble(),
            a.y.toDouble() - b.y.toDouble(),
            a.z.toDouble() - b.z.toDouble()
        )
        val cb = doubleArrayOf(
            c.x.toDouble() - b.x.toDouble(),
            c.y.toDouble() - b.y.toDouble(),
            c.z.toDouble() - b.z.toDouble()
        )

        val dot = ab[0] * cb[0] + ab[1] * cb[1] + ab[2] * cb[2]
        val magAB = Math.sqrt(ab[0]*ab[0] + ab[1]*ab[1] + ab[2]*ab[2])
        val magCB = Math.sqrt(cb[0]*cb[0] + cb[1]*cb[1] + cb[2]*cb[2])

        val cosine = dot / (magAB * magCB)
        return Math.toDegrees(Math.acos(cosine))
    }
}