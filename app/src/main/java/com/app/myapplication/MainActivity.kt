package com.app.myapplication

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.util.*
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.webView = findViewById(R.id.webView);
        this.webView.settings.javaScriptEnabled = true
        this.webView.webViewClient = WebViewClient()
        this.webView.settings.domStorageEnabled = true
        this.webView.settings.allowFileAccess = true
        this.webView.settings.allowContentAccess = true
        this.webView.addJavascriptInterface(WebAppInterface(this), "Android")
        this.webView.loadUrl("http://10.0.2.2:4201")
        // The following lines connects the Android app to the server.
    }



    class WebAppInterface(
        private val mContext: Context,
    ) {

        @SuppressLint("JavascriptInterface")
        @JavascriptInterface
        @RequiresApi(Build.VERSION_CODES.O)
        fun createNotification(title: String, message: String) {
            val chanel:NotificationChannel = NotificationChannel("1","1",NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notification =  NotificationCompat.Builder(mContext, "1")
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build()
            with(notificationManager){
                createNotificationChannel(chanel)
                notify(1,notification)
            }
        }

    }

   public override fun  onPause() {
       super.onPause();
    }
    public override fun onDestroy() {
        super.onDestroy();
        print("destroyed")
    }
    fun  handleGoBack() {
        if (this.webView.canGoBack()) {
            this.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    @Override
    override fun onBackPressed() {
        this.handleGoBack()
    }
}
