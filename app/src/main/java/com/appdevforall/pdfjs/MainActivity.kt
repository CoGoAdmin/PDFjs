package com.appdevforall.pdfjs
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // Configure WebView settings
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // Enable JavaScript for PDF.js to work
        webSettings.allowFileAccess = true // Allow access to local files (assets)
        webSettings.domStorageEnabled = true // Enable DOM storage for PDF.js
        webSettings.builtInZoomControls = true // Enable built-in zoom controls
        webSettings.displayZoomControls = true // Hide the zoom controls on screen
        webSettings.loadWithOverviewMode = false // Load content in overview mode, fitting to screen
        webSettings.useWideViewPort = false // Use a wide viewport

        // Set a WebViewClient to handle page navigation within the WebView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // You can add any post-load logic here if needed
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                // Handle errors during page loading
                // For debugging, you might log these errors:
                Log.e("WebViewError", "Error: $description, URL: $failingUrl")
            }
        }

        // Set a WebChromeClient to handle JavaScript alerts, prompts, and console messages
        webView.webChromeClient = object : WebChromeClient() {
            // This is useful for debugging JavaScript errors in PDF.js
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    // Log console messages from the WebView to Logcat
                    Log.d("WebViewConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Construct the URL to load PDF.js viewer with the local PDF file
        // The path assumes 'viewer.html' is in 'assets/pdfjs/web/'
        // and 'sample.pdf' is directly in 'assets/'
        val pdfUrl = "file:///android_asset/pdfjs/web/viewer.html?file=" +
                "file:///android_asset/output.pdf"

        // Load the URL into the WebView
        webView.loadUrl(pdfUrl)
    }

    // Handle back button press to navigate within the WebView history
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
