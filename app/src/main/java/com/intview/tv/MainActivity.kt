package com.intview.tv

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var overlay: TextView
    private var posts = emptyList<VideoPost>()
    private var index = 0
    private var sort = "hot"
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val frame = FrameLayout(this)
        frame.addView(PlayerView(this).apply {
            useController = false
            player = this@MainActivity.player
            setShutterBackgroundColor(android.graphics.Color.BLACK)
        }, FrameLayout.LayoutParams(-1, -1))
        // Only used for first-run setup or an error. It is removed during playback.
        overlay = TextView(this).apply {
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0xB0000000.toInt())
            visibility = View.GONE
        }
        frame.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
        if (BuildConfig.REDDIT_CLIENT_ID == "CHANGE_ME") overlay.text = "Set REDDIT_CLIENT_ID, then rebuild."
        else if (getSharedPreferences("auth", MODE_PRIVATE).getString("refresh_token", null) == null) { overlay.text = "Press OK to sign in to Reddit"; frame.isFocusableInTouchMode = true; frame.requestFocus() }
        else loadQueue()
        player.addListener(object : androidx.media3.common.Player.Listener { override fun onPlaybackStateChanged(state: Int) { if (state == ExoPlayer.STATE_ENDED) next() } })
    }
    private fun loadQueue() = scope.launch {
        overlay.visibility = View.GONE
        posts = RedditClient(this@MainActivity).queue(sort)
        index = 0
        if (posts.isEmpty()) {
            overlay.text = "No playable videos found"
            overlay.visibility = View.VISIBLE
        }
        else playCurrent()
    }
    private fun playCurrent() {
        val post = posts[index]
        overlay.visibility = View.GONE
        player.setMediaItem(MediaItem.fromUri(post.videoUrl))
        player.prepare()
        player.play()
    }
    private fun next() { if (posts.isNotEmpty()) { index = (index + 1) % posts.size; playCurrent() } }
    private fun previous() { if (posts.isNotEmpty()) { index = (index - 1 + posts.size) % posts.size; playCurrent() } }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER -> { if (getSharedPreferences("auth", MODE_PRIVATE).getString("refresh_token", null) == null) RedditAuth.start(this) else if (player.isPlaying) player.pause() else player.play(); true }
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> { next(); true }
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { previous(); true }
        KeyEvent.KEYCODE_MENU -> { sort = listOf("hot", "new", "top", "rising", "controversial")[(listOf("hot", "new", "top", "rising", "controversial").indexOf(sort) + 1) % 5]; loadQueue(); true }
        else -> super.onKeyDown(keyCode, event)
    }
    override fun onStop() { super.onStop(); player.pause() }
    override fun onDestroy() { player.release(); super.onDestroy() }
}
