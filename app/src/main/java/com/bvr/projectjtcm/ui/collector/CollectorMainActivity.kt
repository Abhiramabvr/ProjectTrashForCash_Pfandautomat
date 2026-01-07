package com.bvr.projectjtcm.ui.collector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bvr.projectjtcm.R
import com.bvr.projectjtcm.databinding.ActivityCollectorMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.ResultPoint
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
                    // Pause scanner agar tidak scan berulang-ulang saat proses loading
                    barcodeView.pause()
                    findAndUpdateOrder(orderId)
                }
            }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectorMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        barcodeView = binding.barcodeScanner
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

    // --- LOGIC BARU: MENCARI ORDER DI DALAM FOLDER USER ---
    private fun findAndUpdateOrder(orderId: String) {
        val rootRef = FirebaseDatabase.getInstance().getReference("waste_history")

        // Kita ambil "Snapshot" dari seluruh folder waste_history
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isFound = false

                // Loop: Cek satu per satu folder milik User (Uid)
                for (userSnapshot in snapshot.children) {
                    // Cek apakah User ini punya anak bernama 'orderId'
                    if (userSnapshot.hasChild(orderId)) {

                        // KETEMU! Order ada di user ini.
                        // Sekarang kita update statusnya jadi "Completed"
                        userSnapshot.child(orderId).child("status").ref.setValue("Completed")
                            .addOnSuccessListener {
                                onSuccessUpdate(orderId)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@CollectorMainActivity, "Gagal update: ${it.message}", Toast.LENGTH_SHORT).show()
                                resumeScanner()
                            }

                        isFound = true
                        break // Berhenti mencari karena sudah ketemu
                    }
                }

                if (!isFound) {
                    Toast.makeText(this@CollectorMainActivity, "❌ Order ID tidak ditemukan!", Toast.LENGTH_SHORT).show()
                    resumeScanner()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CollectorMainActivity, "Error Database: ${error.message}", Toast.LENGTH_SHORT).show()
                resumeScanner()
            }
        })
    }

    private fun onSuccessUpdate(orderId: String) {
        // Efek Suara
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)

        // Efek Visual
        showSuccessOverlay()
        Toast.makeText(this, "✅ Order Selesai!", Toast.LENGTH_SHORT).show()

        // Delay sedikit sebelum scan lagi
        resumeScanner(2000)
    }

    private fun resumeScanner(delay: Long = 0) {
        barcodeView.postDelayed({
            barcodeView.resume()
            barcodeView.decodeContinuous(callback)
        }, delay)
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

        navScan.alpha = 1.0f // Aktif
        navEditPrices.alpha = 0.5f // Tidak aktif

        navScan.setOnClickListener {
            // Sudah di halaman ini
        }

        navEditPrices.setOnClickListener {
            val intent = Intent(this, CollectorPriceListActivity::class.java)
            // Agar transisi mulus
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }
    }
}