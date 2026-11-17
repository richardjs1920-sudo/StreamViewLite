package com.example.streamviewlite

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        // Load the JS we made earlier
        val script = assets.open("js/fake_camera.js").bufferedReader().readText()

        val web = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false // Critical for loop
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            // Serve the internal file to the browser via a fake URL
            addJavascriptInterface(object {
                @JavascriptInterface
                fun getVideoUrl(): String {
                    return File(filesDir, "fake_cam.mp4").absolutePath
                }
            }, "bridge")
            
            // Inject script immediately so the site sees it first
            addJavascriptInterface(object : java.io.Serializable {
                @JavascriptInterface
                fun run() {
                    evaluateJavascript(script, null)
                }
            }, "injector")
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    // Also inject on page start for insurance
                    view.evaluateJavascript(script, null)
                }
            }
        }
        findViewById<FrameLayout>(R.id.webContainer).addView(web)
        web.loadUrl("https://www.google.com") // Default test
    }
}
