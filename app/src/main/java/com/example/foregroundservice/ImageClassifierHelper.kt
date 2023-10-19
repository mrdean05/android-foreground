package com.example.foregroundservice


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.lang.reflect.InvocationTargetException


class ImageClassifierHelper(
    var threshold: Float = 0.6f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 2,
    var currentModel: Int = 4,
    val context: Context,
    val imageClassifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    fun clearImageClassifier() {
        imageClassifier = null
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }

            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    imageClassifierListener?.onError("GPU is not supported on this device")
                }
            }

            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName =
            when (currentModel) {
                MODEL_MOBILENETV1 -> "genericv1.tflite"
                MODEL_EFFICIENTNETV0 -> "efficientnet-lite0.tflite"
                MODEL_EFFICIENTNETV1 -> "efficientnet-lite1.tflite"
                MODEL_EFFICIENTNETV2 -> "efficientnet-lite2.tflite"
                MODEL_CHICAGO -> "chicago_01.tflite"
                else -> "genericv1.tflite"
            }

        try {
            imageClassifier =
                ImageClassifier.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            imageClassifierListener?.onError(
                "Image classifier failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        } catch (e: InvocationTargetException) {
            imageClassifierListener?.onError(
                "Image classifier failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    @SuppressLint("HardwareIds")
    fun classify(image: Bitmap) {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // save original image
        /*ImageFile(context).save(
            imageName = "orig-image",
            image = image,
            inferenceTime = inferenceTime
        )*/

        // this is cropping values
        val rideCropFromWidth = 500
        val rideCropToWidth = 850
        val rideCropHeightFrom = 20
        val rideCropHeightTo = 900

        // crop image
        val cropImage = Bitmap.createBitmap(
            image,
            rideCropFromWidth,
            rideCropHeightFrom,
            rideCropToWidth,
            rideCropHeightTo
        )
        /*
                // save cropped image
                ImageFile(context).save(
                    imageName = "crop-image",
                    image = cropImage,
                    inferenceTime = inferenceTime
                )
        */
        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessorBuilder = ImageProcessor.Builder()
        val imageProcessor = imageProcessorBuilder
            .add(
                ResizeOp(
                    Constants.IMAGE_SIZE,
                    Constants.IMAGE_SIZE,
                    ResizeOp.ResizeMethod.BILINEAR
                )
            )
            .build()

        // Preprocess the image and convert it into a TensorImage for classification.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(cropImage))
        val results = imageClassifier?.classify(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        imageClassifierListener?.onResults(
            results,
            inferenceTime
        )
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }



    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTNETV0 = 1
        const val MODEL_EFFICIENTNETV1 = 2
        const val MODEL_EFFICIENTNETV2 = 3
        const val MODEL_CHICAGO = 4

        private const val TAG = "ImageClassifierHelper"
    }

}