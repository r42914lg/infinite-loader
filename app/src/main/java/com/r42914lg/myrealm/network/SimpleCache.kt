package com.r42914lg.myrealm.network

import android.content.Context
import com.r42914lg.myrealm.network.HttpService.HEADER_CACHE
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import java.io.File
import java.util.concurrent.TimeUnit

object HttpService {
    const val HEADER_CACHE = "android-cache"
    private const val CACHE_DIR = "httpCache"

    private var httpClient: OkHttpClient? = null

    fun initOfflineFirst(ctx: Context) {
        val httpCacheDirectory = File(ctx.cacheDir, CACHE_DIR)
        val cache = Cache(httpCacheDirectory, 10 * 1024 * 1024)
        httpClient = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(5, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request: Request = chain.request()
                if (request.header(HEADER_CACHE) != null) {
                    val offlineRequest: Request = request.newBuilder()
                        .header(
                            "Cache-Control", "only-if-cached, "
                                    + "max-stale=" + request.header(HEADER_CACHE)
                        )
                        .build()
                    val response: Response = chain.proceed(offlineRequest)
                    if (response.isSuccessful) {
                        return@addInterceptor response
                    }
                }
                chain.proceed(request)
            }
            .build()
    }

    fun initOfflineLast(ctx: Context) {
        val httpCacheDirectory = File(ctx.cacheDir, CACHE_DIR)
        val cache = Cache(httpCacheDirectory, 10 * 1024 * 1024)
        httpClient = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(5, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                try {
                    chain.proceed(chain.request())
                } catch (e: Exception) {
                    val offlineRequest = chain.request()
                        .newBuilder()
                        .header("Cache-Control", "public, only-if-cached, "
                                + "max-stale=" + 60 * 60 * 24)
                    .build()
                    chain.proceed(offlineRequest);
                }
            }
            .build()
    }

    fun getHttpClient(): OkHttpClient {
        if (httpClient == null)
            throw IllegalStateException("Call init with context first!!!")

        return httpClient!!
    }
}

interface SomeService {
    @GET("/my_endpoint")
    @Headers("$HEADER_CACHE: 60")
    suspend fun getData(): List<Any>
}