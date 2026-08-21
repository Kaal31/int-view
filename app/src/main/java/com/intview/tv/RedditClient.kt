package com.intview.tv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class VideoPost(val title: String, val author: String, val score: Int, val permalink: String, val videoUrl: String)

class RedditClient(private val context: Context) {
    private val http = OkHttpClient()
    private val agent = "android:com.intview.tv:v0.1.0 (personal TV viewer)"
    private suspend fun accessToken(): String? = withContext(Dispatchers.IO) {
        val refresh = context.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("refresh_token", null) ?: return@withContext null
        val request = Request.Builder().url("https://www.reddit.com/api/v1/access_token")
            .header("Authorization", okhttp3.Credentials.basic(BuildConfig.REDDIT_CLIENT_ID, ""))
            .header("User-Agent", agent)
            .post(FormBody.Builder().add("grant_type", "refresh_token").add("refresh_token", refresh).build()).build()
        http.newCall(request).execute().use { r -> Regex("\"access_token\"\\s*:\\s*\"([^\"]+)\"").find(r.body?.string().orEmpty())?.groupValues?.get(1) }
    }
    suspend fun queue(sort: String, period: String = "week"): List<VideoPost> = withContext(Dispatchers.IO) {
        val token = accessToken() ?: return@withContext emptyList()
        val url = "https://oauth.reddit.com/r/ai_video/$sort?limit=100&t=$period"
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").header("User-Agent", agent).build()
        val json = http.newCall(request).execute().use { it.body?.string().orEmpty() }
        val children = JSONObject(json).getJSONObject("data").getJSONArray("children")
        buildList {
            for (i in 0 until children.length()) {
                val post = children.getJSONObject(i).getJSONObject("data")
                val redditVideo = post.optJSONObject("media")?.optJSONObject("reddit_video")
                    ?: post.optJSONObject("secure_media")?.optJSONObject("reddit_video")
                // DASH includes Reddit's separate audio track. Direct MP4 is the fallback.
                val video = redditVideo?.optString("dash_url")?.takeIf { it.isNotBlank() }
                    ?: post.optString("url_overridden_by_dest").takeIf { it.substringBefore('?').endsWith(".mp4") }
                    ?: continue
                add(VideoPost(
                    title = post.optString("title", "Untitled"),
                    author = post.optString("author", "unknown"),
                    score = post.optInt("score"),
                    permalink = post.optString("permalink"),
                    videoUrl = video.replace("\\u0026", "&")
                ))
            }
        }
    }
}
