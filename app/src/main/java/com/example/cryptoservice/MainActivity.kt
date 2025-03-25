package com.example.cryptoservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvUSD: TextView
    private lateinit var tvJPY: TextView
    private lateinit var tvRUB: TextView

    private val priceUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val usd = it.getDoubleExtra("usd", 0.0)
                val jpy = it.getDoubleExtra("jpy", 0.0)
                val rub = it.getDoubleExtra("rub", 0.0)

                tvUSD.text = "USD: $usd"
                tvJPY.text = "JPY: $jpy"
                tvRUB.text = "RUB: $rub"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvUSD = findViewById(R.id.tvUSD)
        tvJPY = findViewById(R.id.tvJPY)
        tvRUB = findViewById(R.id.tvRUB)

        val intent = Intent(this, CryptoService::class.java)
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.example.cryptoservice.PRICE_UPDATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(priceUpdateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(priceUpdateReceiver)
    }
}
