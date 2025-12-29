package com.bvr.projectjtcm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bvr.projectjtcm.databinding.ActivityCollectorMainBinding
import com.google.firebase.database.FirebaseDatabase
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CollectorMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectorMainBinding
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var toneGenerator: ToneGenerator
    private val CAMERA_PERMISSION_REQUEST_CODE = 101
    
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            result?.let {
                val orderId = it.text
                if (orderId.isNotEmpty()) {
                    barcodeView.pause()
                    updateOrderStatus(orderId)
                }
            }
        }
        
        override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectorMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        barcodeView = binding.barcodeScanner
        // Inisialisasi ToneGenerator
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            barcodeView.decodeContinuous(callback)
        }

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Lepaskan resource saat activity dihancurkan
        toneGenerator.release()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                barcodeView.resume()
                barcodeView.decodeContinuous(callback)
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk scanner", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateOrderStatus(orderId: String) {
        val database = FirebaseDatabase.getInstance().getReference("waste_history").child(orderId)
        database.child("status").setValue("Completed")
            .addOnSuccessListener {
                // Mainkan suara BEEP
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                
                showSuccessOverlay()
                Toast.makeText(this, "✅ Order #$orderId Selesai!", Toast.LENGTH_SHORT).show()
                barcodeView.postDelayed({
                    barcodeView.resume()
                    barcodeView.decodeContinuous(callback)
                }, 2000)
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Gagal mengupdate status: ${it.message}", Toast.LENGTH_SHORT).show()
                barcodeView.resume()
                barcodeView.decodeContinuous(callback)
            }
    }

    private fun showSuccessOverlay() {
        binding.ivSuccessOverlay.visibility = View.VISIBLE
        binding.ivSuccessOverlay.postDelayed({
            binding.ivSuccessOverlay.visibility = View.GONE
        }, 1500)
    }

    private fun setupBottomNavigation() {
        val navScan = findViewById<ImageView>(R.id.navScan)
        val navEditPrices = findViewById<ImageView>(R.id.navEditPrices)

        navScan.setOnClickListener {
            // Sudah di sini
        }

        navEditPrices.setOnClickListener {
            val intent = Intent(this, CollectorPriceListActivity::class.java)
            startActivity(intent)
        }
    }
}