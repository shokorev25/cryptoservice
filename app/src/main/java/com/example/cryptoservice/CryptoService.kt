package com.example.cryptoservice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask

class CryptoService : Service() {

    private val TAG = "CryptoService"

    // Инициализация OkHttp клиента с логированием
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private val timer = Timer()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                fetchCryptoPrice()
            }
        }, 0, 10_000)
        return START_STICKY
    }

    private fun fetchCryptoPrice() {
        val url = "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD,JPY,RUB"
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonResponse = response.body?.string()
                jsonResponse?.let {
                    val jsonObject = JSONObject(it)
                    val usdPrice = jsonObject.optDouble("USD", 0.0)
                    val jpyPrice = jsonObject.optDouble("JPY", 0.0)
                    val rubPrice = jsonObject.optDouble("RUB", 0.0)

                    Log.d(TAG, "BTC Price: USD=$usdPrice, JPY=$jpyPrice, RUB=$rubPrice")

                    val intent = Intent("com.example.cryptoservice.PRICE_UPDATE").apply {
                        putExtra("usd", usdPrice)
                        putExtra("jpy", jpyPrice)
                        putExtra("rub", rubPrice)
                    }
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    Log.d(TAG, "Broadcast sent")
                }
            } else {
                Log.e(TAG, "Ошибка запроса: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}
