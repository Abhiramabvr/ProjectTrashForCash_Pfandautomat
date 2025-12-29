package com.bvr.projectjtcm

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bvr.projectjtcm.databinding.ActivityQrDisplayBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class QrDisplayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrDisplayBinding
    private var orderId: String? = null
    private var statusListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra("ORDER_ID")

        if (orderId == null) {
            binding.tvOrderId.text = "Error: No Order ID Found"
            return
        }

        binding.tvOrderId.text = "Order ID: $orderId"
        binding.toolbar.setNavigationOnClickListener { finish() }

        generateQrCode(orderId!!)
        listenForStatusChanges(orderId!!)
    }

    private fun generateQrCode(orderId: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(orderId, BarcodeFormat.QR_CODE, 400, 400)
            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenForStatusChanges(orderId: String) {
        val database = FirebaseDatabase.getInstance().getReference("waste_history").child(orderId)
        statusListener = database.child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "Completed") {
                    binding.ivSuccessOverlay.visibility = View.VISIBLE
                    // Optional: auto-close after a few seconds
                    binding.root.postDelayed({ finish() }, 3000) 
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hentikan listener untuk mencegah memory leak
        if (orderId != null && statusListener != null) {
            val database = FirebaseDatabase.getInstance().getReference("waste_history").child(orderId!!)
            database.child("status").removeEventListener(statusListener!!)
        }
    }
}