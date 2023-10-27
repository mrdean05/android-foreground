package com.example.foregroundservice

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.fragment.app.Fragment
import android.content.SharedPreferences
import android.provider.Settings.System
import android.util.Base64
import com.segway.robot.sdk.vision.BindStateListener
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.stream.PixelFormat
import com.segway.robot.sdk.vision.stream.Resolution
import com.segway.robot.sdk.vision.stream.VisionStreamType
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class CameraFragment : ImageClassifierHelper.ClassifierListener {

    private var currentTrackName = ""
    private lateinit var aiResult: AIResult
    private val mLock = Any()

    private lateinit var sharedPreferences: SharedPreferenceManager

    private lateinit var imageClassifierHelper: ImageClassifierHelper

    private fun initializeImage(context: Context) {
        imageClassifierHelper = ImageClassifierHelper(context = context, imageClassifierListener = this)
        aiResult = AIResult(context = context)

        sharedPreferences = SharedPreferenceManager(context)

    }

    private fun classifyImage(bitmapBuffer: Bitmap) {
        imageClassifierHelper.classify(bitmapBuffer)
    }

    fun bindCamera(context: Context) {
        initializeImage(context)
        println("Binding camera")
        Vision.getInstance().bindService(context, object : BindStateListener {
            override fun onBind() {
                Log.d(TAG, "Vision Service onBind")
                Vision.getInstance().startVision(
                    VisionStreamType.FISH_EYE
                ) { _, frame -> parseFrame(frame) }
            }

            override fun onUnbind(reason: String) {
                Log.d(TAG, "Vision Service onUnbind")
            }
        })
    }

    private fun parseFrame(frame: Frame) {
        synchronized(mLock) {
            val resolution = frame.info.resolution
            val width: Int = Resolution.getWidth(resolution)
            val height: Int = Resolution.getHeight(resolution)
            val bitmapBuffer = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )


            sharedPreferences.setWidthHeightBitmap(width, height, bitmapBuffer)


                val pixelFormat = frame.info.pixelFormat
            if (pixelFormat == PixelFormat.YUV420 || pixelFormat == PixelFormat.YV12) {
                val limit = frame.byteBuffer.limit()
                val buff = ByteArray(limit)
                frame.byteBuffer.position(0)
                frame.byteBuffer.get(buff)
                yuv2RGBBitmap(buff, bitmapBuffer, width, height)
                classifyImage(bitmapBuffer)
            } else {
                Log.d(TAG, "An unsupported format")
            }
        }
    }

    private fun yuv2RGBBitmap(data: ByteArray, bitmap: Bitmap, width: Int, height: Int) {
        val frameSize = width * height
        val rgba = IntArray(frameSize)
        for (i in 0 until height) {
            for (j in 0 until width) {
                var y = 0xff and data[i * width + j].toInt()
                val v = 0xff and data[frameSize + (i shr 1) * width + (j and 1.inv()) + 0].toInt()
                val u = 0xff and data[frameSize + (i shr 1) * width + (j and 1.inv()) + 1].toInt()
                y = if (y < 16) 16 else y
                var r = (1.164f * (y - 16) + 1.596f * (v - 128)).roundToInt()
                var g = (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128)).roundToInt()
                var b = (1.164f * (y - 16) + 2.018f * (u - 128)).roundToInt()
                r = if (r < 0) 0 else if (r > 255) 255 else r
                g = if (g < 0) 0 else if (g > 255) 255 else g
                b = if (b < 0) 0 else if (b > 255) 255 else b
                rgba[i * width + j] = -0x1000000 + (b shl 16) + (g shl 8) + r
            }
        }
        bitmap.setPixels(rgba, 0, width, 0, 0, width, height)
    }

    companion object {
        private const val TAG = "Image Classifier"
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onError(error: String) {
        // Handle error
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        // Handle classification results
        if (results != null) {
            playSoundsForDetectedCategories(results)
        }
    }

    private fun playSoundsForDetectedCategories(results: List<Classifications>?) {
        results?.let { classifications ->
            if (classifications.isNotEmpty()) {
                if (classifications[0].categories.isNotEmpty()) {
                    classifications[0].categories[0].label?.let { label ->
                        when {
                            label.contentEquals("sidewalk") -> {
                                if (!currentTrackName.contentEquals("sidewalk")) {
                                    currentTrackName = "sidewalk"
                                    Log.d(TAG, "Playing sidewalk sound with IoT file")
                                    aiResult.setDetectedSidewalks()
                                }
                            }

                            label.contentEquals("garage") -> {
                                currentTrackName = "garage"
                                aiResult.setDetectedNonSidewalks()
                            }

                            label.contentEquals("crosswalk") -> {
                                if (!currentTrackName.contentEquals("crosswalk")) {
                                    currentTrackName = "crosswalk"
                                    Log.d(TAG, "Playing crosswalk sound")
                                    aiResult.setDetectedNonSidewalks()
                                }
                            }

                            label.contentEquals("street") -> {
                                currentTrackName = "street"
                                aiResult.setDetectedNonSidewalks()
                            }

                            else -> {
                                currentTrackName = ""
                                aiResult.setDetectedNonSidewalks()
                            }
                        }
                    }
                }
            }
        }
    }
}