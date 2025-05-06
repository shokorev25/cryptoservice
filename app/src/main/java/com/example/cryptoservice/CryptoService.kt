package com.example.cryptoservice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask

class CryptoService : Service() {

    private val TAG = "CryptoService"
    private val CHANNEL_ID = "CryptoServiceChannel"
    private var lastUsdPrice = 0.0
    private val threshold = 0.000001  // Минимальный порог изменения

    // Инициализация OkHttp клиента с логированием
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // Таймер для периодических запросов
    private val timer = Timer()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()  // Создание канала уведомлений
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                fetchCryptoPrice()
            }
        }, 0, 10_000)  // Запрос каждые 10 секунд
        return START_STICKY
    }

    private fun fetchCryptoPrice() {
        // Формируем запрос к API CryptoCompare для получения курсов BTC
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

                    // Отправляем данные через LocalBroadcast
                    val broadcastIntent = Intent("com.example.cryptoservice.PRICE_UPDATE").apply {
                        putExtra("usd", usdPrice)
                        putExtra("jpy", jpyPrice)
                        putExtra("rub", rubPrice)
                    }
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)

                    // Проверка и отправка уведомления при изменении цены
                    if (lastUsdPrice != 0.0) {
                        val diff = (usdPrice - lastUsdPrice) / lastUsdPrice
                        if (kotlin.math.abs(diff) >= threshold) {
                            sendNotification(usdPrice, diff)
                        }
                    }
                    lastUsdPrice = usdPrice
                }
            } else {
                Log.e(TAG, "Ошибка запроса: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных", e)
        }
    }

    private fun sendNotification(price: Double, diff: Double) {
        val direction = if (diff > 0) "⬆️" else "⬇️"
        val percentage = String.format("%.2f", diff * 100)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Обновление курса BTC")
            .setContentText("$direction BTC: $price USD ($percentage%)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Crypto Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для уведомлений о курсе BTC"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
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
