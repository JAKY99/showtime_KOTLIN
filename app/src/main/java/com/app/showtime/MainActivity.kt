package com.app.showtime
//import io.socket.engineio.parser.Base64.encodeToString
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64.*
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.io.File
import java.io.IOException
import java.net.URL
import java.time.Duration
import java.util.*


class MainActivity : AppCompatActivity() {
    private var userMail: String? = null
    private var bearerToken: String? = null
    private var uploadUrl: String? = null
    private var countTest : Int = 0
    val env = "dev"
    private val FILE_CHOOSER_REQUEST_CODE = 1
    private val REQUEST_READ_EXTERNAL_STORAGE = 2
    private val READ_STORAGE_PERMISSION_REQUEST_CODE = 1
    private val SPEECH_REQUEST_CODE = 3
    private val SPEECH_REQUEST_COMMAND_CODE = 4
    private val REQUEST_RECORD_AUDIO_PERMISSION = 5
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var idInputTypeFile = ""
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
        this.webView.settings.allowFileAccess = true
        this.webView.settings.allowContentAccess = true
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
        @JavascriptInterface
        fun toggleVocalSearch() {
            GlobalScope.launch(Dispatchers.Main) {
                this@WebAppInterface.mainActivity.vocalCommand()
            }
        }
    }
    private fun selectFile(activity: Activity) {

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
                val builder = AlertDialog.Builder(this@MainActivity)
                if(uploadUrl=="/api/v1/user/uploadBackgroundPicture"){
                    builder.setTitle("Update background picture")
                }
                if(uploadUrl=="/api/v1/user/uploadProfilePicture"){
                    builder.setTitle("Update profile picture")
                }
                val message = "Upload in progress please wait ..."
                builder.setMessage(message)
                builder.setCancelable(false)
                val alert = builder.create()
                uploadFileToAws(fileUri,alert)
            }
// Do something with the file URI
        }
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
    }
    fun showFilePicker(activity: Activity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "/"
        activity.startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
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
            }
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call, response: Response) {
                response.code().let { println(it) }
                if(response.code()==200){
                    status = "true"
                }
                if(response.code()!==200){
                    status = "false"
                }
            }

        })
        while (status=="none"){
            Thread.sleep(1000)
        }
        if(status=="true"){
            webView.loadUrl(checkUrlRequest)
        }
        if(status=="false"){
            val errorPage = URL("file:///android_asset/error_page.html")
            webView.loadUrl(errorPage.toString())
        }
        return null
    }

    class HealthCheckStatus {
        var status = "OK"
    }
    fun startVoiceRecognition() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        this.startActivityForResult(speechIntent, SPEECH_REQUEST_COMMAND_CODE)
    }
//   @RequiresApi(Build.VERSION_CODES.S)
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
                       if (spokenText.contains("profile", true)) {
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
                       if (spokenText.contains("Openhome", true)||spokenText.contains("explore", true)) {
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
    fun kafkaListenerContainer(){
        val props = Properties()
        props["bootstrap.servers"] = "2-3-129-154-103:30010"
        props["group.id"] = "dev"
        props["enable.auto.commit"] = "true"
        props["auto.commit.interval.ms"] = "1000"
        props["session.timeout.ms"] = "30000"
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf("devUser"))
        while (true) {
            val records = consumer.poll(Duration.ofMillis(100))
            for (record in records) {
                Log.d("Kafka", "offset = ${record.offset()}, key = ${record.key()}, value = ${record.value()}")
            }
        }
    }
}


