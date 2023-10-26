package com.example.foregroundservice

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.foregroundservice.databinding.FragmentCameraBinding
import com.example.foregroundservice.databinding.ItemClassificationResultBinding

class VisionFragment : Fragment(R.layout.fragment_camera),
     {

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private var mImageDisplay: ImageDisplay? = null

    override fun onDestroyView() {
        Log.d(TAG, "destroying view!!!")
        _fragmentCameraBinding = null
        super.onDestroyView()

        closeCamera()
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
            //bindCamera()

            // Attach listeners to UI control widgets
            initBottomSheetControls()
        }

        //aiResult = AIResult(context = requireContext())
    }

    inner class ViewHolder(private val binding: ItemClassificationResultBinding) :
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



    companion object {
        private const val TAG = "Vision Fragment"
        private const val NO_VALUE = "--"
    }

}