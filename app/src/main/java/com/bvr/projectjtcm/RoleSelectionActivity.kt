package com.bvr.projectjtcm

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
            // Menambahkan padding atas pada judul
            binding.tvTitle.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        binding.cardCollector.setOnClickListener {
            val intent = Intent(this, CollectorMainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.cardUser.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Hapus halaman ini dari back stack
        }
    }
}