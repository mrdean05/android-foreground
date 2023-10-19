package com.example.foregroundservice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {

    private val CHANNEL_ID = "ServiceChannel"
    private var cameraFragment: CameraFragment? = null
    private var aiResult: AIResult? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Create an explicit intent for what you want to start
        val notificationIntent = Intent(this, MainActivity::class.java)

        // Create a PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build your notification, attaching the PendingIntent
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Drover App")
            .setContentText("Running...")
            .setContentIntent(pendingIntent) // Attach the PendingIntent
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Keep your existing icon or replace as needed
            .build()

        startForeground(1, notification)

        //val activityIntent = Intent(this, MainActivity::class.java)
        //activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  // This flag is necessary to start an activity from a non-activity context.
        //startActivity(activityIntent)

        //println("Foreground Service has began")

        //cameraFragment = CameraFragment()
        //val aiResult = AIResult(this)


        // Call the turnThirdPartyAlgorithmAccessSwitch function
        //aiResult?.turnThirdPartyAlgorithmAccessSwitch()

        // Bind the CameraFragment to the service
        //cameraFragment?.bindCamera(this)

        //val activityIntent = Intent(this, MainActivity::class.java)
        //activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  // This flag is necessary to start an activity from a non-activity context.
        //startActivity(activityIntent)

        return START_NOT_STICKY
    }

    fun setCameraFragment(fragment: CameraFragment) {
        this.cameraFragment = fragment
    }

    fun setAIResult(aiResult: AIResult) {
        this.aiResult = aiResult
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }
}
