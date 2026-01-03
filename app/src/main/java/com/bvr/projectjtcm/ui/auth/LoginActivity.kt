package com.bvr.projectjtcm.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bvr.projectjtcm.databinding.ActivityLoginBinding
import com.bvr.projectjtcm.ui.user.MainActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Loading..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Selamat Datang!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "LOGIN"
                        Toast.makeText(this, "Login Gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.tvRegister.setOnClickListener {
            // Pindah ke halaman Register
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}