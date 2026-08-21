package com.intview.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONObject
import kotlin.math.abs

/** A deliberately interface-free, remote-controlled YouTube channel surfer. */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val PLAYER_URL =
            "https://appassets.androidplatform.net/assets/youtube_player.html"
    }

    private lateinit var webView: WebView
    private val gestures by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                runPlayer("togglePlayback()")
                return true
            }

            override fun onFling(
                first: MotionEvent?,
                second: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val start = first ?: return false
                val distanceX = second.x - start.x
                if (abs(distanceX) < 120 || abs(velocityX) < abs(velocityY)) return false
                runPlayer(if (distanceX < 0) "changeChannel(1)" else "changeChannel(-1)")
                return true
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: android.webkit.WebResourceRequest
                ): android.webkit.WebResourceResponse? =
                    assetLoader.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView, url: String) {
                    if (url == PLAYER_URL) {
                        view.evaluateJavascript(
                            "window.setYoutubeApiKey(${JSONObject.quote(BuildConfig.YOUTUBE_API_KEY)});",
                            null
                        )
                    }
                }
            }
            setOnTouchListener { _, event -> gestures.onTouchEvent(event) }
            loadUrl(PLAYER_URL)
        }
        setContentView(webView)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> {
            runPlayer("changeChannel(1)")
            true
        }
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
            runPlayer("changeChannel(-1)")
            true
        }
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
            runPlayer("togglePlayback()")
            true
        }
        KeyEvent.KEYCODE_MEDIA_PLAY -> {
            runPlayer("play()")
            true
        }
        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
            runPlayer("pause()")
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun runPlayer(command: String) {
        webView.evaluateJavascript("$command;", null)
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
