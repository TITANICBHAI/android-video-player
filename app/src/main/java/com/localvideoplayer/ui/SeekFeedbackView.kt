package com.localvideoplayer.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.localvideoplayer.R

class SeekFeedbackView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    fun animateSeek() {
        visibility = View.VISIBLE
        alpha = 1f
        scaleX = 0.8f
        scaleY = 0.8f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@SeekFeedbackView, View.ALPHA, 1f, 0f).apply {
                    startDelay = 300
                    duration = 300
                },
                ObjectAnimator.ofFloat(this@SeekFeedbackView, View.SCALE_X, 0.8f, 1.1f, 1f).apply {
                    duration = 300
                },
                ObjectAnimator.ofFloat(this@SeekFeedbackView, View.SCALE_Y, 0.8f, 1.1f, 1f).apply {
                    duration = 300
                }
            )
            start()
        }

        postDelayed({ visibility = View.GONE }, 700)
    }
}
