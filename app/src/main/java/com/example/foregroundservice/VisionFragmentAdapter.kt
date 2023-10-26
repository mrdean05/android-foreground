package com.example.foregroundservice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foregroundservice.databinding.ItemClassificationResultBinding

class VisionFragmentAdapter : RecyclerView.Adapter<VisionFragment.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisionFragment.ViewHolder {
        val binding = ItemClassificationResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisionFragment.ViewHolder, position: Int) {
        categories[position].let { category ->
            holder.bind(category?.label, category?.score)
        }
    }

    override fun getItemCount(): Int = categories.size

}

