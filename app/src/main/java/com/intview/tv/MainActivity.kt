package com.intview.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

/** A deliberately interface-free, remote-controlled YouTube channel surfer. */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val PLAYER_URL =
            "https://raunakpatil.github.io/InterdimentionalCable/"

        private val HOSTED_PLAYER_SETUP = """
            (() => {
              const mount = () => {
                const target = document.getElementById('yt-player');
                if (!target || typeof powerOn !== 'function') return false;
                document.body.appendChild(target);
                const style = document.createElement('style');
                style.textContent = `
                  html, body { margin: 0 !important; overflow: hidden !important; background: #000 !important; }
                  body > *:not(#yt-player) { display: none !important; }
                  #yt-player, #yt-player iframe {
                    display: block !important; position: fixed !important; inset: 0 !important;
                    width: 100vw !important; height: 100vh !important; margin: 0 !important;
                    padding: 0 !important; border: 0 !important; z-index: 2147483647 !important;
                  }
                `;
                document.head.appendChild(style);
                window.togglePlayback = () => {
                  if (typeof player === 'undefined' || !player) return;
                  player.getPlayerState() === YT.PlayerState.PLAYING
                    ? player.pauseVideo() : player.playVideo();
                };
                window.play = () => {
                  if (typeof player !== 'undefined' && player) player.playVideo();
                };
                window.pause = () => {
                  if (typeof player !== 'undefined' && player) player.pauseVideo();
                };
                if (typeof isPoweredOn === 'undefined' || !isPoweredOn) powerOn();
                return true;
              };
              if (!mount()) {
                const retry = setInterval(() => { if (mount()) clearInterval(retry); }, 250);
                setTimeout(() => clearInterval(retry), 10000);
              }
            })();
        """.trimIndent()
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

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (url.startsWith(PLAYER_URL)) {
                        view.evaluateJavascript(HOSTED_PLAYER_SETUP, null)
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
