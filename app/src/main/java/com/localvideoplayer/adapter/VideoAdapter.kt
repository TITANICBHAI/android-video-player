package com.localvideoplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.localvideoplayer.R
import com.localvideoplayer.databinding.ItemVideoBinding
import com.localvideoplayer.model.VideoItem
import com.localvideoplayer.model.WatchProgress

class VideoAdapter(
    private val onVideoClick: (VideoItem) -> Unit,
    private val onVideoLongClick: (VideoItem) -> Boolean = { false }
) : ListAdapter<VideoItem, VideoAdapter.VideoViewHolder>(DIFF) {

    private var progressMap: Map<Long, WatchProgress> = emptyMap()

    fun setProgressMap(map: Map<Long, WatchProgress>) {
        progressMap = map
        notifyItemRangeChanged(0, itemCount, PAYLOAD_PROGRESS)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position), progressMap[getItem(position).id])
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_PROGRESS)) {
            holder.updateProgress(progressMap[getItem(position).id])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class VideoViewHolder(private val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_ID) onVideoClick(getItem(pos.toInt()))
            }
            binding.root.setOnLongClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_ID) onVideoLongClick(getItem(pos.toInt())) else false
            }
        }

        fun bind(video: VideoItem, progress: WatchProgress?) {
            binding.tvTitle.text = video.displayName
                .substringBeforeLast(".")
                .replace("_", " ")
                .replace("-", " ")

            binding.tvMeta.text = buildString {
                append(video.durationFormatted)
                append(" • ")
                append(video.sizeFormatted)
                if (video.resolutionLabel.isNotEmpty()) {
                    append(" • ")
                    append(video.resolutionLabel)
                }
            }

            Glide.with(binding.ivThumbnail)
                .load(video.uri)
                .transform(CenterCrop())
                .placeholder(R.drawable.placeholder_video)
                .error(R.drawable.placeholder_video)
                .into(binding.ivThumbnail)

            updateProgress(progress)
        }

        fun updateProgress(progress: WatchProgress?) {
            if (progress != null && progress.duration > 0 && !progress.completed) {
                val pct = (progress.position.toFloat() / progress.duration).coerceIn(0f, 1f)
                binding.progressBar.progress = (pct * 1000).toInt()
                binding.progressBar.visibility = android.view.View.VISIBLE
            } else {
                binding.progressBar.visibility = android.view.View.INVISIBLE
            }
        }
    }

    companion object {
        private const val PAYLOAD_PROGRESS = "progress"
        private val DIFF = object : DiffUtil.ItemCallback<VideoItem>() {
            override fun areItemsTheSame(a: VideoItem, b: VideoItem) = a.id == b.id
            override fun areContentsTheSame(a: VideoItem, b: VideoItem) = a == b
        }
    }
}
