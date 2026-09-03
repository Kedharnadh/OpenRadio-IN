package dev.openradio.android.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared HTTP client for all network calls in the app. Reusing one client avoids
 * repeated connection setup, enables connection pooling, and keeps timeouts
 * consistent across stations, metadata and the HLS probe.
 */
object HttpClient {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
