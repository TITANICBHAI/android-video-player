package com.localvideoplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.localvideoplayer.adapter.FolderAdapter
import com.localvideoplayer.adapter.VideoAdapter
import com.localvideoplayer.databinding.ActivityMainBinding
import com.localvideoplayer.model.SortOrder
import com.localvideoplayer.model.VideoItem
import com.localvideoplayer.model.WatchProgress
import com.localvideoplayer.viewmodel.LibraryViewModel
import com.localvideoplayer.viewmodel.ViewMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LibraryViewModel by viewModels()

    private val videoAdapter = VideoAdapter(
        onVideoClick = { playVideo(it) },
        onVideoLongClick = { showVideoOptions(it); true }
    )

    private val folderAdapter = FolderAdapter { folder ->
        viewModel.loadFolderVideos(folder.bucketId, folder.bucketName)
        viewModel.setViewMode(ViewMode.FOLDER_DETAIL)
        binding.rvFolders.visibility = View.GONE
        binding.rvVideos.visibility = View.VISIBLE
        binding.tvFolderTitle.text = folder.bucketName
        binding.tvFolderTitle.visibility = View.VISIBLE
        binding.btnFolderBack.visibility = View.VISIBLE
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            viewModel.loadAllVideos()
        } else {
            binding.tvEmpty.text = getString(R.string.permission_denied)
            binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URI, uri.toString())
            putExtra(PlayerActivity.EXTRA_TITLE, uri.lastPathSegment ?: "Video")
            putExtra(PlayerActivity.EXTRA_VIDEO_ID, -1L)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerViews()
        setupChips()
        setupSearch()
        setupFolderBack()
        setupObservers()
        checkPermissionsAndLoad()

        binding.fabPickFile.setOnClickListener {
            filePickerLauncher.launch("video/*")
        }
    }

    private fun setupRecyclerViews() {
        binding.rvVideos.apply {
            adapter = videoAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        binding.rvFolders.apply {
            adapter = folderAdapter
            layoutManager = GridLayoutManager(this@MainActivity, 2)
        }
    }

    private fun setupChips() {
        binding.chipAll.setOnClickListener { switchToMode(ViewMode.ALL_VIDEOS) }
        binding.chipFolders.setOnClickListener { switchToMode(ViewMode.FOLDERS) }
        binding.chipRecent.setOnClickListener { switchToMode(ViewMode.RECENT) }
    }

    private fun switchToMode(mode: ViewMode) {
        binding.tvFolderTitle.visibility = View.GONE
        binding.btnFolderBack.visibility = View.GONE
        when (mode) {
            ViewMode.FOLDERS -> {
                binding.rvVideos.visibility = View.GONE
                binding.rvFolders.visibility = View.VISIBLE
                viewModel.setViewMode(ViewMode.FOLDERS)
            }
            else -> {
                binding.rvVideos.visibility = View.VISIBLE
                binding.rvFolders.visibility = View.GONE
                viewModel.setViewMode(mode)
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFolderBack() {
        binding.btnFolderBack.setOnClickListener {
            binding.tvFolderTitle.visibility = View.GONE
            binding.btnFolderBack.visibility = View.GONE
            binding.rvVideos.visibility = View.GONE
            binding.rvFolders.visibility = View.VISIBLE
            viewModel.setViewMode(ViewMode.FOLDERS)
            viewModel.loadFolders()
        }
    }

    private fun setupObservers() {
        viewModel.videos.observe(this) { videos ->
            videoAdapter.submitList(videos)
            binding.tvEmpty.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
            binding.tvEmpty.text = getString(R.string.no_videos)
            binding.tvVideoCount.text = "${videos.size} video${if (videos.size != 1) "s" else ""}"
        }

        viewModel.folders.observe(this) { folders ->
            folderAdapter.submitList(folders)
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        }

        lifecycleScope.launch {
            viewModel.watchProgress.collectLatest { progresses ->
                val map = progresses.associateBy { it.videoId }
                videoAdapter.setProgressMap(map)
            }
        }
    }

    private fun checkPermissionsAndLoad() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.loadAllVideos()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun playVideo(video: VideoItem) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URI, video.uri.toString())
            putExtra(PlayerActivity.EXTRA_TITLE, video.displayName)
            putExtra(PlayerActivity.EXTRA_VIDEO_ID, video.id)
        }
        startActivity(intent)
    }

    private fun showVideoOptions(video: VideoItem) {
        val anchor = binding.rvVideos
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.video_options, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_play -> { playVideo(video); true }
                R.id.action_share -> { shareVideo(video); true }
                R.id.action_info -> { showVideoInfo(video); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun shareVideo(video: VideoItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = video.mimeType.ifEmpty { "video/*" }
            putExtra(Intent.EXTRA_STREAM, video.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_video)))
    }

    private fun showVideoInfo(video: VideoItem) {
        val msg = """
            Name: ${video.displayName}
            Duration: ${video.durationFormatted}
            Size: ${video.sizeFormatted}
            Resolution: ${video.width}×${video.height} (${video.resolutionLabel})
            Type: ${video.mimeType}
            Folder: ${video.bucketName}
        """.trimIndent()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.video_info)
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                showSortMenu(binding.toolbar)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.apply {
            add(0, 0, 0, "Name (A-Z)")
            add(0, 1, 1, "Name (Z-A)")
            add(0, 2, 2, "Date Added (Newest)")
            add(0, 3, 3, "Date Added (Oldest)")
            add(0, 4, 4, "Size (Largest)")
            add(0, 5, 5, "Size (Smallest)")
            add(0, 6, 6, "Duration (Longest)")
            add(0, 7, 7, "Duration (Shortest)")
        }
        popup.setOnMenuItemClickListener { menuItem ->
            val order = when (menuItem.itemId) {
                0 -> SortOrder.NAME_ASC
                1 -> SortOrder.NAME_DESC
                2 -> SortOrder.DATE_ADDED_DESC
                3 -> SortOrder.DATE_ADDED_ASC
                4 -> SortOrder.SIZE_DESC
                5 -> SortOrder.SIZE_ASC
                6 -> SortOrder.DURATION_DESC
                7 -> SortOrder.DURATION_ASC
                else -> SortOrder.DATE_ADDED_DESC
            }
            viewModel.setSortOrder(order)
            true
        }
        popup.show()
    }
}
