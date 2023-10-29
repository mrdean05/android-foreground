package com.example.foregroundservice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import com.google.gson.Gson

class SharedPreferenceManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

    fun setWidthHeightBitmap(width: Int, height: Int, bitmapBuffer: Bitmap) {
        val editor = sharedPreferences.edit()
        editor.putInt("width", width)
        editor.putInt("height", height)

        // Convert bitmapBuffer to a Base64 string

        val bitmapBufferString = bitmapBuffer.toBase64String()

        editor.putString("bitmapBuffer", bitmapBufferString)
        editor.apply()
    }

    fun getWidthHeightBitmap(): Triple<Int, Int, Bitmap?> {
        val width = sharedPreferences.getInt("width", 1920)
        val height = sharedPreferences.getInt("height", 1080)
        val bitmapBufferString = sharedPreferences.getString("bitmapBuffer", null)
        val bitmapBuffer = bitmapBufferString?.fromBase64String()

        return Triple(width, height, bitmapBuffer)
    }

    fun setRGBA(rgba: IntArray){
        val intArrayString = Gson().toJson(rgba)
        val editor = sharedPreferences.edit()
        editor.putString("rgbaString", intArrayString)
        editor.apply()
    }

    fun getRGBA(): IntArray?{
        val intArrayString = sharedPreferences.getString("rgbaString", null)
        return if (intArrayString != null) {
            Gson().fromJson(intArrayString, IntArray::class.java)
        } else {
            null
        }
    }


    // Extension functions for Bitmap to convert to/from Base64 String
    private fun Bitmap.toBase64String(): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun String.fromBase64String(): Bitmap? {
        val decodedBytes = Base64.decode(this, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}

