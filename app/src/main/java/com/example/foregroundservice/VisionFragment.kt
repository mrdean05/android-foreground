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

import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.example.foregroundservice.databinding.FragmentCameraBinding
import com.example.foregroundservice.databinding.ItemClassificationResultBinding
import org.tensorflow.lite.task.vision.classifier.Classifications

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

    //private val sharedPreferences = context?.getSharedPreferences("ParsePreference", Context.MODE_PRIVATE)
    private var mImageDisplay: ImageDisplay? = null
    private lateinit var sharedPreferenceManager: SharedPreferenceManager


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

        /*
        val sharedPreferenceManager = SharedPreferenceManager(requireContext())
        val (width, height, bitmapBuffer) = sharedPreferenceManager.getWidthHeightBitmap()
        println("the width is $width, height is $height")
        */

        val width = 1920
        val height = 1080

        val bitmapBufferAnnex = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        //imageClassifierHelper = ImageClassifierHelper (context = requireContext(), imageClassifierListener = this)


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

            //val rgba = sharedPreferenceManager.getRGBA()
            mImageDisplay = ImageDisplay(width, height, bitmapBufferAnnex, fragmentCameraBinding.ivCamera)

            //if (rgba  != null ) {
            //    bitmapBufferAnnex.setPixels(rgba, 0, width, 0, 0, width, height )
            //}

            println("UI/UX updating")

            runOnUiThread(mImageDisplay)


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


    companion object {
        private const val TAG = "Vision Fragment"
        private const val NO_VALUE = "--"
    }

}