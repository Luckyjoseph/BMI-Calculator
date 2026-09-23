package com.example.bmicalculator

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

/**
 * Horizontal BMI scale from [MIN_BMI] to [MAX_BMI] split into the four WHO categories,
 * with a marker that animates to the current value.
 */
class BmiGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Segment(val start: Float, val end: Float, val colorRes: Int)

    private val segments = listOf(
        Segment(MIN_BMI, 18.5f, R.color.bmi_underweight),
        Segment(18.5f, 25f, R.color.bmi_normal),
        Segment(25f, 30f, R.color.bmi_overweight),
        Segment(30f, MAX_BMI, R.color.bmi_obese)
    )

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            11f,
            resources.displayMetrics
        )
        textAlign = Paint.Align.CENTER
    }

    private val segmentRect = RectF()
    private val density = resources.displayMetrics.density
    private val trackHeight = 12f * density
    private val segmentGap = 3f * density
    private val markerRadius = 9f * density

    private var animator: ValueAnimator? = null
    private var animatedBmi = Float.NaN

    var bmi: Float = Float.NaN
        private set

    var labelColor: Int = Color.GRAY
        set(value) {
            field = value
            labelPaint.color = value
            invalidate()
        }

    var markerOutlineColor: Int = Color.WHITE
        set(value) {
            field = value
            markerRingPaint.color = value
            invalidate()
        }

    fun setBmi(value: Float, animate: Boolean = true) {
        bmi = value
        animator?.cancel()
        if (!animate || animatedBmi.isNaN()) {
            animatedBmi = value
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(animatedBmi, value).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animatedBmi = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (trackHeight + markerRadius * 2 + labelPaint.textSize * 1.8f).toInt()
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val usableStart = paddingStart + markerRadius
        val usableEnd = width - paddingEnd - markerRadius
        val usableWidth = usableEnd - usableStart
        if (usableWidth <= 0f) return

        val trackTop = paddingTop + markerRadius * 0.6f
        val trackBottom = trackTop + trackHeight
        val radius = trackHeight / 2f

        segments.forEach { segment ->
            trackPaint.color = ContextCompat.getColor(context, segment.colorRes)
            val left = usableStart + fractionOf(segment.start) * usableWidth
            val right = usableStart + fractionOf(segment.end) * usableWidth
            segmentRect.set(left, trackTop, max(left, right - segmentGap), trackBottom)
            canvas.drawRoundRect(segmentRect, radius, radius, trackPaint)
        }

        BOUNDARY_LABELS.forEach { boundary ->
            val x = usableStart + fractionOf(boundary) * usableWidth
            canvas.drawText(
                formatBoundary(boundary),
                x,
                trackBottom + labelPaint.textSize * 1.6f,
                labelPaint
            )
        }

        if (animatedBmi.isNaN()) return

        val markerX = usableStart + fractionOf(animatedBmi) * usableWidth
        val markerY = trackTop + trackHeight / 2f
        markerPaint.color = ContextCompat.getColor(context, colorResFor(animatedBmi))
        markerRingPaint.strokeWidth = 3f * density
        canvas.drawCircle(markerX, markerY, markerRadius, markerRingPaint)
        canvas.drawCircle(markerX, markerY, markerRadius - 2f * density, markerPaint)
    }

    private fun fractionOf(value: Float): Float =
        (min(max(value, MIN_BMI), MAX_BMI) - MIN_BMI) / (MAX_BMI - MIN_BMI)

    private fun formatBoundary(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString() else value.toString()

    private fun colorResFor(value: Float): Int =
        segments.first { value < it.end || it === segments.last() }.colorRes

    companion object {
        private const val MIN_BMI = 15f
        private const val MAX_BMI = 40f
        private const val ANIMATION_DURATION_MS = 700L
        private val BOUNDARY_LABELS = listOf(15f, 18.5f, 25f, 30f, 40f)
    }
}
