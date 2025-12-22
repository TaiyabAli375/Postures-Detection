package com.example.posturesdetection.View

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

data class Joint(val name: String, var x: Float, var y: Float)

class SidePoseOverlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val jointPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val linePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLUE
        textSize = 37f
        isAntiAlias = true
    }
    private val joints = mutableListOf<Joint>()

    private var imageWidth = 1f
    private var imageHeight = 1f

    private var selectedJoint: Joint? = null
    private val touchRadius = 50f

    fun setImageSize(w: Float, h: Float) {
        imageWidth = max(1f, w)
        imageHeight = max(1f, h)
    }

    fun setJoints(jointList: List<Joint>) {
        joints.clear()
        joints.addAll(jointList)
        invalidate()
    }

    private fun scalex(x: Float): Float = x * width / imageWidth
    private fun scaley(y: Float): Float = y * height / imageHeight

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (joints.size < 5) return

        val head = joints[0]
        val shoulder = joints[1]
        val hip = joints[2]
        val knee = joints[3]
        val ankle = joints[4]

        // Draw bones
        canvas.drawLine(
            scalex(head.x), scaley(head.y),
            scalex(shoulder.x), scaley(shoulder.y),
            linePaint
        )
        canvas.drawLine(
            scalex(shoulder.x), scaley(shoulder.y),
            scalex(hip.x), scaley(hip.y),
            linePaint
        )
        canvas.drawLine(
            scalex(hip.x), scaley(hip.y),
            scalex(knee.x), scaley(knee.y),
            linePaint
        )
        canvas.drawLine(
            scalex(knee.x), scaley(knee.y),
            scalex(ankle.x), scaley(ankle.y),
            linePaint
        )

        for (i in 0..4) {
            val joint = joints[i]
            canvas.drawCircle(
                scalex(joint.x),
                scaley(joint.y),
                18f,
                jointPaint
            )
        }

        val angleShoulder = calculateAngle(
            PointF(scalex(head.x), scaley(head.y)),
            PointF(scalex(shoulder.x), scaley(shoulder.y)),
            PointF(scalex(hip.x), scaley(hip.y))
        )
        canvas.drawText(
            ("${joints[1].name} " + "%.1f°").format(angleShoulder),
            scalex(shoulder.x) + 20,
            scaley(shoulder.y) - 20,
            textPaint
        )
       // --------------------------------
        val angleHip = calculateAngle(
            PointF(scalex(shoulder.x), scaley(shoulder.y)),
            PointF(scalex(hip.x), scaley(hip.y)),
            PointF(scalex(knee.x), scaley(knee.y))
        )
        canvas.drawText(
            ("${joints[2].name} " + "%.1f°").format(angleHip),
            scalex(hip.x) + 20,
            scaley(hip.y) - 20,
            textPaint
        )
       // ----------------------------------
        val angleKnee = calculateAngle(
            PointF(scalex(hip.x), scaley(hip.y)),
            PointF(scalex(knee.x), scaley(knee.y)),
            PointF(scalex(ankle.x), scaley(ankle.y))
        )
        canvas.drawText(
            ("${joints[3].name} " + "%.1f°").format(angleKnee),
            scalex(knee.x) + 20,
            scaley(knee.y) - 20,
            textPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedJoint = findTouchedJoint(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> {
                selectedJoint?.let {
                    it.x = (event.x * imageWidth / width)
                        .coerceIn(0f, imageWidth)
                    it.y = (event.y * imageHeight / height)
                        .coerceIn(0f, imageHeight)
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                selectedJoint = null
            }
        }
        return true
    }

    private fun findTouchedJoint(x: Float, y: Float): Joint? {
        return joints.find {
            hypot(scalex(it.x) - x, scaley(it.y) - y) <= touchRadius
        }
    }

    private fun calculateAngle(a: PointF, b: PointF, c: PointF): Double {
        val abX = a.x - b.x
        val abY = a.y - b.y
        val cbX = c.x - b.x
        val cbY = c.y - b.y
        
        val dot = abX * cbX + abY * cbY
        val magAB = sqrt(abX * abX + abY * abY)
        val magCB = sqrt(cbX * cbX + cbY * cbY)

        if (magAB == 0f || magCB == 0f) return 0.0

        val cos = (dot / (magAB * magCB)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos.toDouble()))
    }
}