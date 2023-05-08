package com.app.showtime
//import io.socket.engineio.parser.Base64.encodeToString
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.*
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64.*
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {
    public var userMail: String? = null
    public var bearerToken: String? = null
    public var uploadUrl: String? = null
    public var countTest : Int = 0
    val env = "prod"
    public val FILE_CHOOSER_REQUEST_CODE = 1
    public val FILE_CHOOSER_REQUEST_CODE_FOR_CROP = 6
    public val REQUEST_READ_EXTERNAL_STORAGE = 2
    public val READ_STORAGE_PERMISSION_REQUEST_CODE = 1
    public val SPEECH_REQUEST_CODE = 3
    public val SPEECH_REQUEST_COMMAND_CODE = 4
    public val REQUEST_RECORD_AUDIO_PERMISSION = 5
    public var filePathCallback: ValueCallback<Array<Uri>>? = null
    public var idInputTypeFile = ""


    lateinit var webView: WebView
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
        this.checkLastVersionApp()
        // Check for storage permission
        this.webView = findViewById(R.id.webView);
        this.webView.settings.javaScriptEnabled = true
        this.webView.addJavascriptInterface(WebAppInterface(this, this), "Android")
        this.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                this@MainActivity.webView.post {
                    this@MainActivity.webView.evaluateJavascript("localStorage.setItem('isAndroid', 'true' )", null)
                }
            }
            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                println("errorResponse: ${errorResponse.toString()}")
            }
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val errorPage = URL("file:///android_asset/error_page.html")
                webView.loadUrl(errorPage.toString())
                this@MainActivity.checkWebAccess()
            }
        }
        this.webView.webChromeClient = object : WebChromeClient() {

        }
        this.webView.settings.domStorageEnabled = true
//        this.webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        this.webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        this.webView.settings.allowFileAccess = true
        this.webView.settings.allowContentAccess = true
        this.webView.settings.userAgentString  = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.5615.48 Mobile Safari/537.36"
        this.webView.settings.javaScriptCanOpenWindowsAutomatically = true
        this.webView.settings.javaScriptEnabled = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        this.checkWebAccess()
//        this.kafkaListenerContainer()
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
    override fun onResume() {
        super.onResume()
        this.checkLastVersionApp()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Override configuration change to prevent screen rotation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    fun updateNoCacheWebview() {
        val builder = AlertDialog.Builder(this@MainActivity)

        val message = "Reloading the app , please wait"
        builder.setTitle("Update")
        builder.setMessage(message)
        builder.setCancelable(false)
        val alert = builder.create()
        alert.show()
        this.webView.post {
            this.webView.clearCache(true)
            this.webView.clearHistory()
//            this.webView.loadUrl(urlToUse)
        }

        this@MainActivity.webView.evaluateJavascript("""
                (function() {
                    location.reload(true);
                })();
                """.trimIndent()) { value -> println(value)
//            val timer0 = Timer()
//            val task0 = object : TimerTask() {
//                override fun run() {
//                    alert.setTitle("Update")
//                    alert.setMessage("Reload complete , thanks for your patience!")
//                }
//            }
//            timer0.schedule(task0, 3000)
            val timer = Timer()
            val task = object : TimerTask() {
                override fun run() {
                    alert.dismiss()
                }
            }
            timer.schedule(task, 5000)
        }

    }

    fun selectFile(activity: Activity) {

        // Create an intent to open the file picker
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"

        // Set the title of the file picker
//        intent.putExtra(Intent.EXTRA_TITLE, "Select a file")
        // Only allow the user to select a single file
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        val chooserIntent = Intent.createChooser(intent, "Select a file")
        // Start the activity and wait for a result
        activity.startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE)
    }
    fun selectTempFileForCrop(activity: Activity) {

        // Create an intent to open the file picker
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"

        // Set the title of the file picker
//        intent.putExtra(Intent.EXTRA_TITLE, "Select a file")
        // Only allow the user to select a single file
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        val chooserIntent = Intent.createChooser(intent, "Select a file")
        // Start the activity and wait for a result
        activity.startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE_FOR_CROP)
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
                this.selectTempFileForCrop(this)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE && resultCode == Activity.RESULT_OK) {
            this.showFilePicker(this)
        }
//        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            val fileUri = data?.data
//            if (fileUri != null) {
//                val builder = AlertDialog.Builder(this@MainActivity)
//                if(uploadUrl=="/api/v1/user/uploadBackgroundPicture"){
//                    builder.setTitle("Update background picture")
//                }
//                if(uploadUrl=="/api/v1/user/uploadProfilePicture"){
//                    builder.setTitle("Update profile picture")
//                }
//                val message = "Upload in progress please wait ..."
//                builder.setMessage(message)
//                builder.setCancelable(false)
//                val alert = builder.create()
//                uploadFileToAws(fileUri,alert)
//            }
//// Do something with the file URI
//        }
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                val spokenText = result[0]
                if (spokenText.equals("OK showtime", true)) {
                    startVoiceRecognition()
                }
            }
        }
        if(requestCode == REQUEST_RECORD_AUDIO_PERMISSION && resultCode == Activity.RESULT_OK){
            vocalCommand()
        }
        if (requestCode == FILE_CHOOSER_REQUEST_CODE_FOR_CROP && resultCode == Activity.RESULT_OK) {
            val fileUri = data?.data
            if (fileUri != null) {
                val builder = AlertDialog.Builder(this@MainActivity)
                if(uploadUrl=="/api/v1/user/uploadBackgroundPicture"){
                    builder.setTitle("Loading preview for background picture")
                }
                if(uploadUrl=="/api/v1/user/uploadProfilePicture"){
                    builder.setTitle("Loading preview for profile picture")
                }
                val message = "Upload in progress please wait ..."
                builder.setMessage(message)
                builder.setCancelable(false)
                val alert = builder.create()
                uploadFileToWebview(fileUri,alert)
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
    fun uploadFileToWebview(fileUri: Uri,alertDialog: AlertDialog) :Int {
        alertDialog.show()
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
                GlobalScope.launch(Dispatchers.Main) {
                    alertDialog.setTitle("Preview loading status")
                    alertDialog.setMessage("failed")
                    alertDialog.setCancelable(true)
                }

                isImageUpload = "false"
                e.printStackTrace()
                println("Failed to execute request")
            }
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call, response: Response) {
                GlobalScope.launch(Dispatchers.Main) {

                    if(uploadUrl=="/api/v1/user/tempForCrop/uploadBackgroundPicture"){
                        this@MainActivity.webView.evaluateJavascript("""
                    (function() {
                       document.getElementById("callbackUploadTempCropHandler").value="background"
                       document.getElementById("callbackUploadTempCropHandler").dispatchEvent(new Event('change'));
                    })();
                    """.trimIndent()) {
                                value -> println(value)
                            alertDialog.setTitle("Preview loading")
                            alertDialog.setMessage("Ready")
                            val timer = Timer()
                            val task = object : TimerTask() {
                                override fun run() {
                                    alertDialog.dismiss()
                                }
                            }
                            timer.schedule(task, 2000)
                        }
                    }
                    if(uploadUrl=="/api/v1/user/tempForCrop/uploadProfilePicture"){
                        this@MainActivity.webView.evaluateJavascript("""
                    (function() {
                        document.getElementById("callbackUploadTempCropHandler").value="avatar"
                        document.getElementById("callbackUploadTempCropHandler").dispatchEvent(new Event('change'));
                    })();
                    """.trimIndent()) { value -> println(value)
                            alertDialog.setTitle("Preview loading")
                            alertDialog.setMessage("Ready")
                            val timer = Timer()
                            val task = object : TimerTask() {
                                override fun run() {
                                    alertDialog.dismiss()
                                }
                            }
                            timer.schedule(task, 2000)
                        }
                    }
                }

                isImageUpload = "true"
                println(response.body()?.string())
            }

        })





        return 1

    }

    fun ByteArray.encodeBase64(): String {
        return Base64.getEncoder().encodeToString(this)
    }
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun uploadFileToAws(fileUri : Uri , alertDialog: AlertDialog ): Int {
        alertDialog.show()
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
                GlobalScope.launch(Dispatchers.Main) {
                    alertDialog.setTitle("Upload status")
                    alertDialog.setMessage("failed")
                    alertDialog.setCancelable(true)
                }

                isImageUpload = "false"
                e.printStackTrace()
                println("Failed to execute request")
            }
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call, response: Response) {
                GlobalScope.launch(Dispatchers.Main) {

                    if(uploadUrl=="/api/v1/user/uploadBackgroundPicture"){
                        this@MainActivity.webView.evaluateJavascript("""
                    (function() {
                       document.getElementById("background-upload-input-android").dispatchEvent(new Event('change'));
                    })();
                    """.trimIndent()) {
                                value -> println(value)
                            alertDialog.setTitle("Upload status")
                            alertDialog.setMessage("Done")
                            val timer = Timer()
                            val task = object : TimerTask() {
                                override fun run() {
                                    alertDialog.dismiss()
                                }
                            }
                            timer.schedule(task, 2000)
                        }
                    }
                    if(uploadUrl=="/api/v1/user/uploadProfilePicture"){
                        this@MainActivity.webView.evaluateJavascript("""
                    (function() {
                        document.getElementById("avatar-upload-input-android").dispatchEvent(new Event('change'));
                    })();
                    """.trimIndent()) { value -> println(value)
                            alertDialog.setTitle("Upload status")
                            alertDialog.setMessage("Done")
                            val timer = Timer()
                            val task = object : TimerTask() {
                                override fun run() {
                                    alertDialog.dismiss()
                                }
                            }
                            timer.schedule(task, 2000)
                        }
                    }
                }

                isImageUpload = "true"
                println(response.body()?.string())
            }

        })





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
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun checkApiAccess(fileUri : Uri): Int {
        val uploadUrlRequest  : String = apiUrlToUse+"/api/v1/health/check"
        val client = OkHttpClient()
        var status = "none"
        val request = Request.Builder()
            .url(uploadUrlRequest)
            .build()
        val response = client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                status = "false"
                println("Failed to execute request")

            }
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call, response: Response) {
               var ResponseType : HealthCheckStatus = response.body()?.string()?.let { Gson().fromJson(it, HealthCheckStatus::class.java) }!!
                if(ResponseType.status=="OK"){
//                    uploadFileToAws(fileUri)
                }
                if(ResponseType.status!=="OK"){
//                    status = "false"
                }
            }

        })
        return 0

    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun checkWebAccess(): Void? {
        val loadingPage = URL("file:///android_asset/default_loading_page.html")
        webView.loadUrl(loadingPage.toString())
        var checkUrlRequest  : String = urlToUse
        val client = OkHttpClient()
        var status = "none"
        val request = Request.Builder()
            .url(checkUrlRequest)
            .get()
            .build()
        val response = client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                status = "false"
                e.printStackTrace()
                println("Failed to execute request")
                GlobalScope.launch(Dispatchers.Main) {
                    status = "false"
                    val errorPage = URL("file:///android_asset/error_page.html")
                    webView.loadUrl(errorPage.toString())
                }

            }
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call, response: Response) {
                response.code().let { println(it) }
                GlobalScope.launch(Dispatchers.Main) {
                    if(response.code()==200){
                        status = "true"
                        webView.loadUrl(checkUrlRequest)
                    }
                    if(response.code()!==200){
                        status = "false"
                        val errorPage = URL("file:///android_asset/error_page.html")
                        webView.loadUrl(errorPage.toString())
                    }
                }

            }

        })

        return null
    }

    class HealthCheckStatus {
        var status = "OK"
    }
    class versioncheck{
        val versionCode : String =""
        val versionName : String =""
    }
    fun startVoiceRecognition() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        this.startActivityForResult(speechIntent, SPEECH_REQUEST_COMMAND_CODE)
    }

   fun vocalCommand(){
       val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
       val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
       vibrator.vibrate(vibrationEffect)
       if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
           ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
       }else{
           val builder = AlertDialog.Builder(this@MainActivity)
           builder.setTitle("Vocal Command")
           val message = "Speak now"
           builder.setMessage(message)
           val alert = builder.create()
           alert.show()
           val recognizer = SpeechRecognizer.createSpeechRecognizer(this@MainActivity)
           val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
           recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//           recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
//           recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
           recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk now")
           recognizer.setRecognitionListener(object : RecognitionListener {
               @RequiresApi(Build.VERSION_CODES.KITKAT)
               override fun onResults(results: Bundle) {
                   alert.dismiss()
                   val result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                   if (result != null && result.isNotEmpty()) {
                       val spokenText = result[0].replace(" ", "")
                       val spokenTextSearch = result[0].lowercase()
                       println(spokenTextSearch)
                       if(spokenTextSearch.contains("search")){
                           val search = spokenTextSearch.replace("search","")
                           this@MainActivity.webView.post {
                               this@MainActivity.webView.evaluateJavascript("""
                                    (function() {
                                       document.getElementById('navbar-explore-btn').dispatchEvent(new Event('click') );
                                       document.getElementsByClassName('ng-fa-icon search-icon-action')[0].dispatchEvent(new Event('click') )
                                       document.getElementsByClassName('search-input-container')[0].getElementsByTagName('input')[0].value = '$search'
                                       document.getElementsByClassName('search-input-container')[0].getElementsByTagName('input')[0].dispatchEvent(new Event('input') )
                                       document.getElementsByClassName('main-search')[0].getElementsByTagName('form')[0].dispatchEvent(new Event('submit') )                                                                      
                                    })();
                                    """.trimIndent()) { value -> println(value) }
                           }
//

                       }
                       if (spokenText.contains("profile", true)||spokenText.contains("profil", true)) {
                           this@MainActivity.webView.post {
                               this@MainActivity.webView.evaluateJavascript("""
                                    (function() {
                                       document.getElementById('navbar-profil-btn').dispatchEvent(new Event('click') );
                                    })();
                                    """.trimIndent()) { value -> println(value) }
                           }
                       }
                       if (spokenText.contains("series", true)||spokenText.contains("série", true)||spokenText.contains("cerise", true)||spokenText.contains("show", true)) {
                           this@MainActivity.webView.post {
                               this@MainActivity.webView.evaluateJavascript("""
                                    (function() {
                                       document.getElementById('navbar-series-btn').dispatchEvent(new Event('click') );
                                    })();
                                    """.trimIndent()) { value -> println(value) }
                           }
                       }
                       if (spokenText.contains("movies", true)) {
                           this@MainActivity.webView.post {
                               this@MainActivity.webView.evaluateJavascript("""
                                    (function() {
                                       document.getElementById('navbar-movies-btn').dispatchEvent(new Event('click') );
                                    })();
                                    """.trimIndent()) { value -> println(value) }
                           }
                       }
                       if (spokenText.contains("social", true)) {
                           this@MainActivity.webView.post {
                               this@MainActivity.webView.evaluateJavascript("""
                                    (function() {
                                       document.getElementById('navbar-social-btn').dispatchEvent(new Event('click') );
                                    })();
                                    """.trimIndent()) { value -> println(value) }
                           }
                       }
                       if (spokenText.contains("home", true)||spokenText.contains("explore", true)) {
                           this@MainActivity.webView.post {
                               this@MainActivity.webView.evaluateJavascript("""
                                (function() {
                                   document.getElementById('navbar-explore-btn').dispatchEvent(new Event('click') )
                                })();
                                """.trimIndent()) { value -> println(value) }
                           }
                       }
                       this@MainActivity.webView.post {
                           this@MainActivity.webView.evaluateJavascript("""
                                (function() {
                                    document.getElementById('navbar-vocal-btn')?.classList.remove('navbar-vocal-btn-pulse');
                                })();
                                """.trimIndent()) { value -> println(value) }
                       }
                   }

               }

               @RequiresApi(Build.VERSION_CODES.KITKAT)
               override fun onPartialResults(p0: Bundle?) {
                   alert.dismiss()
                   this@MainActivity.webView.post {
                       this@MainActivity.webView.evaluateJavascript("""
                                (function() {
                                   document.getElementById('navbar-vocal-btn')?.classList.remove('navbar-vocal-btn-pulse');
                                })();
                                """.trimIndent()) { value -> println(value) }
                   }
                   Log.d("Speech", "onPartialResults")
               }

               override fun onEvent(p0: Int, p1: Bundle?) {
                   Log.d("Speech", "onEvent")
               }

               @RequiresApi(Build.VERSION_CODES.KITKAT)
               override fun onError(error: Int) {
                   alert.dismiss()
                   this@MainActivity.webView.post {
                       this@MainActivity.webView.evaluateJavascript("""
                                (function() {
                                    document.getElementById('navbar-vocal-btn')?.classList.remove('navbar-vocal-btn-pulse');
                                })();
                                """.trimIndent()) { value -> println(value) }
                   }
                   when (error) {
                       SpeechRecognizer.ERROR_AUDIO -> Log.e("SpeechRecognizer", "Error audio")
                       SpeechRecognizer.ERROR_CLIENT -> Log.e("SpeechRecognizer", "Error client")
                       SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Log.e(
                           "SpeechRecognizer",
                           "Error insufficient permissions"
                       )
                       SpeechRecognizer.ERROR_NETWORK -> Log.e("SpeechRecognizer", "Error network")
                       SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Log.e(
                           "SpeechRecognizer",
                           "Error network timeout"
                       )
                       SpeechRecognizer.ERROR_NO_MATCH -> Log.e(
                           "SpeechRecognizer",
                           "Error no match"
                       )
                       SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Log.e(
                           "SpeechRecognizer",
                           "Error recognizer busy"
                       )
                       SpeechRecognizer.ERROR_SERVER -> Log.e("SpeechRecognizer", "Error server")
                       SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Log.e(
                           "SpeechRecognizer",
                           "Error speech timeout"
                       )
                   }
               }

               override fun onEndOfSpeech() {
                   Log.d("SpeechRecognizer", "End of speech")
               }

               override fun onReadyForSpeech(params: Bundle?) {
                   Log.d("SpeechRecognizer", "Ready for speech")
               }

               override fun onBeginningOfSpeech() {
                     Log.d("SpeechRecognizer", "Beginning of speech")
               }

               override fun onRmsChanged(rmsdB: Float) {}
               override fun onBufferReceived(buffer: ByteArray?) {}
           })
           recognizer.startListening(recognizerIntent)
       }

   }
    fun checkLastVersionApp(){
        val uploadUrlRequest  : String = apiUrlToUse+"/api/v1/version/android/information"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(uploadUrlRequest)
            .build()
        val response = client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                GlobalScope.launch(Dispatchers.Main) {
                    println("Failed to execute request")
                }

            }
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call, response: Response) {
                var ResponseType : versioncheck = response.body()?.string()?.let { Gson().fromJson(it, versioncheck::class.java) }!!
                var currentVersion = BuildConfig.VERSION_CODE.toString()
                var lastVersion = ResponseType.versionCode
                if(ResponseType.versionCode!=="" && lastVersion != currentVersion){
                    GlobalScope.launch(Dispatchers.Main) {
                        AlertDialog.Builder(this@MainActivity)
                            .setMessage("A new version of the app is available. Please update to continue using the app.")
                            .setPositiveButton("Update") { _, _ ->
                                // Open the Google Play Store to download the latest version of the app
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("market://details?id=${this@MainActivity.packageName}")
                                startActivity(intent)
                            }
                            .setNegativeButton("Cancel") { _, _ ->
                                // Close the app if the user chooses not to update
                                this@MainActivity.finish()
                            }
                            .setCancelable(false) // Prevent the user from dismissing the dialog
                            .create()
                            .show()
                    }

                }

            }

        })

    }
}


