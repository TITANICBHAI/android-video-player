package com.localvideoplayer.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupMenu
import com.localvideoplayer.R
import com.localvideoplayer.databinding.ViewPlayerControlsBinding

class PlayerControlsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    interface Listener {
        fun onPlayPause()
        fun onSeekForward()
        fun onSeekBackward()
        fun onSeekTo(positionMs: Long)
        fun onSpeedSelected(speed: Float)
        fun onFullscreen()
        fun onLock()
        fun onPiP()
        fun onBack()
    }

    private val binding = ViewPlayerControlsBinding.inflate(LayoutInflater.from(context), this, true)
    private var listener: Listener? = null
    private var duration: Long = 0L
    private var isPlaying = false
    private var isFullscreen = false
    private var isLocked = false
    private var isSeeking = false

    init {
        setupClickListeners()
    }

    fun setListener(l: Listener) { listener = l }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { listener?.onBack() }
        binding.btnPlayPause.setOnClickListener { listener?.onPlayPause() }
        binding.btnSkipForward.setOnClickListener { listener?.onSeekForward() }
        binding.btnSkipBackward.setOnClickListener { listener?.onSeekBackward() }
        binding.btnFullscreen.setOnClickListener { listener?.onFullscreen() }
        binding.btnLock.setOnClickListener { listener?.onLock() }
        binding.btnPip.setOnClickListener { listener?.onPiP() }
        binding.btnSpeed.setOnClickListener { showSpeedMenu() }
        binding.btnMore.setOnClickListener { showMoreMenu() }

        binding.seekBar.setOnSeekChangeListener { positionMs, fromUser ->
            if (fromUser) {
                isSeeking = true
                updateTimeDisplay(positionMs, duration)
            }
        }
        binding.seekBar.setOnSeekEndListener { positionMs ->
            isSeeking = false
            listener?.onSeekTo(positionMs)
        }
    }

    fun setTitle(title: String) {
        binding.tvTitle.text = title.substringBeforeLast(".").replace("_", " ")
    }

    fun setDuration(durationMs: Long) {
        duration = durationMs
        binding.tvTotal.text = formatTime(durationMs)
        binding.seekBar.setDuration(durationMs)
    }

    fun setProgress(positionMs: Long, durationMs: Long, bufferedMs: Long) {
        if (!isSeeking) {
            binding.seekBar.setProgress(positionMs, bufferedMs)
            updateTimeDisplay(positionMs, durationMs)
        }
    }

    private fun updateTimeDisplay(positionMs: Long, durationMs: Long) {
        binding.tvCurrent.text = formatTime(positionMs)
        binding.tvTotal.text = formatTime(durationMs)
    }

    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        binding.btnPlayPause.setImageResource(
            if (playing) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    fun setFullscreen(fullscreen: Boolean) {
        isFullscreen = fullscreen
        binding.btnFullscreen.setImageResource(
            if (fullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
        )
    }

    fun setLocked(locked: Boolean) {
        isLocked = locked
        binding.btnLock.setImageResource(
            if (locked) R.drawable.ic_lock else R.drawable.ic_lock_open
        )
        binding.topBar.visibility = if (locked) GONE else VISIBLE
        binding.centerControls.visibility = if (locked) GONE else VISIBLE
        binding.seekBar.visibility = if (locked) GONE else VISIBLE
        binding.timeRow.visibility = if (locked) GONE else VISIBLE
        binding.btnFullscreen.visibility = if (locked) GONE else VISIBLE
        binding.btnPip.visibility = if (locked) GONE else VISIBLE
        binding.btnSpeed.visibility = if (locked) GONE else VISIBLE
    }

    fun showLockOnly() {
        animate().alpha(1f).setDuration(150).start()
        postDelayed({ animate().alpha(0f).setDuration(300).start() }, 2000)
    }

    fun show() {
        visibility = VISIBLE
        animate().alpha(1f).setDuration(200).setListener(null).start()
    }

    fun hide() {
        animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = GONE
            }
        }).start()
    }

    fun showVolumeIndicator(percent: Int) {
        binding.volumeIndicator.visibility = VISIBLE
        binding.tvVolume.text = "$percent%"
        binding.volumeProgress.progress = percent
    }

    fun hideVolumeIndicator() {
        binding.volumeIndicator.visibility = GONE
    }

    fun showBrightnessIndicator(percent: Int) {
        binding.brightnessIndicator.visibility = VISIBLE
        binding.tvBrightness.text = "$percent%"
        binding.brightnessProgress.progress = percent
    }

    fun hideBrightnessIndicator() {
        binding.brightnessIndicator.visibility = GONE
    }

    private fun showSpeedMenu() {
        val popup = PopupMenu(context, binding.btnSpeed)
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
        speeds.forEachIndexed { i, speed ->
            val label = if (speed == 1f) "Normal (1x)" else "${speed}x"
            popup.menu.add(0, i, i, label)
        }
        popup.setOnMenuItemClickListener { item ->
            listener?.onSpeedSelected(speeds[item.itemId])
            binding.btnSpeed.text = if (speeds[item.itemId] == 1f) "1x" else "${speeds[item.itemId]}x"
            true
        }
        popup.show()
    }

    private fun showMoreMenu() {
        val popup = PopupMenu(context, binding.btnMore)
        popup.menuInflater.inflate(R.menu.player_more, popup.menu)
        popup.setOnMenuItemClickListener { true }
        popup.show()
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
