package com.bvr.projectjtcm.ui.user

import android.content.Intent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bvr.projectjtcm.R

// Class ini adalah INDUK. Semua Activity User akan mewarisi class ini.
open class BaseActivity : AppCompatActivity() {

    // Fungsi ini bisa dipanggil oleh MainActivity, HistoryActivity, dll.
    protected fun setupBottomNavigation(activeId: Int) {

        val navHome = findViewById<ImageView>(R.id.navHome)
        val navHistory = findViewById<ImageView>(R.id.navHistory)
        val navOrder = findViewById<ImageView>(R.id.navOrder)
        val navProfile = findViewById<ImageView>(R.id.navProfile)

        // Cek jika layout tidak memiliki navigasi (Safety Check)
        if (navHome == null || navHistory == null || navOrder == null || navProfile == null) return

        // Set transparansi ikon (0.5 = redup, 1.0 = terang)
        navHome.alpha = if (activeId == R.id.navHome) 1.0f else 0.5f
        navHistory.alpha = if (activeId == R.id.navHistory) 1.0f else 0.5f
        navOrder.alpha = if (activeId == R.id.navOrder) 1.0f else 0.5f
        navProfile.alpha = if (activeId == R.id.navProfile) 1.0f else 0.5f

        // Logika Pindah Halaman
        navHome.setOnClickListener { navigateTo(MainActivity::class.java, activeId, R.id.navHome) }
        navHistory.setOnClickListener { navigateTo(HistoryActivity::class.java, activeId, R.id.navHistory) }
        navOrder.setOnClickListener { navigateTo(PriceListActivity::class.java, activeId, R.id.navOrder) }
        navProfile.setOnClickListener { navigateTo(ProfileActivity::class.java, activeId, R.id.navProfile) }
    }

    private fun navigateTo(destination: Class<*>, currentId: Int, targetId: Int) {
        if (currentId != targetId) {
            val intent = Intent(this, destination)
            // Hapus animasi agar transisi terlihat instan seperti aplikasi profesional
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish() // Matikan activity lama hemat memori
        }
    }
}