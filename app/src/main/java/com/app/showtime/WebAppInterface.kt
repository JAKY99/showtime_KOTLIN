package com.app.showtime

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WebAppInterface(
    private val mContext: Context,
    private val mainActivity: MainActivity
) {

    @SuppressLint("JavascriptInterface")
    @JavascriptInterface
    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotification(title: String, message: String,type: String) {
        val notificationMedia = when (type) {
            "info" -> R.raw.notif1
            "warning" -> R.raw.notif2
            "error" -> R.raw.notif3
            else -> R.raw.notif1
        }
// Create a PendingIntent to launch your app's main activity
        val intent = Intent(mContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val chanel: NotificationChannel = NotificationChannel("1","1", NotificationManager.IMPORTANCE_HIGH)
        val notificationManager = mContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        val notification =  NotificationCompat.Builder(mContext, "1")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_baseline_circle_notifications_24)
            .setLargeIcon(BitmapFactory.decodeResource(mContext.resources,
                R.drawable.ic_launcher_fore
            )) // Set custom icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // clear notification after click
            .build()
        with(notificationManager){
            createNotificationChannel(chanel)
            notify(1,notification)
        }
        val mediaPlayer = MediaPlayer.create(mContext,notificationMedia)
        mediaPlayer.start()
    }
    @JavascriptInterface
    fun updateVariableForCrop(bearerToken : String,userEmail : String , uploadUrl : String) {
        // Update the variable in Kotlin with the name of the input element
//            this.mainActivity.idInputTypeFile = name
        this.mainActivity.userMail = userEmail
        this.mainActivity.bearerToken = bearerToken
        this.mainActivity.uploadUrl = uploadUrl


        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (ContextCompat.checkSelfPermission(this.mainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.mainActivity, permissions, this.mainActivity.REQUEST_READ_EXTERNAL_STORAGE)
        } else {
            this.mainActivity.selectTempFileForCrop(this.mainActivity)
        }
    }
    @JavascriptInterface
    fun toggleVocalSearch() {
        GlobalScope.launch(Dispatchers.Main) {
            this@WebAppInterface.mainActivity.vocalCommand()
        }
    }
    @JavascriptInterface
    fun updateApp() {
        GlobalScope.launch(Dispatchers.Main) {
            this@WebAppInterface.mainActivity.updateNoCacheWebview()
        }
    }
}