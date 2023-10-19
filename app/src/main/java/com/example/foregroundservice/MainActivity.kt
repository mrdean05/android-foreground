package com.example.foregroundservice

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //println("Get started.........")

        //val intentFilter = IntentFilter("com.example.ACTION_FROM_SERVICE")
        //registerReceiver(receiver, intentFilter)

        // Create a CameraFragment instance
        val cameraFragment = CameraFragment()

        // Create an AIResult instance
        val aiResult = AIResult(this)
        AIResult(context = applicationContext).turnThirdPartyAlgorithmAccessSwitch()

        // Call the turnThirdPartyAlgorithmAccessSwitch function
        //aiResult?.turnThirdPartyAlgorithmAccessSwitch()

        // Bind the CameraFragment to the service
        cameraFragment?.bindCamera(this)

        // Create a ForegroundService instance
        val foregroundService = ForegroundService()

        // Set the CameraFragment instance and AIResult instance in the ForegroundService
        foregroundService.setCameraFragment(cameraFragment)
        foregroundService.setAIResult(aiResult)

        // Start the ForegroundService
        val serviceIntent = Intent(this, ForegroundService::class.java)
        startService(serviceIntent)
    }
}