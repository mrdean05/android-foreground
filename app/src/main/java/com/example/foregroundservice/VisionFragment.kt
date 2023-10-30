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

class VisionFragment : Fragment(R.layout.fragment_camera), ImageClassifierHelper.ClassifierListener {

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private val classificationResultsAdapter by lazy {
        ClassificationResultsAdapter().apply {
            updateAdapterSize(imageClassifierHelper.maxResults)
        }
    }

    private var mTimer1: Timer? = null
    //private val sharedPreferences = context?.getSharedPreferences("ParsePreference", Context.MODE_PRIVATE)
    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private var mImageDisplay: ImageDisplay? = null

    private val mLock = Any()


    override fun onDestroyView() {
        Log.d(TAG, "destroying view!!!")
        _fragmentCameraBinding = null
        super.onDestroyView()

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

        //imageClassifierHelper = ImageClassifierHelper (context = requireContext(), imageClassifierListener = this)
        sharedPreferenceManager = SharedPreferenceManager(requireContext())


        //with(fragmentCameraBinding.recyclerviewResults) {
        //    layoutManager = LinearLayoutManager(requireContext())
        //    adapter = classificationResultsAdapter
        //}

        if (_fragmentCameraBinding != null) {
            // bind camera

            /*
            mImageDisplay =
                bitmapBuffer?.let {
                    ImageDisplay(width, height,
                        it, fragmentCameraBinding.ivCamera)
                }

             */

            //val (width, height, bitmapBuffer) = sharedPreferenceManager.getWidthHeightBitmap()

            if (mTimer1 == null) {
                mTimer1 = Timer()
                mTimer1?.schedule(ImageDisplayTimerTask(), 0, 34)
            }



            // Attach listeners to UI control widgets
            //initBottomSheetControls()
        }

        //aiResult = AIResult(context = requireContext())
    }

    private fun initBottomSheetControls() {
        // When clicked, lower classification score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (imageClassifierHelper.threshold >= 0.1) {
                imageClassifierHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise classification score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (imageClassifierHelper.threshold < 0.9) {
                imageClassifierHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be classified at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (imageClassifierHelper.maxResults > 1) {
                imageClassifierHelper.maxResults--
                updateControlsUi()
                classificationResultsAdapter.updateAdapterSize(size = imageClassifierHelper.maxResults)
            }
        }

        // When clicked, increase the number of objects that can be classified at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (imageClassifierHelper.maxResults < 3) {
                imageClassifierHelper.maxResults++
                updateControlsUi()
                classificationResultsAdapter.updateAdapterSize(size = imageClassifierHelper.maxResults)
            }
        }

        // When clicked, decrease the number of threads used for classification
        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (imageClassifierHelper.numThreads > 1) {
                imageClassifierHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for classification
        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (imageClassifierHelper.numThreads < 4) {
                imageClassifierHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageClassifierHelper.currentDelegate = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /* no op */
                }
            }

        // When clicked, change the underlying model used for object classification
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageClassifierHelper.currentModel = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset classifier.
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
            imageClassifierHelper.maxResults.toString()

        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", imageClassifierHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
            imageClassifierHelper.numThreads.toString()
        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        imageClassifierHelper.clearImageClassifier()
    }

   /* inner class ViewHolder(private val binding: ItemClassificationResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(label: String?, score: Float?) {
            // Define a set of labels to consider as "Non-Sidewalks"
            val nonSidewalkLabels = setOf("bike_lane", "garage", "street", "crosswalk")

            if (label in nonSidewalkLabels) {
                with(binding) {
                    tvLabel.text = "non-sidewalk" ?: NO_VALUE
                    tvScore.text = if (score != null) String.format("%.2f", score) else NO_VALUE
                }
            } else {
                with(binding) {
                    tvLabel.text = label ?: NO_VALUE
                    tvScore.text = if (score != null) String.format("%.2f", score) else NO_VALUE
                }
            }
        }
    }
    */

    @SuppressLint("NotifyDataSetChanged")
    override fun onError(error: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            classificationResultsAdapter.updateResults(null)
            classificationResultsAdapter.notifyDataSetChanged()
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onResults(
        results: List<Classifications>?,
        inferenceTime: Long
    ) {
        if (activity != null) {
            requireActivity().runOnUiThread {
                // Show result on bottom sheet
                classificationResultsAdapter.updateResults(results)
                classificationResultsAdapter.notifyDataSetChanged()
                if (_fragmentCameraBinding != null) {
                    fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                        String.format("%d ms", inferenceTime)
                }
            }
        }
    }

    inner class ImageDisplayTimerTask : TimerTask() {
        //private lateinit var sharedPreferenceManager: SharedPreferenceManager
        // val sharedPreferenceManager = SharedPreferenceManager(requireContext())
        override fun run() {
            synchronized(mLock) {
                var frame: Frame? = null
                try {
                    //frame = Vision.getInstance().getLatestFrame(VisionStreamType.FISH_EYE)
                    frame = sharedPreferenceManager.getFrame()
                } catch (e: Exception) {
                    Log.e(TAG, "IllegalArgumentException  " + e.message)
                }
                if (frame != null) {
                    parseFrame(frame)
                    //.getInstance().returnFrame(frame)
                }
            }
        }
    }

        /*
    inner class ImageDisplay(private val mWidth: Int, private val mHeight: Int) : Runnable {
        var setParamsFlag: Boolean = false
        private val zoom = 0.5f

        override fun run() {
            if (!setParamsFlag) {
                val params = mCameraView.layoutParams
                params.width = (mWidth * zoom).toInt()
                params.height = (mHeight * zoom).toInt()
                mCameraView.layoutParams = params
                setParamsFlag = true
            }

            mCameraView.setImageBitmap(mBitmap)
        }
    }

         */

    companion object {
        private const val TAG = "Vision Fragment"
        private const val NO_VALUE = "--"
    }

}