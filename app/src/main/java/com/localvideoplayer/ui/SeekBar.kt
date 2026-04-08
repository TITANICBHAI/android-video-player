package com.localvideoplayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.localvideoplayer.R

class SeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var duration: Long = 0L
    private var position: Long = 0L
    private var buffered: Long = 0L
    private var isDragging = false
    private var dragPosition: Long = 0L

    private var onSeekChange: ((Long, Boolean) -> Unit)? = null
    private var onSeekEnd: ((Long) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF
    }
    private val bufferedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55FFFFFF
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF0000.toInt()
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF0000.toInt()
    }

    private val trackHeight = context.resources.displayMetrics.density * 3
    private val thumbRadius = context.resources.displayMetrics.density * 6
    private val touchThumbRadius = context.resources.displayMetrics.density * 12
    private val trackRect = RectF()

    fun setDuration(ms: Long) { duration = ms; invalidate() }
    fun setProgress(positionMs: Long, bufferedMs: Long) {
        if (!isDragging) {
            position = positionMs
            buffered = bufferedMs
            invalidate()
        }
    }

    fun setOnSeekChangeListener(l: (Long, Boolean) -> Unit) { onSeekChange = l }
    fun setOnSeekEndListener(l: (Long) -> Unit) { onSeekEnd = l }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cy = height / 2f
        val paddingH = thumbRadius

        trackRect.set(paddingH, cy - trackHeight / 2, width - paddingH, cy + trackHeight / 2)
        val trackWidth = trackRect.width()

        canvas.drawRoundRect(trackRect, trackHeight / 2, trackHeight / 2, trackPaint)

        if (duration > 0) {
            val bufferedRatio = (buffered.toFloat() / duration).coerceIn(0f, 1f)
            val bufferedRect = RectF(trackRect.left, trackRect.top,
                trackRect.left + trackWidth * bufferedRatio, trackRect.bottom)
            canvas.drawRoundRect(bufferedRect, trackHeight / 2, trackHeight / 2, bufferedPaint)

            val currentPos = if (isDragging) dragPosition else position
            val progressRatio = (currentPos.toFloat() / duration).coerceIn(0f, 1f)
            val progressRect = RectF(trackRect.left, trackRect.top,
                trackRect.left + trackWidth * progressRatio, trackRect.bottom)
            canvas.drawRoundRect(progressRect, trackHeight / 2, trackHeight / 2, progressPaint)

            val thumbX = trackRect.left + trackWidth * progressRatio
            val r = if (isDragging) touchThumbRadius else thumbRadius
            canvas.drawCircle(thumbX, cy, r, thumbPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (duration <= 0) return false
        val paddingH = thumbRadius
        val trackWidth = width - paddingH * 2

        fun xToPosition(x: Float): Long {
            val ratio = ((x - paddingH) / trackWidth).coerceIn(0f, 1f)
            return (ratio * duration).toLong()
        }

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                dragPosition = xToPosition(event.x)
                onSeekChange?.invoke(dragPosition, true)
                invalidate()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                dragPosition = xToPosition(event.x)
                onSeekChange?.invoke(dragPosition, true)
                invalidate()
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                val finalPos = xToPosition(event.x)
                position = finalPos
                onSeekEnd?.invoke(finalPos)
                invalidate()
                true
            }
            else -> false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (thumbRadius * 3).toInt()
        val h = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h)
    }
}
