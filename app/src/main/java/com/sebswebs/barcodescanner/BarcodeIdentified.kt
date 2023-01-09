package com.sebswebs.barcodescanner

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.sebswebs.barcodescanner.databinding.ActivityBarcodeIdentifiedBinding

class BarcodeIdentified : AppCompatActivity() {
    private lateinit var binding: ActivityBarcodeIdentifiedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeIdentifiedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvHooray.text = intent.getStringExtra("barcodeValue")
        binding.btScanAnother.setOnClickListener(object: View.OnClickListener {
            override fun onClick(view: View?) {
                switchToMain()
            }
        })
    }
    private fun switchToMain() {
        val switchActivityIntent = Intent(this, MainActivity::class.java)
        startActivity(switchActivityIntent)
    }
}