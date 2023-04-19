package com.app.showtime

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
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
    fun createNotification(title: String, message: String) {
        val chanel: NotificationChannel = NotificationChannel("1","1", NotificationManager.IMPORTANCE_HIGH)
        val notificationManager = mContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        val notification =  NotificationCompat.Builder(mContext, "1")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.notification_icon)
            .build()
        with(notificationManager){
            createNotificationChannel(chanel)
            notify(1,notification)
        }
    }
    @JavascriptInterface
    fun updateVariable(bearerToken : String,userEmail : String , uploadUrl : String) {
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
            this.mainActivity.selectFile(this.mainActivity)
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
    @JavascriptInterface
    fun onSwipeLeft() {
        println("onSwipeLeft")
    }

    @JavascriptInterface
    fun onSwipeRight() {
        // perform action on swipe right
        println("onSwipeRight")
    }

    @JavascriptInterface
    fun onSwipeUp() {
        // perform action on swipe up
        println("onSwipeUp")
    }

    @JavascriptInterface
    fun onSwipeDown() {
        // perform action on swipe down
        println("onSwipeDown")
    }
    @JavascriptInterface
    fun onMessageReceived(message: String) {
        // Handle the received message here
        Log.d("WebView", "Received message: $message")
    }
}