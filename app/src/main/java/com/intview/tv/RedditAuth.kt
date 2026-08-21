package com.intview.tv

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.util.Base64

object RedditAuth {
    private const val REDIRECT = "intview://oauth"
    private const val TOKEN_URL = "https://www.reddit.com/api/v1/access_token"
    private val client = OkHttpClient()

    fun start(context: Context) {
        require(BuildConfig.REDDIT_CLIENT_ID != "CHANGE_ME") { "Set REDDIT_CLIENT_ID before building." }
        val state = ByteArray(24).also { SecureRandom().nextBytes(it) }.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit().putString("state", state).apply()
        val uri = Uri.parse("https://www.reddit.com/api/v1/authorize.compact").buildUpon()
            .appendQueryParameter("client_id", BuildConfig.REDDIT_CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", state)
            .appendQueryParameter("redirect_uri", REDIRECT)
            .appendQueryParameter("duration", "permanent")
            .appendQueryParameter("scope", "read")
            .build()
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }

    suspend fun finish(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val expected = context.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("state", null)
        if (expected == null || expected != uri.getQueryParameter("state")) return@withContext false
        val code = uri.getQueryParameter("code") ?: return@withContext false
        val body = FormBody.Builder().add("grant_type", "authorization_code").add("code", code).add("redirect_uri", REDIRECT).build()
        val basic = okhttp3.Credentials.basic(BuildConfig.REDDIT_CLIENT_ID, "")
        val request = Request.Builder().url(TOKEN_URL).header("Authorization", basic).header("User-Agent", "android:com.intview.tv:v0.1.0 (personal TV viewer)").post(body).build()
        client.newCall(request).execute().use { response ->
            val token = Regex("\"refresh_token\"\\s*:\\s*\"([^\"]+)\"").find(response.body?.string().orEmpty())?.groupValues?.get(1)
            if (!response.isSuccessful || token == null) return@withContext false
            context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit().putString("refresh_token", token).remove("state").apply()
            true
        }
    }
}
