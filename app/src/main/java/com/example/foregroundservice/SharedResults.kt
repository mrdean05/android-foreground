package com.example.foregroundservice

import org.tensorflow.lite.task.vision.classifier.Classifications

object SharedResults {
    var cachedResults: List<Classifications>? = null
    var cacheInferenceTime:  Long = 0
}