package com.intview.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/** A deliberately interface-free, remote-controlled YouTube channel surfer. */
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()
        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (url == "file:///android_asset/youtube_player.html") {
                        view.evaluateJavascript(
                            "window.setYoutubeApiKey(${JSONObject.quote(BuildConfig.YOUTUBE_API_KEY)});",
                            null
                        )
                    }
                }
            }
            loadUrl("file:///android_asset/youtube_player.html")
        }
        setContentView(webView)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> { runPlayer("changeChannel(1)"); true }
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { runPlayer("changeChannel(-1)"); true }
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { runPlayer("togglePlayback()"); true }
        KeyEvent.KEYCODE_MEDIA_PLAY -> { runPlayer("play()"); true }
        KeyEvent.KEYCODE_MEDIA_PAUSE -> { runPlayer("pause()"); true }
        else -> super.onKeyDown(keyCode, event)
    }
    private fun runPlayer(command: String) { webView.evaluateJavascript("$command;", null) }
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemUi() }
    override fun onDestroy() { webView.destroy(); super.onDestroy() }
}
