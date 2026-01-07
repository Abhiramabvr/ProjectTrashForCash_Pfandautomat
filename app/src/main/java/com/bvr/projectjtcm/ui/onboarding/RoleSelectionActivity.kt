package com.bvr.projectjtcm.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bvr.projectjtcm.databinding.ActivityRoleSelectionBinding
import com.bvr.projectjtcm.ui.auth.LoginActivity
import com.bvr.projectjtcm.ui.collector.CollectorMainActivity
import com.bvr.projectjtcm.ui.user.MainActivity
import com.google.firebase.auth.FirebaseAuth

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- FITUR BARU: AUTO LOGIN CHECK ---
        // Cek apakah ada user yang masih login?
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Tutup halaman role selection
            return // Stop eksekusi kode di bawah
        }

        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Saya ubah padding ke root agar lebih aman untuk semua layout
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // Jalur Collector (Masuk Langsung)
        binding.cardCollector.setOnClickListener {
            val intent = Intent(this, CollectorMainActivity::class.java)
            startActivity(intent)
            // finish() // Opsional: Boleh di-finish atau tidak, tergantung selera navigasi
        }

        // Jalur User (Ke Login Dulu)
        binding.cardUser.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // SAYA HAPUS finish() DI SINI
            // Kenapa? Supaya kalau user salah pencet "User", dia bisa tekan tombol Back
            // di HP-nya untuk kembali ke halaman ini dan memilih "Collector".
        }
    }
}