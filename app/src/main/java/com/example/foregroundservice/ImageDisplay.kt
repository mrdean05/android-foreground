package com.example.foregroundservice

import android.graphics.Bitmap
import android.widget.ImageView

class ImageDisplay(width: Int, height: Int, private val bitmapBuffer: Bitmap, private val ivCamera: ImageView) :
    Runnable {
    private var mWidth: Int
    private var mHeight: Int
    private var setParamsFlag: Boolean
    private var zoom = 0.5f

    init {
        mWidth = (width * zoom).toInt()
        mHeight = (height * zoom).toInt()
        setParamsFlag = false
    }

    override fun run() {
        if (!setParamsFlag) {
            val params = ivCamera.layoutParams
            params.width = mWidth
            params.height = mHeight
            ivCamera.layoutParams = params
            setParamsFlag = true
        }
        ivCamera.setImageBitmap(bitmapBuffer)
    }
}