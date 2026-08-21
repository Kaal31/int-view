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
                const staticOverlay = document.getElementById('static-overlay');
                if (staticOverlay) document.body.appendChild(staticOverlay);
                const style = document.createElement('style');
                style.textContent = `
                  html, body { margin: 0 !important; overflow: hidden !important; background: #000 !important; }
                  body > *:not(#yt-player):not(#static-overlay) { display: none !important; }
                  #yt-player, #yt-player iframe {
                    display: block !important; position: fixed !important; inset: 0 !important;
                    width: 100vw !important; height: 100vh !important; margin: 0 !important;
                    padding: 0 !important; border: 0 !important; z-index: 2147483646 !important;
                    min-width: 0 !important; min-height: 0 !important; max-width: none !important;
                    max-height: none !important; aspect-ratio: auto !important;
                    transform: none !important; opacity: 1 !important; visibility: visible !important;
                  }
                  #static-overlay {
                    display: block !important; position: fixed !important; inset: 0 !important;
                    width: 100vw !important; height: 100vh !important; z-index: 2147483647 !important;
                    pointer-events: none !important;
                  }
                  #static-canvas { width: 100% !important; height: 100% !important; }
                `;
                document.head.appendChild(style);

                // Keep the room and controls hidden, but preserve the original tuning
                // noise overlay and sound until the replacement video reaches PLAYING.
                window.startScreenGlitches = () => {};
                window.stopScreenGlitches = () => {};
                window.showChannelOSD = () => {};
                window.showCommercialBug = () => {};
                window.hideCommercialBug = () => {};


                // Extend the hosted project's active fallback vault with the user's
                // additional playlists. Keep existing entries and avoid weighting duplicates.
                const enrichedPlaylists = [
                  'PLS00S3Xq4XB3RNFwg8hPYgc_iW96zCHUN',
                  'PLtL91bVltahFoj-jnpxCKdpaE9iamuu-N',
                  'PLI-oiEIgb2QZJ4MXhkOO2Y8NZIVhp9sss',
                  'PLGUcOrs__IKoeYi8zpCOgZ3fj8eWV_sVW',
                  'PL3zdZqyGuXYLwkJob57od8CJyKJrwJMW-',
                  'PLu-XN9Jj7cvGXajaxAiJbRCI6wK_SfSw0',
                  'PLrHYjLk237Hvv45VUEvTpAf4kiF0rXuFt',
                  'PLlaScuy7yOlIKaWWZ1MYYF_WovUmrRtIw',
                  'PLIKUHNlLzewEvYpfePeO4Z-3893UytlEg',
                  'PLXKAG8g1Ls_Ax-SU7rCgyiGWjylB5NHL-',
                  'PLFF8A556FBD940D6B',
                  'PLJ50CyqqoNgboadrAdfMsMj3o-QLAZL5H'
                ];
                if (typeof FALLBACK_VAULT !== 'undefined') {
                  enrichedPlaylists.forEach(id => {
                    if (!FALLBACK_VAULT.includes(id)) FALLBACK_VAULT.push(id);
                  });
                  // Refill from the enriched vault even if the hosted page initialized its bag.
                  if (typeof vaultBag !== 'undefined') vaultBag = [];
                }
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
                isPoweredOn = true;
                appState = 'powered_on';
                if (!playerReady) initYouTubePlayer(); else playNextVideo();
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
            alpha = 0f
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (url.startsWith(PLAYER_URL)) {
                        view.evaluateJavascript(HOSTED_PLAYER_SETUP) {
                            view.animate().alpha(1f).setDuration(120L).start()
                        }
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
