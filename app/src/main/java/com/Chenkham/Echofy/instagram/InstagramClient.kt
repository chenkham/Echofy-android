package com.Chenkham.Echofy.instagram

import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.UUID

/**
 * A minimal, internal Instagram Client.
 * WARNING: This uses the Private API. Usage carries risk.
 */
class InstagramClient {

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host]?.addAll(cookies) ?: cookieStore.put(url.host, cookies.toMutableList())
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .build()

    private val uuid = UUID.randomUUID().toString()
    private val deviceId = "android-" + UUID.randomUUID().toString().take(16) // Mock Android ID
    private val phoneId = UUID.randomUUID().toString()
    
    // Basic constants to mimic a device (Pixel 2 or similar known user agent)
    private val USER_AGENT = "Instagram 269.0.0.18.75 Android (33/13; 420dpi; 1080x1920; Google/google; Pixel 2; walleye; qcom; en_US; 436324209)"

    suspend fun login(username: String, pass: String): Result<InstaLoginResponse> = runCatching {
        // 1. Pre-login / CSRF handshake (often needed, but we try direct first or basic flow)
        // Ideally we hit the landing page or a pre-login endpoint to get a CSRF token.
        // For simplicity, we'll try a direct signed login request or standard form depending on what works easiest without signatures.
        // "Generic" private API ports usually do complex signature signing (hmac).
        // Let's rely on standard form params first if possible, or we must implement the signature.
        // If simple form fails, we need full signature logic.
        
        // Actually, the new web-based API is easier for read-only if we just want data, but "Reels" specific endpoints often require App API.
        // We will try the App API approach.
        
        // NOTE: Implementing full signature generation (IG_SIG_KEY) is complex in one go.
        // Strategy: Try the WEB API login? Usually safer/less strict?
        // But Web API doesn't always have "Reels Tray". 
        // Let's try mimicking the WEB API login first as it's cleaner.
        
        // Web Login Flow:
        // POST https://www.instagram.com/accounts/login/ajax/
        
        val loginUrl = "https://www.instagram.com/accounts/login/ajax/"
        
        // We need an initial CSRF token.
        val initReq = Request.Builder()
            .url("https://www.instagram.com/")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Pixel 2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")
            .build()
            
        client.newCall(initReq).execute().close() // Just to populate cookie jar with csrftoken
        
        val cookies = cookieStore["www.instagram.com"] ?: emptyList()
        val csrfToken = cookies.find { it.name == "csrftoken" }?.value ?: ""
        Timber.d("CSRF Token found: ${csrfToken.isNotEmpty()}")
        if (csrfToken.isEmpty()) {
             Timber.w("No CSRF token found in cookies! Login will likely fail.")
        }
        
        val form = FormBody.Builder()
            .add("enc_password", "#PWD_INSTAGRAM_BROWSER:0:${System.currentTimeMillis() / 1000}:$pass") // Specific format for web
            .add("username", username)
            .add("queryParams", "{}")
            .add("optIntoOneTap", "false")
            .build()
            
        val request = Request.Builder()
            .url(loginUrl)
            .post(form)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Pixel 2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")
            .header("X-CSRFToken", csrfToken)
            .header("X-Instagram-AJAX", "1")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "https://www.instagram.com/accounts/login/")
            .build()
            
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty Login Response")
        
        // Parse Web response. It's different from App response.
        // Usually contains { "authenticated": true, "userId": ... }
        if (body.contains("\"authenticated\":true")) {
            // Success
             android.util.Log.d("InstagramClient", "Login Successful")
             InstaLoginResponse("ok", InstaUser(0, username, "", ""), null, null) // Mock user obj for now
        } else {
             android.util.Log.e("InstagramClient", "Login Failed Body: $body")
             Timber.e("Instagram Login Failed. Response: $body")
             throw Exception("Login Failed. Server Response: $body")
        }
    }
    
    // For Reels, if we used Web Login, we must use Web Endpoints.
    // GET https://www.instagram.com/api/v1/clips/home/ (GraphQL or REST)
    
    suspend fun getReels(): Result<List<InstaClipItem>> = runCatching {
        // Try Web API for clips
        val request = Request.Builder()
            .url("https://www.instagram.com/api/v1/clips/home/")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Pixel 2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")
            .header("X-IG-App-ID", "936619743392459") // Common Web App ID
            // Add other headers as needed
            .build()
            
        val response = client.newCall(request).execute()
        val body = response.body?.string()
        
        // Timber.d("Reels Response: $body")
        
        // Parse Logic needed here.
        // ...
        emptyList()
    }
}
