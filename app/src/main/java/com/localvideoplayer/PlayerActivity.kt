package com.localvideoplayer

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.localvideoplayer.databinding.ActivityPlayerBinding
import com.localvideoplayer.ui.PlayerControlsView
import com.localvideoplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_VIDEO_ID = "extra_video_id"
        private const val CONTROLS_HIDE_DELAY = 3500L
        private const val SEEK_AMOUNT_MS = 10_000L
    }

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private var hideControlsJob: Job? = null

    private var isFullscreen = false
    private var isLocked = false
    private var isControlsVisible = true
    private var videoId: Long = -1L
    private var videoUri: Uri? = null
    private var videoTitle: String = ""

    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var initialScaleFactor = 1f
    private var currentScaleFactor = 1f

    // Swipe gesture state
    private var swipeStartY = 0f
    private var swipeStartX = 0f
    private var isVolumeSwipe = false
    private var isBrightnessSwipe = false
    private var startVolume = 0f
    private var startBrightness = 0f

    private val windowInsetsController by lazy {
        WindowInsetsControllerCompat(window, binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoId = intent.getLongExtra(EXTRA_VIDEO_ID, -1L)
        videoUri = intent.getStringExtra(EXTRA_URI)?.let { Uri.parse(it) }
        videoTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Video"

        binding.controls.setTitle(videoTitle)

        setupGestures()
        setupControls()
        setupObservers()
        enableEdgeToEdge()
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onStop() {
        super.onStop()
        val pos = player?.currentPosition ?: 0L
        val dur = player?.duration ?: 0L
        viewModel.saveProgressNow(pos, dur)
        viewModel.stopPeriodicSave()
        progressJob?.cancel()
        releasePlayer()
    }

    private fun initPlayer() {
        val uri = videoUri ?: return

        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()

            lifecycleScope.launch {
                val saved = withContext(Dispatchers.IO) { viewModel.loadProgress(videoId) }
                val resumePos = saved?.position ?: 0L
                if (resumePos > 5000L) {
                    exo.seekTo(resumePos)
                    showResumeSnackbar(resumePos)
                }
                exo.playWhenReady = true
            }

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> binding.loadingIndicator.visibility = View.VISIBLE
                        Player.STATE_READY -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.controls.setDuration(exo.duration)
                            viewModel.startPeriodicSave(
                                getPosition = { exo.currentPosition },
                                getDuration = { exo.duration }
                            )
                            startProgressUpdates()
                        }
                        Player.STATE_ENDED -> {
                            binding.controls.setPlaying(false)
                            viewModel.saveProgressNow(exo.duration, exo.duration)
                        }
                        else -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    binding.controls.setPlaying(isPlaying)
                    if (isPlaying) scheduleHideControls() else hideControlsJob?.cancel()
                }
            })
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            while (isActive) {
                val pos = player?.currentPosition ?: 0L
                val dur = player?.duration?.takeIf { it > 0 } ?: 1L
                val buffered = player?.bufferedPosition ?: 0L
                binding.controls.setProgress(pos, dur, buffered)
                delay(500)
            }
        }
    }

    private fun setupControls() {
        binding.controls.setListener(object : PlayerControlsView.Listener {
            override fun onPlayPause() {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                scheduleHideControls()
            }

            override fun onSeekForward() {
                player?.let { it.seekTo((it.currentPosition + SEEK_AMOUNT_MS).coerceAtMost(it.duration)) }
                scheduleHideControls()
            }

            override fun onSeekBackward() {
                player?.let { it.seekTo((it.currentPosition - SEEK_AMOUNT_MS).coerceAtLeast(0L)) }
                scheduleHideControls()
            }

            override fun onSeekTo(positionMs: Long) {
                player?.seekTo(positionMs)
                scheduleHideControls()
            }

            override fun onSpeedSelected(speed: Float) {
                player?.setPlaybackSpeed(speed)
                scheduleHideControls()
            }

            override fun onFullscreen() {
                toggleFullscreen()
                scheduleHideControls()
            }

            override fun onLock() {
                isLocked = !isLocked
                binding.controls.setLocked(isLocked)
                scheduleHideControls()
            }

            override fun onPiP() {
                enterPiP()
            }

            override fun onBack() {
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isLocked) {
                    binding.controls.showLockOnly()
                    return true
                }
                toggleControlsVisibility()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isLocked) return false
                val screenWidth = binding.playerView.width
                if (e.x < screenWidth / 2) {
                    player?.seekTo((player!!.currentPosition - SEEK_AMOUNT_MS).coerceAtLeast(0L))
                    binding.seekFeedbackLeft.animateSeek()
                } else {
                    player?.seekTo((player!!.currentPosition + SEEK_AMOUNT_MS).coerceAtMost(player!!.duration))
                    binding.seekFeedbackRight.animateSeek()
                }
                return true
            }
        })

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                initialScaleFactor = currentScaleFactor
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentScaleFactor = (initialScaleFactor * detector.scaleFactor).coerceIn(0.8f, 3f)
                binding.playerView.scaleX = currentScaleFactor
                binding.playerView.scaleY = currentScaleFactor
                return true
            }
        })

        binding.playerView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                gestureDetector.onTouchEvent(event)
                handleSwipeGesture(event)
            }
            true
        }
    }

    private fun handleSwipeGesture(event: MotionEvent) {
        if (isLocked) return
        val screenWidth = binding.playerView.width.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                swipeStartX = event.x
                swipeStartY = event.y
                isVolumeSwipe = event.x > screenWidth * 0.6f
                isBrightnessSwipe = event.x < screenWidth * 0.4f
                val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
                startVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                val lp = window.attributes
                startBrightness = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = swipeStartY - event.y
                val screenHeight = binding.playerView.height.toFloat()
                val ratio = dy / screenHeight

                if (isVolumeSwipe && abs(dy) > 20) {
                    val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
                    val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    val newVol = (startVolume + ratio * maxVol).coerceIn(0f, maxVol.toFloat()).toInt()
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0)
                    val pct = (newVol.toFloat() / maxVol * 100).toInt()
                    binding.controls.showVolumeIndicator(pct)
                } else if (isBrightnessSwipe && abs(dy) > 20) {
                    val newBrightness = (startBrightness + ratio).coerceIn(0.01f, 1f)
                    val lp = window.attributes
                    lp.screenBrightness = newBrightness
                    window.attributes = lp
                    val pct = (newBrightness * 100).toInt()
                    binding.controls.showBrightnessIndicator(pct)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                binding.controls.hideVolumeIndicator()
                binding.controls.hideBrightnessIndicator()
                isVolumeSwipe = false
                isBrightnessSwipe = false
            }
        }
    }

    private fun setupObservers() {}

    private fun toggleControlsVisibility() {
        if (isControlsVisible) {
            hideControls()
        } else {
            showControls()
            scheduleHideControls()
        }
    }

    private fun showControls() {
        isControlsVisible = true
        binding.controls.show()
        if (!isFullscreen) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun hideControls() {
        isControlsVisible = false
        binding.controls.hide()
    }

    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        if (player?.isPlaying == true) {
            hideControlsJob = lifecycleScope.launch {
                delay(CONTROLS_HIDE_DELAY)
                hideControls()
            }
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
        binding.controls.setFullscreen(isFullscreen)
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPiPMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPiPMode)
        if (isInPiPMode) {
            binding.controls.visibility = View.GONE
        } else {
            binding.controls.visibility = View.VISIBLE
        }
    }

    private fun showResumeSnackbar(positionMs: Long) {
        val mins = positionMs / 60000
        val secs = (positionMs % 60000) / 1000
        val msg = "Resuming from %d:%02d".format(mins, secs)
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        hideControlsJob?.cancel()
        player?.release()
        player = null
    }
}
