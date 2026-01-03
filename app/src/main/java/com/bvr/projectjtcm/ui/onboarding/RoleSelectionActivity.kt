package com.bvr.projectjtcm.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bvr.projectjtcm.ui.collector.CollectorMainActivity
import com.bvr.projectjtcm.ui.auth.LoginActivity // Import LoginActivity yang baru dibuat
import com.bvr.projectjtcm.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tvTitle.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // Jalur Collector (Masih Langsung Masuk - Belum ada Login Khusus)
        binding.cardCollector.setOnClickListener {
            val intent = Intent(this, CollectorMainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Jalur User (UBAH KE SINI: Masuk ke Login dulu)
        binding.cardUser.setOnClickListener {
            // Arahkan ke LoginActivity, bukan MainActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Kita finish() supaya kalau user tekan Back di halaman Login,
            // dia keluar aplikasi (bukan balik ke pemilihan role).
            // Ini standar UX yang umum.
            finish()
        }
    }
}