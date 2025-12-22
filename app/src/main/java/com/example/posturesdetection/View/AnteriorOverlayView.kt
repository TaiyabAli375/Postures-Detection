package com.example.posturesdetection.View

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

class AnteriorOverlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
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

        if (joints.size < 13) return

        drawLineInJoints(joints[1],joints[2],canvas)
        drawLineInJoints(joints[2],joints[3],canvas)
        drawLineInJoints(joints[3],joints[4],canvas)
        drawLineInJoints(joints[5],joints[6],canvas)
        drawLineInJoints(joints[6],joints[7],canvas)
        drawLineInJoints(joints[7],joints[8],canvas)
        drawLineInJoints(joints[1],joints[5],canvas)
        drawLineInJoints(joints[2],joints[6],canvas)

        drawLineInJoints(joints[1],joints[9],canvas)
        drawLineInJoints(joints[9],joints[10],canvas)
        drawLineInJoints(joints[5],joints[11],canvas)
        drawLineInJoints(joints[11],joints[12],canvas)

        drawJoints(canvas)



        drawAngle(joints[1],joints[9],joints[10],canvas)
        drawAngle(joints[1],joints[2],joints[3],canvas)
        drawAngle(joints[2],joints[3],joints[4],canvas)
        drawAngle(joints[5],joints[11],joints[12],canvas)
        drawAngle(joints[5],joints[6],joints[7],canvas)
        drawAngle(joints[6],joints[7],joints[8],canvas)
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
    fun drawJoints(canvas: Canvas){
        val jointPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        for (joint in joints) {
            canvas.drawCircle(
                scalex(joint.x),
                scaley(joint.y),
                18f,
                jointPaint
            )
        }
    }
    fun drawLineInJoints(startPoint: Joint, endPoint: Joint, canvas: Canvas){
        val linePaint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawLine(
            scalex(startPoint.x), scaley(startPoint.y),
            scalex(endPoint.x), scaley(endPoint.y),
            linePaint
        )
    }
    fun drawAngle(a: Joint, b: Joint, c: Joint, canvas: Canvas) {
    val textPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 32f
        isAntiAlias = true
    }
        val angle = calculateAngle(
            PointF(scalex(a.x), scaley(a.y)),
            PointF(scalex(b.x), scaley(b.y)),
            PointF(scalex(c.x), scaley(c.y))
        )
        val text = "%.1f°".format(angle)

        val x = scalex(b.x) + 20
        val y = scaley(b.y) - 20

        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val padding = 12f

        val rectLeft = x - padding
        val rectTop = y - textBounds.height() - padding
        val rectRight = x + textBounds.width() + padding
        val rectBottom = y + padding

        val bgPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            rectLeft,
            rectTop,
            rectRight,
            rectBottom,
            25f,   // corner radius X
            25f,   // corner radius Y
            bgPaint
        )
        val textX = x
        val textY = y
        canvas.drawText(text, textX, textY, textPaint)
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