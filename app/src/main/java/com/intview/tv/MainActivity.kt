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
import org.json.JSONObject
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
                    min-width: 0 !important; min-height: 0 !important; max-width: none !important;
                    max-height: none !important; aspect-ratio: auto !important;
                    transform: none !important; opacity: 1 !important; visibility: visible !important;
                  }
                `;
                document.head.appendChild(style);

                // Keep only the original project's curated YouTube engine. Its room,
                // static, channel-transition and power effects belong to the web UI.
                window.playSound = () => {};
                window.startStaticSound = () => {};
                window.stopStaticSound = () => {};
                window.showStaticOverlay = () => {};
                window.hideStaticOverlay = () => {};
                window.startScreenGlitches = () => {};
                window.stopScreenGlitches = () => {};
                window.showChannelOSD = () => {};
                window.showCommercialBug = () => {};
                window.hideCommercialBug = () => {};
                window.channelSwitchEffect = (_channel, callback) => callback();

                const intViewTopics = [
                  ['weird experimental video art', 'bizarre short film surreal', 'surreal animation loop', 'glitch art visual', 'abstract experimental film', 'avant garde animation'],
                  ['street food around the world', 'bizarre food challenge extreme', 'extreme cooking technique', 'food science experiment', 'molecular gastronomy chef', 'satisfying food making process'],
                  ['synthwave retrowave music video', '80s music video classic', 'retrowave drive compilation', 'vintage commercial compilation 1980s', 'vhs aesthetic music retro', '80s aesthetic outrun'],
                  ['incredible nature footage 4K', 'rare animal behavior caught on camera', 'extreme weather storm footage', 'deep ocean creatures footage', 'volcano eruption close up', 'wildlife encounter unexpected'],
                  ['incredible fails compilation', 'extreme sport impossible stunt', 'unexpected live tv moment funny', 'people doing impossible things', 'world record attempt guinness', 'instant regret compilation'],
                  ['physics experiment mind blowing', 'chemistry experiment spectacular', 'engineering satisfying machine', 'space footage 4K NASA', 'microscope footage fascinating', 'science demonstration incredible'],
                  ['worst infomercial compilation', 'vintage infomercial 90s', 'as seen on tv product review', 'weird product advertisement', 'telemarketing fails funny', 'late night infomercial classic'],
                  ['television test pattern color bars', 'static noise visual art', 'glitch art video loop', 'analog tv static noise', 'crt tv test pattern', 'signal lost tv broadcast'],
                  ['local commercial 1993', 'weird public access tv', 'unlisted vhs rip 90s', 'found footage weird video', 'bizarre infomercial bloopers', 'local news fail 1980s', 'obscure animation short', 'weird amateur video']
                ];
                const intViewModifiers = [
                  'trippy', 'surreal', 'bizarre', 'psychedelic', 'fever dream',
                  'weird', 'liminal', 'absurdist comedy', 'late night adult swim',
                  'cursed video', 'weirdcore', 'dreamcore', 'surreal meme'
                ];
                const intViewQueues = {};
                let intViewChannel = Math.floor(Math.random() * intViewTopics.length);
                let intViewRequest = 0;

                const parseDuration = value => {
                  const match = value.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);
                  if (!match) return 0;
                  return ((Number(match[1]) || 0) * 3600) +
                    ((Number(match[2]) || 0) * 60) + (Number(match[3]) || 0);
                };

                const apiSearch = async channel => {
                  const cacheKey = 'int_view_api_channel_' + channel;
                  try {
                    const cached = JSON.parse(localStorage.getItem(cacheKey) || 'null');
                    if (cached && cached.expiry > Date.now() && cached.videos?.length) {
                      return cached.videos;
                    }
                  } catch (_) {}

                  const topics = intViewTopics[channel];
                  let query = topics[Math.floor(Math.random() * topics.length)];
                  if (Math.random() > 0.3) {
                    query += ' ' + intViewModifiers[Math.floor(Math.random() * intViewModifiers.length)];
                  }
                  query += ' -tutorial -news -"how to" -"breaking news" -"news channel" -podcast';

                  const searchUrl = 'https://www.googleapis.com/youtube/v3/search?' +
                    new URLSearchParams({
                      part: 'id', type: 'video', q: query, videoEmbeddable: 'true',
                      maxResults: '15', order: 'relevance', videoDuration: 'medium',
                      safeSearch: 'moderate', key: window.INT_VIEW_API_KEY
                    });
                  const searchResponse = await fetch(searchUrl);
                  if (!searchResponse.ok) throw new Error('Search API ' + searchResponse.status);
                  const searchData = await searchResponse.json();
                  const ids = (searchData.items || []).map(item => item.id?.videoId).filter(Boolean);
                  if (!ids.length) throw new Error('No search results');

                  const detailUrl = 'https://www.googleapis.com/youtube/v3/videos?' +
                    new URLSearchParams({
                      part: 'contentDetails,snippet', id: ids.join(','),
                      key: window.INT_VIEW_API_KEY
                    });
                  const detailResponse = await fetch(detailUrl);
                  if (!detailResponse.ok) throw new Error('Videos API ' + detailResponse.status);
                  const detailData = await detailResponse.json();
                  const videos = (detailData.items || []).map(item => ({
                    id: item.id,
                    duration: parseDuration(item.contentDetails?.duration || '')
                  })).filter(video => video.duration > 60);
                  if (!videos.length) throw new Error('No playable videos');

                  localStorage.setItem(cacheKey, JSON.stringify({
                    videos: videos,
                    expiry: Date.now() + (60 * 60 * 1000)
                  }));
                  return videos;
                };

                const apiPlayNext = async () => {
                  if (!playerReady || !player) return;
                  if (!window.INT_VIEW_API_KEY) {
                    playFallbackVaultItem();
                    return;
                  }
                  const request = ++intViewRequest;
                  try {
                    if (!intViewQueues[intViewChannel]?.length) {
                      const videos = await apiSearch(intViewChannel);
                      intViewQueues[intViewChannel] = videos.sort(() => Math.random() - 0.5);
                    }
                    if (request !== intViewRequest) return;
                    const played = getPlayedVideos();
                    const queue = intViewQueues[intViewChannel];
                    let index = queue.findIndex(video => !played.includes(video.id));
                    if (index < 0) index = 0;
                    const video = queue.splice(index, 1)[0];
                    if (!video) throw new Error('Empty queue');
                    markVideoPlayed(video.id);
                    const minimum = Math.floor(video.duration * 0.03);
                    const maximum = Math.max(minimum + 1, Math.floor(video.duration * 0.30));
                    const start = minimum + Math.floor(Math.random() * (maximum - minimum));
                    player.loadVideoById({ videoId: video.id, startSeconds: start });
                  } catch (error) {
                    console.warn('Int View API search failed; using curated vault.', error);
                    playFallbackVaultItem();
                  }
                };

                window.playNextVideo = apiPlayNext;
                window.changeChannel = direction => {
                  intViewChannel = (intViewChannel + direction + intViewTopics.length) % intViewTopics.length;
                  apiPlayNext();
                };

                // The source project intentionally waits four seconds before unmuting
                // to cover its TV-static animation. Int View has no such animation.
                const sourceStateChange = onPlayerStateChange;
                window.onPlayerStateChange = event => {
                  if (event.data === YT.PlayerState.PLAYING && !isMuted && player) {
                    player.unMute();
                  }
                  sourceStateChange(event);
                };
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
                        val apiSetup =
                            "window.INT_VIEW_API_KEY=${JSONObject.quote(BuildConfig.YOUTUBE_API_KEY)};\n" +
                                HOSTED_PLAYER_SETUP
                        view.evaluateJavascript(apiSetup) {
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
