package com.example.foregroundservice

import android.os.RemoteException
import com.segway.robot.datatransmit.DataTransmitV1
import com.segway.robot.datatransmit.exception.DataTransmitUnbindException
import com.segway.robot.datatransmit.utils.NativeByteBuffer

object UartData {
    private const val TYPE_WHEEL = 1
    private const val TYPE_IOT = 2

    private val WHEEL_PARAM = intArrayOf(TYPE_WHEEL)
    private val WHEEL_LEN_PARAM = intArrayOf(2)
    private val IOT_PARAM = intArrayOf(TYPE_IOT)
    private val IOT_LEN_PARAM = intArrayOf(22)

    @Throws(DataTransmitUnbindException::class, RemoteException::class)
    fun sendAiResult(aiInferenceResult: Int, pedestrianDetected: Int) {
        val nativeData = NativeByteBuffer.obtain(1)
        nativeData.put(aiInferenceResult, 4)
        nativeData.put(pedestrianDetected, 1)
        DataTransmitV1.getInstance().sendData(nativeData.data)
        nativeData.recycle()
    }

    @Throws(DataTransmitUnbindException::class, RemoteException::class)
    fun getWheelData(): WheelData? {
        val data = DataTransmitV1.getInstance().getData(WHEEL_PARAM, WHEEL_LEN_PARAM)
        data?.let {
            if (it.isNotEmpty()) {
                val nativeData = NativeByteBuffer.obtain().wrap(it)
                val type = nativeData.byte
                val timestamp = nativeData.long
                val size = nativeData.byte
                val power = nativeData.byte
                val speed = nativeData.byte
                nativeData.recycle()
                if (type == TYPE_WHEEL) {
                    return WheelData(timestamp, speed)
                }
            }
        }
        return null
    }

    @Throws(DataTransmitUnbindException::class, RemoteException::class)
    fun getLocationData(): LocationData? {
        val data = DataTransmitV1.getInstance().getData(IOT_PARAM, IOT_LEN_PARAM)
        data?.let {
            if (it.isNotEmpty()) {
                val nativeData = NativeByteBuffer.obtain().wrap(it)
                val type = nativeData.byte
                val timestamp = nativeData.long
                val size = nativeData.byte
                val locTimestamp = nativeData.int * 1000000L
                val longitude = nativeData.int / 1000000.00
                val latitude = nativeData.int / 1000000.00
                val gpsAt = nativeData.int / 10.00
                val gpsHeading = nativeData.short / 10.00
                val gpsSpeed = nativeData.short / 10.00
                val gpsHdop = nativeData.short / 10.00
                nativeData.recycle()
                if (type == TYPE_IOT) {
                    return LocationData(locTimestamp, longitude.toFloat(), latitude.toFloat(), gpsAt.toFloat(), gpsHeading.toFloat(), gpsSpeed.toFloat(), gpsHdop.toFloat())
                }
            }
        }
        return null
    }
}