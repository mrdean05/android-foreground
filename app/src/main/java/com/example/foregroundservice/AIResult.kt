package com.example.foregroundservice

import android.content.Context
import android.os.RemoteException
import android.provider.Settings
import android.util.Log

import com.segway.robot.datatransmit.BindStateListener
import com.segway.robot.datatransmit.DataTransmitV1
import com.segway.robot.datatransmit.exception.DataTransmitUnbindException

/**
 * This class is wrapper to getting access to IoT functionalities
 */
class AIResult(private val context: Context) {

    fun turnThirdPartyAlgorithmAccessSwitch() {

        println("turnThirdPartyCalled")
        // Bind Service
        DataTransmitV1.getInstance().bindService(context, object : BindStateListener {
            override fun onBind() {
                try {
                    DataTransmitV1.getInstance().setDefaultAiSwitch(1)
                    Log.d(TAG, "UART Data Transmit Service bind")
                } catch (e: RemoteException){
                    Log.e(TAG, "UART Data Transmit Service bind error: ", e)
                } catch (e: DataTransmitUnbindException){
                    Log.e(TAG, "UART Data Transmit Service bind error: ", e)
                }
            }

            override fun onUnbind(reason: String) {
                Log.d(TAG, "UART Data Transmit Service Unbind")
            }
        })
    }

    fun setDetectedSidewalks() {
        try {
            UartData.sendAiResult(2, 0)
            Log.d(TAG, "Sidewalk Detected")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending sidewalk alert over UART: ", e)
        }
    }

    fun setDetectedNonSidewalks() {
        try {
            UartData.sendAiResult(1, 0)
            Log.d(TAG, "No Sidewalk Detected")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending non-sidewalk alert over UART: ", e)
        }
    }

    companion object {
        private const val TAG = "AIResult"
    }
}