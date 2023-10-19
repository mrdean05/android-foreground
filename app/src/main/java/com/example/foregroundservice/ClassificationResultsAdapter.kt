package com.example.foregroundservice

import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.classifier.Classifications
import kotlin.math.min

class ClassificationResultsAdapter {
    companion object {
        private const val NO_VALUE = "--"
    }

    private var categories: MutableList<Category?> = mutableListOf()
    private var adapterSize: Int = 0

    fun updateResults(listClassifications: List<Classifications>?) {
        categories = MutableList(adapterSize) { null }
        listClassifications?.let { it ->
            if (it.isNotEmpty()) {
                val sortedCategories = it[0].categories.sortedBy { it?.index }
                val min = min(sortedCategories.size, categories.size)
                for (i in 0 until min) {
                    categories[i] = sortedCategories[i]
                }
            }
        }
    }

    fun updateAdapterSize(size: Int) {
        adapterSize = size
    }

    fun getCategoryLabelAndScore(position: Int): Pair<String, Float> {
        val category = categories[position]
        val label = category?.label ?: "Unknown"
        val score = category?.score ?: 0f
        return Pair(label, score)
    }

    fun getItemCount(): Int = categories.size
}