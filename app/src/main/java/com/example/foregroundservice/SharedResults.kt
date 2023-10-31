package com.example.foregroundservice

import org.tensorflow.lite.task.vision.classifier.Classifications
import com.segway.robot.sdk.vision.frame.Frame


object SharedResults {
    var cachedResults: List<Classifications>? = null
    var cacheInferenceTime:  Long = 0
    var frame: Frame? = null
    var height: Int = 1080
    var width: Int = 1920
}