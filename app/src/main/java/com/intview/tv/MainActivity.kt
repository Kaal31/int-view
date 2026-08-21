package com.intview.tv

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

/** A deliberately interface-free, remote-controlled YouTube channel surfer. */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val PLAYER_URL =
            "https://raunakpatil.github.io/InterdimentionalCable/"
        private const val TUNING_MASK_MS = 4_000L

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

                // The four-second tuning transition is native so it works reliably
                // with Android TV remote events and does not depend on hosted DOM/audio state.
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

                // Enrich the hosted fallback vault while keeping existing entries.
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
                  if (typeof vaultBag !== 'undefined') vaultBag = [];
                }

                // Force caption policy into the player before it is constructed. YouTube
                // otherwise may restore a viewer's remembered/automatic caption preference.
                if (window.YT?.Player && !YT.Player.__intViewCaptionsWrapped) {
                  const YouTubePlayer = YT.Player;
                  const IntViewPlayer = function(element, options = {}) {
                    options.playerVars = Object.assign({}, options.playerVars || {}, {
                      cc_load_policy: 0,
                      iv_load_policy: 3
                    });
                    return new YouTubePlayer(element, options);
                  };
                  IntViewPlayer.prototype = YouTubePlayer.prototype;
                  Object.assign(IntViewPlayer, YouTubePlayer);
                  IntViewPlayer.__intViewCaptionsWrapped = true;
                  YT.Player = IntViewPlayer;
                }

                const disableCaptions = () => {
                  if (typeof player === 'undefined' || !player) return;
                  try { player.setOption('captions', 'track', {}); } catch (_) {}
                  try { player.setOption('cc', 'track', {}); } catch (_) {}
                  try { player.unloadModule('captions'); } catch (_) {}
                  try { player.unloadModule('cc'); } catch (_) {}
                };

                // The hosted page can bind its state callback before this takeover runs,
                // so do not rely on replacing that callback. Clear tracks repeatedly to
                // catch captions that YouTube loads asynchronously after PLAYING.
                const captionGuard = setInterval(disableCaptions, 500);
                window.addEventListener('beforeunload', () => clearInterval(captionGuard));

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
                  showStaticOverlay();
                  startStaticSound();
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
                    disableCaptions();
                  } catch (error) {
                    console.warn('Int View API search failed; using curated vault.', error);
                    playFallbackVaultItem();
                  }
                };

                window.playNextVideo = apiPlayNext;
                window.changeChannel = direction => {
                  intViewChannel = (intViewChannel + direction + intViewTopics.length) % intViewTopics.length;
                  channelSwitchEffect(intViewChannel + 1, apiPlayNext);
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

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var tuningNoise: TuningNoiseView
    private val handler = Handler(Looper.getMainLooper())
    private var noiseTrack: AudioTrack? = null
    private val hideTuningMask = Runnable {
        tuningNoise.stop()
        stopTuningSound()
        runPlayer("if(window.player){player.unMute();}")
    }

    private inner class TuningNoiseView(context: Context) : View(context) {
        private val widthPixels = 160
        private val heightPixels = 90
        private val pixels = IntArray(widthPixels * heightPixels)
        private val bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        private val paint = android.graphics.Paint().apply { isFilterBitmap = false }
        private val frame = object : Runnable {
            override fun run() {
                for (index in pixels.indices) {
                    val value = Random.nextInt(256)
                    pixels[index] = Color.rgb(value, value, value)
                }
                bitmap.setPixels(pixels, 0, widthPixels, 0, 0, widthPixels, heightPixels)
                invalidate()
                postDelayed(this, 45L)
            }
        }

        fun start() {
            removeCallbacks(frame)
            visibility = VISIBLE
            bringToFront()
            post(frame)
        }

        fun stop() {
            removeCallbacks(frame)
            visibility = GONE
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, width, height), paint)
        }
    }

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
                switchChannel(if (distanceX < 0) "changeChannel(1)" else "changeChannel(-1)")
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
        root = FrameLayout(this)
        tuningNoise = TuningNoiseView(this).apply { visibility = View.GONE }
        root.addView(webView, matchParent())
        root.addView(tuningNoise, matchParent())
        setContentView(root)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handledKey = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    switchChannel("changeChannel(1)")
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    switchChannel("changeChannel(-1)")
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    runPlayer("togglePlayback()")
                }
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    runPlayer("play()")
                }
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    runPlayer("pause()")
                }
                true
            }
            else -> false
        }
        return if (handledKey) true else super.dispatchKeyEvent(event)
    }

    private fun switchChannel(command: String) {
        startTuningMask()
        runPlayer(command)
    }

    private fun startTuningMask() {
        handler.removeCallbacks(hideTuningMask)
        tuningNoise.start()
        runPlayer("if(window.player){player.mute();}")
        startTuningSound()
        handler.postDelayed(hideTuningMask, TUNING_MASK_MS)
    }

    private fun startTuningSound() {
        stopTuningSound()
        val sampleRate = 22_050
        val samples = ShortArray(sampleRate) {
            Random.nextInt(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        noiseTrack = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
                .also { track ->
                    track.write(samples, 0, samples.size)
                    track.setLoopPoints(0, samples.size, -1)
                    track.setVolume(0.22f)
                    track.play()
                }
        }.getOrNull()
    }

    private fun stopTuningSound() {
        noiseTrack?.let { track ->
            runCatching { track.stop() }
            track.release()
        }
        noiseTrack = null
    }

    private fun runPlayer(command: String) {
        webView.evaluateJavascript("$command;", null)
    }

    private fun matchParent() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

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
        handler.removeCallbacksAndMessages(null)
        stopTuningSound()
        webView.destroy()
        super.onDestroy()
    }
}
