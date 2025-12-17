package com.example.posturesdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
enum class NeckPosture {
    STRAIGHT,
    TILTED_LEFT,
    TILTED_RIGHT,
    UNKNOWN
}
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
    fun detectPose(imageBmp: Bitmap, onResult: (Postures, Bitmap)->Unit){
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
                    neckTilt = detectNeckTilt(pose)
                )
                val bitmapWithSkeleton = drawSkeletonOnBitmap(imageBmp, pose)
                onResult(postures, bitmapWithSkeleton)
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
    fun detectNeckTilt(pose: Pose): NeckPosture {
        val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (leftEar == null || rightEar == null ||
            leftShoulder == null || rightShoulder == null
        ) {
            return NeckPosture.UNKNOWN
        }

        val headMidX = (leftEar.position.x + rightEar.position.x) / 2f
        val shoulderMidX = (leftShoulder.position.x + rightShoulder.position.x) / 2f

        val tiltThreshold = 20f

        return when {
            headMidX > shoulderMidX + tiltThreshold -> NeckPosture.TILTED_RIGHT
            headMidX < shoulderMidX - tiltThreshold -> NeckPosture.TILTED_LEFT
            else -> NeckPosture.STRAIGHT
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
    //   Skeleton Drawing Functions -----------------------------------------------------------
    fun drawSkeletonOnBitmap(imageBmp: Bitmap, pose:Pose): Bitmap{
        var mutableBmp = imageBmp.copy(Bitmap.Config.ARGB_8888,true)
        val canvas = Canvas(mutableBmp)
        var paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth =7f

        drawSkeleton(pose,canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_PINKY,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_THUMB,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_INDEX,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,paint)

        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_PINKY,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_THUMB,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_INDEX,paint)

        drawSkeleton(pose,canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.LEFT_HIP,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_HEEL,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_HEEL, PoseLandmark.LEFT_FOOT_INDEX,paint)
        drawSkeleton(pose,canvas, PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.LEFT_ANKLE,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_HEEL,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_HEEL, PoseLandmark.RIGHT_FOOT_INDEX,paint)
        drawSkeleton(pose,canvas, PoseLandmark.RIGHT_FOOT_INDEX, PoseLandmark.RIGHT_ANKLE,paint)
        return mutableBmp
    }
    fun drawSkeleton(pose: Pose,canvas: Canvas,startPoint:Int,endPoint:Int,paint: Paint){
        var startLandmark = pose.getPoseLandmark(startPoint)
        var endLandmark = pose.getPoseLandmark(endPoint)
        if(startLandmark!= null && endLandmark != null) {
            canvas.drawLine(
                startLandmark.position.x,
                startLandmark.position.y,
                endLandmark.position.x,
                endLandmark.position.y,
                paint
            )
            canvas.drawCircle(startLandmark.position.x, startLandmark.position.y, 15f, paint)
            canvas.drawCircle(endLandmark.position.x, endLandmark.position.y, 15f, paint)
        }
    }
}
