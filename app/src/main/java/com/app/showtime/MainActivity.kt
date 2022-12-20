package com.app.showtime
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.app.showtime.R
import okhttp3.*
import java.net.URL
import java.util.*

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
        this.webView.loadUrl("https://dev.showtime-app.click/home")
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        val connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // network is available for use
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                println("Network is available")

                webView.post { webView.loadUrl("https://dev.showtime-app.click/home") }
            }

            // Network capabilities have changed for the network
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }

            // lost network connection
            override fun onLost(network: Network) {
                super.onLost(network)
                val errorPage = URL("file:///android_asset/error_page.html")
                webView.post { webView.loadUrl(errorPage.toString()) }
            }
        }
        connectivityManager.requestNetwork(networkRequest, networkCallback)
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
                .setSmallIcon(R.drawable.notification_icon)
                .build()
            with(notificationManager){
                createNotificationChannel(chanel)
                notify(1,notification)
            }
        }

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
