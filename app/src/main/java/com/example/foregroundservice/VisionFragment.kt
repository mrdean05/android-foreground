package com.example.foregroundservice

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.segway.robot.sdk.vision.BindStateListener
import com.segway.robot.sdk.vision.Vision
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.stream.PixelFormat
import com.segway.robot.sdk.vision.stream.Resolution
import com.segway.robot.sdk.vision.stream.VisionStreamType

import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.example.foregroundservice.databinding.FragmentCameraBinding
import com.example.foregroundservice.databinding.ItemClassificationResultBinding
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.util.Timer
import java.util.TimerTask
import kotlin.math.roundToInt

class VisionFragment : Fragment(R.layout.fragment_camera){

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private val classificationResultsAdapter by lazy {
        ClassificationResultsAdapter().apply {
            updateAdapterSize(3)
        }
    }

    private var mTimer: Timer? = null
    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private var mImageDisplay: ImageDisplay? = null

    private val mLock = Any()


    override fun onDestroyView() {
        Log.d(TAG, "destroying view!!!")
        _fragmentCameraBinding = null
        super.onDestroyView()

        if (mTimer != null) {
            mTimer?.cancel()
            mTimer = null
        }
        // closeCamera()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Stopping Fragment")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(fragmentCameraBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = classificationResultsAdapter
        }

        if (_fragmentCameraBinding != null) {
            // bind camera
            if (mTimer == null) {
                mTimer = Timer()
                mTimer?.schedule(ImageDisplayTimerTask(), 0, 34)
            }

            // Attach listeners to UI control widgets
            //initBottomSheetControls()
        }

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
            mImageDisplay = ImageDisplay(width, height, bitmapBuffer, fragmentCameraBinding.ivCamera)
            val pixelFormat = frame.info.pixelFormat
            if (pixelFormat == PixelFormat.YUV420 || pixelFormat == PixelFormat.YV12) {
                val limit = frame.byteBuffer.limit()
                val buff = ByteArray(limit)
                frame.byteBuffer.position(0)
                frame.byteBuffer.get(buff)
                yuv2RGBBitmap(buff, bitmapBuffer, width, height)
            } else {
                Log.d(TAG, "An unsupported format")
            }
        }
        runOnUiThread(mImageDisplay)
        val results = SharedResults.cachedResults
        val inferenceTime = SharedResults.cacheInferenceTime

        runOnUiThread{
            classificationResultsAdapter.updateResults(results)
            classificationResultsAdapter.notifyDataSetChanged()
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

    inner class ImageDisplayTimerTask : TimerTask() {
        override fun run() {
            synchronized(mLock) {
                var frame: Frame? = null
                try {
                    frame = SharedResults.frame
                } catch (e: Exception) {
                    Log.e(TAG, "IllegalArgumentException  " + e.message)
                }
                if (frame != null) {
                    parseFrame(frame)
                }

            }
        }
    }

    companion object {
        private const val TAG = "Vision Fragment"
        private const val NO_VALUE = "--"
    }

}