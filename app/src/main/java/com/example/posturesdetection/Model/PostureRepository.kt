package com.example.posturesdetection.Model

import android.graphics.Bitmap
import android.util.Log
import com.example.posturesdetection.Model.Postures
import com.example.posturesdetection.View.Joint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlin.math.abs

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
    fun detectPose(imageBmp: Bitmap, onResult: (Postures, List<Joint>)->Unit){
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
                    isStanding = detectStanding(),
                    neckTilt = detectNeckTilt(pose),
                    shoulderDrop = detectShoulderDrop(pose)
                )
//                val bitmapWithSkeleton = drawSkeletonOnBitmap(imageBmp,pose)
                val joints = landmarksForOverlay(pose)
                onResult(postures, joints)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
    //    Postures Detection Functions--------------------------------------------------------
    fun detectSitting(): Boolean{
        val leftHipAngle = getAngle(leftShoulder,leftHip,leftKnee)
        val rightHipAngle = getAngle(rightShoulder,rightHip,rightKnee)
        val leftKneeAngle = getAngle(leftHip,leftKnee,leftAnkle)
        val rightKneeAngle = getAngle(rightHip,rightKnee,rightAnkle)

        val isKneeBent =
            (leftKneeAngle in 25.0..110.0) ||
                    (rightKneeAngle in 25.0..110.0)

        val isHipBent =
            (leftHipAngle in 40.0..120.0) &&
                    (rightHipAngle in 40.0..120.0)

        return isKneeBent && isHipBent
    }
    fun detectStanding(): Boolean{
        val leftHipAngle = getAngle(leftShoulder,leftHip,leftKnee)
        val rightHipAngle = getAngle(rightShoulder,rightHip,rightKnee)
        val leftKneeAngle = getAngle(leftHip,leftKnee,leftAnkle)
        val rightKneeAngle = getAngle(rightHip,rightKnee,rightAnkle)

        val isKneeNotBent =
            (leftKneeAngle in 119.0..190.0) ||
                    (rightKneeAngle in 119.0..190.0)
        Log.d("LEFT KNEE ANGLE","$leftKneeAngle")
        Log.d("RIGHT KNEE ANGLE","$rightKneeAngle")

        val isHipNotBent =
            (leftHipAngle in 140.0..190.0) &&
                    (rightHipAngle in 140.0..190.0)
        Log.d("LEFT HIP ANGLE","$leftHipAngle")
        Log.d("RIGHT HIP ANGLE","$rightHipAngle")

        return isKneeNotBent && isHipNotBent
    }
    fun detectNeckTilt(pose: Pose): String {
        val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (leftEar == null || rightEar == null ||
            leftShoulder == null || rightShoulder == null
        ) {
            return "Unknown"
        }

        val headMidX = (leftEar.position.x + rightEar.position.x) / 2f
        val shoulderMidX = (leftShoulder.position.x + rightShoulder.position.x) / 2f

        val tiltThreshold = 10f

        return when {
            headMidX > shoulderMidX + tiltThreshold -> "Neck is tilted Right"
            headMidX < shoulderMidX - tiltThreshold -> "Neck is tilted Left"
            else -> "Neck is Straight"
        }
    }
    fun detectShoulderDrop(pose: Pose): String {
        val threshold: Float = 0.05f
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        if (leftShoulder == null || rightShoulder == null) return "Shoulders not detected"

        val diff = rightShoulder.position.y - leftShoulder.position.y

        return when {
            abs(diff) < threshold -> "Shoulders are level"
            diff > threshold -> "Right shoulder dropped"
            else -> "Left shoulder dropped"
        }
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
    //  function to send landmarks------------------------------------------------------------
    fun landmarksForOverlay(pose: Pose): List<Joint> {
        val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        if (leftEar == null ||
            leftShoulder == null ||
            leftElbow == null ||
            leftWrist == null ||
            leftHip == null ||
            leftKnee == null ||
            leftAnkle == null ||
            rightShoulder == null ||
            rightHip == null ||
            rightKnee == null ||
            rightAnkle == null ||
            rightElbow == null ||
            rightWrist == null
            ) {
            return emptyList()
        }

        return listOf(
            Joint("Head", leftEar.position.x, leftEar.position.y),
            Joint("Left Shoulder", leftShoulder.position.x, leftShoulder.position.y),
            Joint("Left Hip", leftHip.position.x, leftHip.position.y),
            Joint("Left Knee", leftKnee.position.x, leftKnee.position.y),
            Joint("Left Ankle", leftAnkle.position.x, leftAnkle.position.y),
            Joint("Right Shoulder", rightShoulder.position.x, rightShoulder.position.y),
            Joint("Right Hip", rightHip.position.x, rightHip.position.y),
            Joint("Right Knee", rightKnee.position.x, rightKnee.position.y),
            Joint("Right Ankle", rightAnkle.position.x, rightAnkle.position.y),
            Joint("Left Elbow", leftElbow.position.x, leftElbow.position.y),
            Joint("Left Wrist", leftWrist.position.x, leftWrist.position.y),
            Joint("Right Elbow", rightElbow.position.x, rightElbow.position.y),
            Joint("Right Wrist", rightWrist.position.x, rightWrist.position.y)
        )
    }
}