package com.intview.tv

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONTokener
import java.util.ArrayDeque
import kotlin.math.abs

/** Reddit-only provider test. The production YouTube discovery pipeline is intentionally inactive. */
class MainActivity : AppCompatActivity() {
    companion object {
        // The server-rendered listing exposes a full page of posts and outbound URLs.
        // Modern Reddit only materializes roughly three cards until a visible user scrolls.
        private const val REDDIT_LISTING = "https://old.reddit.com/r/aivideo/hot/"
        private const val PLAYER_PAGE = "file:///android_asset/youtube_player.html"
        private const val HISTORY_KEY = "watched_media_v1"
        private const val HISTORY_LIMIT = 500

        private val YOUTUBE_ID = Regex(
            "(?:youtube(?:-nocookie)?\\.com/(?:watch\\?(?:[^#]*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})",
            RegexOption.IGNORE_CASE
        )
        private val REDDIT_MEDIA_ID =
            Regex("https?://v\\.redd\\.it/([A-Za-z0-9]+)", RegexOption.IGNORE_CASE)
        private val DIRECT_MEDIA =
            Regex("\\.(?:mp4|m3u8|mpd)(?:[?#]|$)", RegexOption.IGNORE_CASE)
    }

    private enum class Kind { NATIVE, YOUTUBE }
    private data class PlaybackItem(
        val kind: Kind,
        val source: String,
        val identity: String,
        val redditIdentity: String? = null
    )
    private data class RedditPost(
        val permalink: String,
        val identity: String,
        val candidates: List<String> = emptyList()
    )

    private lateinit var root: FrameLayout
    private lateinit var nativeView: PlayerView
    private lateinit var webPlayer: WebView
    private lateinit var scraper: WebView
    private lateinit var tuningOverlay: View
    private lateinit var exoPlayer: ExoPlayer

    private val handler = Handler(Looper.getMainLooper())
    private val playbackQueue = ArrayDeque<PlaybackItem>()
    private val previousItems = ArrayDeque<PlaybackItem>()
    private val pendingPosts = ArrayDeque<RedditPost>()
    private val queuedIdentities = mutableSetOf<String>()
    private val watched = LinkedHashSet<String>()
    private var current: PlaybackItem? = null
    private var resolvingPost: RedditPost? = null
    private var resolvingExternal = false
    private var scraperStarted = false
    private var pendingYouTubeId: String? = null
    private var pendingAdvance = false
    private var playbackGeneration = 0
    private var resolutionGeneration = 0
    private var listingAttempts = 0
    private var listingReloadScheduled = false

    private val gestures by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true
            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                togglePlayback()
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
                if (distanceX < 0) playNext() else playPrevious()
                return true
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()
        loadHistory()

        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) playNext()
                    if (state == Player.STATE_BUFFERING && player.playWhenReady) {
                        val generation = playbackGeneration
                        handler.postDelayed({
                            if (generation == playbackGeneration &&
                                player.playbackState == Player.STATE_BUFFERING &&
                                player.playWhenReady
                            ) {
                                playNext()
                            }
                        }, 15_000L)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    handler.post { playNext() }
                }
            })
        }

        root = FrameLayout(this)
        nativeView = PlayerView(this).apply {
            useController = false
            player = exoPlayer
            setShutterBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        webPlayer = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.scheme == "intview" && request.url.host == "ended") {
                        playNext()
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (url == PLAYER_PAGE) {
                        pendingYouTubeId?.let { id ->
                            view.evaluateJavascript("loadRedditVideo(${quoteJs(id)});", null)
                        }
                    }
                }
            }
            setOnTouchListener { _, event -> gestures.onTouchEvent(event) }
            visibility = View.GONE
        }
        tuningOverlay = View(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.VISIBLE
        }
        scraper = createScraper()

        root.addView(nativeView, matchParent())
        root.addView(webPlayer, matchParent())
        root.addView(tuningOverlay, matchParent())
        root.addView(scraper, FrameLayout.LayoutParams(1, 1))
        root.setOnTouchListener { _, event -> gestures.onTouchEvent(event) }
        setContentView(root)

        scraperStarted = true
        scraper.loadUrl(REDDIT_LISTING)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createScraper(): WebView = WebView(this).apply {
        visibility = View.INVISIBLE
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = false
        settings.mediaPlaybackRequiresUserGesture = true
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                when {
                    resolvingPost != null -> extractMediaCandidates(view)
                    url.contains("/r/aivideo/") -> extractListing(view)
                }
            }
        }
    }

    private fun extractListing(view: WebView) {
        val script = """
            (() => JSON.stringify([...document.querySelectorAll('.thing.link, shreddit-post, article')]
              .map(node => {
                const link = node.getAttribute('data-permalink') ||
                  node.getAttribute('permalink') ||
                  node.querySelector('a[href*="/comments/"]')?.href || '';
                const values = [];
                const add = value => { if (value && typeof value === 'string') values.push(value); };
                ['src', 'href', 'content-href', 'data-url', 'video-url', 'data-href-url']
                  .forEach(name => add(node.getAttribute?.(name)));
                add(node.querySelector('a.title')?.href);
                node.querySelectorAll?.('video, source, iframe, shreddit-player, shreddit-embed')
                  .forEach(media => ['src', 'href', 'content-href', 'data-url', 'video-url']
                    .forEach(name => add(media.getAttribute?.(name))));
                const postUrl = new URL(link, location.href);
                postUrl.hostname = 'www.reddit.com';
                return {
                  link: postUrl.href,
                  candidates: [...new Set(values)].map(value => new URL(value, location.href).href)
                };
              })
              .filter(item => item.link.includes('/comments/'))
              .filter((item, index, all) => all.findIndex(other => other.link === item.link) === index)
              .slice(0, 50)))()
        """.trimIndent()
        view.evaluateJavascript(script) { result ->
            val posts = decodeArray(result)
            if (posts == null || posts.length() == 0) {
                listingAttempts += 1
                if (listingAttempts <= 5) {
                    handler.postDelayed({
                        if (scraper.url?.contains("/r/aivideo/") == true) extractListing(scraper)
                    }, 1_500L)
                } else if (!listingReloadScheduled) {
                    listingReloadScheduled = true
                    handler.postDelayed({
                        listingAttempts = 0
                        listingReloadScheduled = false
                        scraper.loadUrl(REDDIT_LISTING)
                    }, 3_000L)
                }
                return@evaluateJavascript
            }
            listingAttempts = 0
            listingReloadScheduled = false
            val discovered = mutableListOf<RedditPost>()
            posts.let {
                for (index in 0 until posts.length()) {
                    val entry = posts.optJSONObject(index) ?: continue
                    val link = entry.optString("link")
                    val postId = Regex("/comments/([^/]+)").find(link)?.groupValues?.get(1)
                    if (postId != null) {
                        val candidates = mutableListOf<String>()
                        val candidateArray = entry.optJSONArray("candidates")
                        if (candidateArray != null) {
                            for (candidateIndex in 0 until candidateArray.length()) {
                                candidateArray.optString(candidateIndex)
                                    .takeIf(String::isNotBlank)
                                    ?.let(candidates::add)
                            }
                        }
                        discovered.add(RedditPost(link, "reddit:$postId", candidates))
                    }
                }
            }
            val randomized = discovered.shuffled()
            val immediatelyPlayable = randomized.filter { post ->
                post.candidates.any(::isDirectMediaCandidate)
            }
            val requiresPageResolution = randomized.filterNot { post ->
                post.candidates.any(::isDirectMediaCandidate)
            }
            pendingPosts.addAll(immediatelyPlayable)
            pendingPosts.addAll(requiresPageResolution)
            resolveNextPost()
        }
    }

    private fun resolveNextPost() {
        if (resolvingPost != null) return
        while (pendingPosts.isNotEmpty()) {
            val post = pendingPosts.removeFirst()
            if (post.identity in watched || post.identity in queuedIdentities) continue
            resolvingPost = post
            resolvingExternal = false
            val generation = ++resolutionGeneration
            handler.postDelayed({
                if (generation == resolutionGeneration && resolvingPost == post) {
                    finishResolution(null)
                }
            }, 8_000L)
            if (post.candidates.any(::isPotentialMediaCandidate)) {
                resolveCandidates(post.candidates)
                return
            }
            scraper.loadUrl(post.permalink)
            return
        }
        if (playbackQueue.isEmpty() && (current == null || pendingAdvance) &&
            scraperStarted && !listingReloadScheduled
        ) {
            listingReloadScheduled = true
            handler.postDelayed({
                listingReloadScheduled = false
                scraper.loadUrl(REDDIT_LISTING)
            }, 3_000L)
        }
    }

    private fun extractMediaCandidates(view: WebView) {
        val postId = resolvingPost?.identity?.removePrefix("reddit:") ?: return
        val script = """
            (() => {
              const values = [];
              const add = value => { if (value && typeof value === 'string') values.push(value); };
              const onReddit = location.hostname === 'reddit.com' || location.hostname.endsWith('.reddit.com');
              const postId = ${quoteJs(postId)};
              const root = onReddit
                ? (document.querySelector(`shreddit-post[id="t3_${'$'}{postId}"]`) ||
                   document.querySelector(`shreddit-post[post-id="${'$'}{postId}"]`) ||
                   document.querySelector('shreddit-post'))
                : document;
              if (!root) return JSON.stringify([]);
              document.querySelectorAll('meta[property="og:video"], meta[property="og:video:url"], meta[property="og:video:secure_url"], meta[name="twitter:player:stream"]')
                .forEach(node => add(node.content));
              ['src', 'href', 'content-href', 'data-url', 'video-url']
                .forEach(name => add(root.getAttribute?.(name)));
              root.querySelectorAll?.('video, source, iframe, shreddit-player, shreddit-embed, a[href]')
                .forEach(node => ['src', 'href', 'content-href', 'data-url', 'video-url'].forEach(name => add(node.getAttribute?.(name))));
              // Shadow-DOM Reddit players may expose v.redd.it only as a loaded resource.
              performance.getEntriesByType('resource')
                .filter(entry => entry.name.includes('v.redd.it/'))
                .forEach(entry => add(entry.name));
              return JSON.stringify([...new Set(values)].map(value => new URL(value, location.href).href));
            })()
        """.trimIndent()
        view.evaluateJavascript(script) { result ->
            val urls = mutableListOf<String>()
            decodeArray(result)?.let { array ->
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf(String::isNotBlank)?.let(urls::add)
                }
            }
            resolveCandidates(urls)
        }
    }

    private fun resolveCandidates(urls: List<String>) {
        resolvingPost ?: return

        urls.firstNotNullOfOrNull { url ->
            YOUTUBE_ID.find(url)?.groupValues?.get(1)
        }?.let { videoId ->
            finishResolution(PlaybackItem(Kind.YOUTUBE, videoId, "youtube:$videoId"))
            return
        }

        urls.firstNotNullOfOrNull { url ->
            REDDIT_MEDIA_ID.find(url)?.groupValues?.get(1)
        }?.let { mediaId ->
            val hls = "https://v.redd.it/$mediaId/HLSPlaylist.m3u8"
            finishResolution(PlaybackItem(Kind.NATIVE, hls, "reddit-media:$mediaId"))
            return
        }

        urls.firstOrNull { DIRECT_MEDIA.containsMatchIn(it) }?.let { direct ->
            finishResolution(PlaybackItem(Kind.NATIVE, direct.replace("&amp;", "&"), mediaIdentity(direct)))
            return
        }

        if (!resolvingExternal) {
            val external = urls.firstOrNull { candidate ->
                candidate.startsWith("https://") &&
                    !candidate.contains("reddit.com/") &&
                    !candidate.contains("redd.it/") &&
                    !candidate.contains("redditstatic.com/") &&
                    !candidate.contains("redditmedia.com/") &&
                    !candidate.contains("google.com/") &&
                    !Regex("\\.(?:jpg|jpeg|png|gif|webp|svg|css|js)(?:[?#]|$)", RegexOption.IGNORE_CASE)
                        .containsMatchIn(candidate)
            }
            if (external != null) {
                resolvingExternal = true
                scraper.loadUrl(external)
                return
            }
        }
        // Never display an unresolved webpage. It may be an ad or unrelated page asset.
        finishResolution(null)
    }

    private fun finishResolution(item: PlaybackItem?) {
        resolutionGeneration += 1
        val resolved = item?.copy(redditIdentity = resolvingPost?.identity)
        resolvingPost = null
        resolvingExternal = false
        val identities = resolved?.identities().orEmpty()
        if (resolved != null && identities.none { it in watched || it in queuedIdentities }) {
            queuedIdentities.addAll(identities)
            playbackQueue.add(resolved)
            if (current == null || pendingAdvance) playNext()
        }
        resolveNextPost()
    }

    private fun playNext() {
        val next = if (playbackQueue.isEmpty()) null else playbackQueue.removeFirst()
        if (next == null) {
            pendingAdvance = true
            resolveNextPost()
            return
        }
        pendingAdvance = false
        queuedIdentities.removeAll(next.identities())
        current?.let { previous ->
            previousItems.addLast(previous)
            while (previousItems.size > 50) previousItems.removeFirst()
        }
        startPlayback(next)
    }

    private fun playPrevious() {
        if (previousItems.isEmpty()) return
        pendingAdvance = false
        current?.let { forward ->
            playbackQueue.addFirst(forward)
            queuedIdentities.addAll(forward.identities())
        }
        startPlayback(previousItems.removeLast())
    }

    private fun startPlayback(next: PlaybackItem) {
        playbackGeneration += 1
        exoPlayer.pause()
        exoPlayer.volume = 1f
        tuningOverlay.visibility = View.GONE
        current = next
        next.identities().forEach(::rememberWatched)

        when (next.kind) {
            Kind.NATIVE -> playNative(next.source)
            Kind.YOUTUBE -> playYouTube(next.source)
        }
        webPlayer.evaluateJavascript("if(window.player){player.unMute();}", null)
    }

    private fun playNative(url: String) {
        webPlayer.visibility = View.GONE
        webPlayer.onPause()
        nativeView.visibility = View.VISIBLE
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun playYouTube(videoId: String) {
        exoPlayer.stop()
        nativeView.visibility = View.GONE
        webPlayer.visibility = View.VISIBLE
        webPlayer.onResume()
        pendingYouTubeId = videoId
        if (webPlayer.url == PLAYER_PAGE) {
            webPlayer.evaluateJavascript("loadRedditVideo(${quoteJs(videoId)});", null)
        } else {
            webPlayer.loadUrl(PLAYER_PAGE)
        }
    }

    private fun togglePlayback() {
        when (current?.kind) {
            Kind.NATIVE -> if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            Kind.YOUTUBE -> webPlayer.evaluateJavascript("togglePlayback();", null)
            else -> Unit
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> {
            playNext()
            true
        }
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
            playPrevious()
            true
        }
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
            togglePlayback()
            true
        }
        KeyEvent.KEYCODE_MEDIA_PLAY -> {
            if (current?.kind == Kind.NATIVE) exoPlayer.play()
            else webPlayer.evaluateJavascript("play();", null)
            true
        }
        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
            if (current?.kind == Kind.NATIVE) exoPlayer.pause()
            else webPlayer.evaluateJavascript("pause();", null)
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun loadHistory() {
        val stored = getSharedPreferences("int_view", MODE_PRIVATE).getString(HISTORY_KEY, "[]") ?: "[]"
        runCatching {
            val array = JSONArray(stored)
            for (index in 0 until array.length()) watched.add(array.getString(index))
        }
    }

    private fun rememberWatched(identity: String) {
        watched.remove(identity)
        watched.add(identity)
        while (watched.size > HISTORY_LIMIT) watched.remove(watched.first())
        getSharedPreferences("int_view", MODE_PRIVATE).edit()
            .putString(HISTORY_KEY, JSONArray(watched.toList()).toString())
            .apply()
    }

    private fun decodeArray(result: String?): JSONArray? = runCatching {
        val decoded = JSONTokener(result ?: return null).nextValue()
        when (decoded) {
            is JSONArray -> decoded
            is String -> JSONArray(decoded)
            else -> null
        }
    }.getOrNull()

    private fun mediaIdentity(url: String): String = "media:${normalizeUrl(url)}"
    private fun isPotentialMediaCandidate(url: String): Boolean =
        isDirectMediaCandidate(url) ||
            (url.startsWith("https://") &&
                !url.contains("reddit.com/") &&
                !url.contains("redd.it/") &&
                !url.contains("redditstatic.com/") &&
                !url.contains("redditmedia.com/"))
    private fun isDirectMediaCandidate(url: String): Boolean =
        YOUTUBE_ID.containsMatchIn(url) ||
            REDDIT_MEDIA_ID.containsMatchIn(url) ||
            DIRECT_MEDIA.containsMatchIn(url)
    private fun normalizeUrl(url: String): String =
        url.substringBefore('#').substringBefore('?').replace("&amp;", "&")
    private fun PlaybackItem.identities(): Set<String> =
        setOfNotNull(identity, redditIdentity)
    private fun quoteJs(value: String): String = JSONArray().put(value).toString().drop(1).dropLast(1)
    private fun matchParent() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scraper.destroy()
        webPlayer.destroy()
        exoPlayer.release()
        super.onDestroy()
    }
}
