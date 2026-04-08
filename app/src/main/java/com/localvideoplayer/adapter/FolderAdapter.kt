package com.localvideoplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.localvideoplayer.R
import com.localvideoplayer.databinding.ItemFolderBinding
import com.localvideoplayer.model.FolderItem

class FolderAdapter(
    private val onClick: (FolderItem) -> Unit
) : ListAdapter<FolderItem, FolderAdapter.FolderViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(private val binding: ItemFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_ID) onClick(getItem(pos.toInt()))
            }
        }

        fun bind(folder: FolderItem) {
            binding.tvFolderName.text = folder.bucketName
            binding.tvCount.text = "${folder.count} video${if (folder.count != 1) "s" else ""}"

            if (folder.firstVideoUri != null) {
                Glide.with(binding.ivCover)
                    .load(folder.firstVideoUri)
                    .transform(CenterCrop())
                    .placeholder(R.drawable.placeholder_folder)
                    .into(binding.ivCover)
            } else {
                binding.ivCover.setImageResource(R.drawable.placeholder_folder)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FolderItem>() {
            override fun areItemsTheSame(a: FolderItem, b: FolderItem) = a.bucketId == b.bucketId
            override fun areContentsTheSame(a: FolderItem, b: FolderItem) = a == b
        }
    }
}
