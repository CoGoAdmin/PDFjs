package com.appdevforall.pdfjs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.io.InputStream
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var webServer: WebServer? = null

    // Define the port for our local HTTP server
    private val SERVER_PORT = 8888
    // Define the host for our local HTTP server
    private val SERVER_HOST = "127.0.0.1"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // Initialize and start the local web server
        try {
            webServer = WebServer(applicationContext, SERVER_HOST, SERVER_PORT)
            webServer?.start()
            // Log a message indicating the server has started
            // Log.d("WebServer", "Local server started on http://$SERVER_HOST:$SERVER_PORT")
        } catch (e: IOException) {
            // Handle server startup error
            // Log.e("WebServer", "Failed to start local web server: ${e.message}")
            // Consider showing an error message to the user or retrying
        }

        // Configure WebView settings
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // Enable JavaScript for PDF.js to work
        webSettings.allowFileAccess = true // Allow access to local files (assets)
        webSettings.domStorageEnabled = true // Enable DOM storage for PDF.js
        webSettings.builtInZoomControls = true // Enable built-in zoom controls
        webSettings.displayZoomControls = false // Hide the zoom controls on screen
        webSettings.loadWithOverviewMode = true // Load content in overview mode, fitting to screen
        webSettings.useWideViewPort = true // Use a wide viewport
        // Allow content to access files from any origin, crucial for local server
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.allowFileAccessFromFileURLs = true


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
        // The PDF.js viewer.html will be served from our local server,
        // and it will then request the PDF file, also from the local server.
        // The 'file' parameter in viewer.html expects a URL, which our server provides.
//JMT        val pdfUrl = "http://$SERVER_HOST:$SERVER_PORT/pdfjs/web/viewer.html?file=../test/pdfs/160F-2019.pdf"
        val filename =
            /*"widget_hidden_print.pdf"*/
            /*"JavaNotesForProfessionals.pdf"*/
            "compressed.tracemonkey-pldi-09.pdf"
        val pdfUrl = "http://$SERVER_HOST:$SERVER_PORT/pdfjs/web/viewer.html?file=$filename"
//JMT               +  "#scaleddecimalforms"

        // Load the URL into the WebView
        webView.loadUrl(pdfUrl)
    }

    // Stop the local web server when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        webServer?.stop()
        // Log a message indicating the server has stopped
        // Log.d("WebServer", "Local server stopped.")
    }

    // Handle back button press to navigate within the WebView history
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * A simple local HTTP server that serves files from the Android app's assets folder.
     * This helps in avoiding CORS issues when loading local files in WebView.
     */
    class WebServer(private val context: Context, hostname: String, port: Int) : NanoHTTPD(hostname, port) {

        override fun serve(session: IHTTPSession): Response {
            // Get the requested URI (e.g., "/pdfjs/web/viewer.html", "/sample.pdf")
            var uri = session.uri

            // Remove leading slash if present for asset manager
            if (uri.startsWith("/")) {
                uri = uri.substring(1)
            }

            // Handle the case where the request is for the root path,
            // default to viewer.html if assets/pdfjs/web/viewer.html exists
            if (uri.isEmpty() || uri == "/") {
                uri = "pdfjs/web/viewer.html"
            }

            var inputStream: InputStream? = null
            var mimeType: String = getMimeType(uri) // Determine MIME type based on file extension

            try {
                // Open the file from assets
                inputStream = context.assets.open(uri)
                val response = newFixedLengthResponse(Response.Status.OK, mimeType, inputStream, inputStream.available().toLong())
                // Add necessary headers to prevent caching and allow range requests for PDFs
                response.addHeader("Accept-Ranges", "bytes")
                response.addHeader("Access-Control-Allow-Origin", "*") // Crucial for CORS
                return response
            } catch (e: IOException) {
                // If file not found or other IO error, return a 404 response
                // Log.e("WebServer", "Error serving file $uri: ${e.message}")
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Error 404: File not found.")
            } finally {
                // NanoHTTPD manages closing the input stream for newFixedLengthResponse
                // if it's a FileInputStream. For asset streams, it's generally safe to let
                // NanoHTTPD handle it, but explicitly closing here is also an option
                // if you were not passing it directly to newFixedLengthResponse.
            }
        }

        /**
         * Determines the MIME type based on the file extension.
         * This is a simplified version; a more robust solution might use MimeTypeMap.
         */
        private fun getMimeType(uri: String): String {
            return when {
                uri.endsWith(".html") -> "text/html"
                uri.endsWith(".ftl") -> "text/html"
                uri.endsWith(".css") -> "text/css"
                uri.endsWith(".js") -> "application/javascript"
                uri.endsWith(".mjs") -> "application/javascript"
                uri.endsWith(".pdf") -> "application/pdf"
                uri.endsWith(".wasm") -> "application/wasm"
                uri.endsWith(".png") -> "image/png"
                uri.endsWith(".ico") -> "image/png"
                uri.endsWith(".jpg") || uri.endsWith(".jpeg") -> "image/jpeg"
                uri.endsWith(".gif") -> "image/gif"
                uri.endsWith(".svg") -> "image/svg+xml"
                uri.endsWith(".json") -> "application/json"
                uri.endsWith(".woff") -> "font/woff"
                uri.endsWith(".woff2") -> "font/woff2"
                uri.endsWith(".ttf") -> "font/ttf"
                uri.endsWith(".otf") -> "font/otf"
                uri.endsWith(".pfb") -> "application/x-font-type1"
                else -> "application/octet-stream" // Default for unknown types
            }
        }
    }
}
