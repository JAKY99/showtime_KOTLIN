package com.app.showtime
//import io.socket.engineio.parser.Base64.encodeToString
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64.*
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity() {
    private var userMail: String? = null
    private var bearerToken: String? = null
    private var uploadUrl: String? = null
    val env = "dev"
    private val FILE_CHOOSER_REQUEST_CODE = 1
    private val REQUEST_READ_EXTERNAL_STORAGE = 2
    private val READ_STORAGE_PERMISSION_REQUEST_CODE = 1
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var idInputTypeFile = ""
    private lateinit var webView: WebView
    object apiUrl{
        val local = "https://dev.showtime-app.click/api"
        val dev = "https://dev.showtime-app.click/api"
        val prod = "https://showtime-app.click/api"
    }
    object webviewURL {
        val local = "http://10.0.2.2:4201/home"
        val dev = "https://dev.showtime-app.click/home"
        val prod = "https://showtime-app.click/home"
    }
    val apiUrlToUse = when(env){
        "local" -> apiUrl.local
        "dev" -> apiUrl.dev
        "prod" -> apiUrl.prod
        else -> ""
    }
    val urlToUse = when (env) {
        "local" -> webviewURL.local
        "dev" -> webviewURL.dev
        "prod" -> webviewURL.prod
        else -> ""
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Check for storage permission
        this.webView = findViewById(R.id.webView);
        this.webView.settings.javaScriptEnabled = true
        this.webView.addJavascriptInterface(WebAppInterface(this, this), "Android")
        this.webView.webViewClient = object : WebViewClient() {}
        this.webView.webChromeClient = object : WebChromeClient() {}
        this.webView.settings.domStorageEnabled = true
        this.webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        this.webView.settings.allowFileAccess = true
        this.webView.settings.allowContentAccess = true
        this.webView.loadUrl(urlToUse)
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

                webView.post {
                    webView.loadUrl(urlToUse)
                }
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
        private val mainActivity: MainActivity
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
    }
    private fun selectFile(activity: Activity) {

        // Create an intent to open the file picker
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.type = "*/*"

        // Set the title of the file picker
        intent.putExtra(Intent.EXTRA_TITLE, "Select a file")
        // Only allow the user to select a single file
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        val chooserIntent = Intent.createChooser(intent, "Choose file")
        // Start the activity and wait for a result
        activity.startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.selectFile(this)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE && resultCode == Activity.RESULT_OK) {
            this.showFilePicker(this)
        }
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val fileUri = data?.data
            if (fileUri != null) {
                val filePath = fileUri.path
                val file = File(filePath)
                val fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString())
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
//                val fileContent = file.readBytes()
                val filename = file.name


                uploadFileToAws(fileUri)
            }
// Do something with the file URI
        }
    }
    fun showFilePicker(activity: Activity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "/"
        activity.startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
    }
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun uploadFileToAws(fileUri : Uri ): Int {
        var isImageUpload = "none"
        val path = getPath(applicationContext, fileUri)
        val file = File(path)
        val uploadUrlRequest  : String = apiUrlToUse+uploadUrl
        val client = OkHttpClient()
        val formBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("email", this.userMail)
            .addFormDataPart("file", file.name, RequestBody.create(MediaType.parse("multipart/form-data"), file))
            .build()
        val request = Request.Builder()
            .url(uploadUrlRequest)
            .addHeader("Authorization", "Bearer ${this.bearerToken}")
            .post(formBody)
            .build()
        val response = client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                isImageUpload = "false"
                e.printStackTrace()
                println("Failed to execute request")
            }
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call, response: Response) {
                isImageUpload = "true"
                println(response.body()?.string())
            }

        })


        if(uploadUrl=="/api/v1/user/uploadBackgroundPicture"){
            while(isImageUpload=="none"){
                Thread.sleep(1000)
            }
            this.webView.evaluateJavascript("""
                    (function() {
                       document.getElementById("background-upload-input-android").dispatchEvent(new Event('change'));
                    })();
                    """.trimIndent()) { value -> println(value) }
        }
        if(uploadUrl=="/api/v1/user/uploadProfilePicture"){
            this.webView.evaluateJavascript("""
                    (function() {
                        document.getElementById("avatar-upload-input-android").dispatchEvent(new Event('change'));
                    })();
                    """.trimIndent()) { value -> println(value) }
        }

        return 1

    }
    fun getPath(context: Context, uri: Uri?): String? {
        var result: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri!!, proj, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(proj[0])
                result = cursor.getString(column_index)
            }
            cursor.close()
        }
        if (result == null) {
            result = "Not found"
        }
        return result
    }


}


