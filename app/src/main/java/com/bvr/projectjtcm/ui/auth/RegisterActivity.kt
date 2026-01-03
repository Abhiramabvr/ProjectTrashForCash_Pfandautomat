package com.bvr.projectjtcm.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bvr.projectjtcm.databinding.ActivityRegisterBinding
import com.bvr.projectjtcm.ui.user.MainActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("projecjtcm") // Dari screenshot json kamu
                    .setApiKey("AIzaSyDPeW3b38E5zkeOwhEGgOarpV4MxzIJRGU") // Dari screenshot json kamu
                    .setDatabaseUrl("https://projecjtcm-default-rtdb.asia-southeast1.firebasedatabase.app") // Dari screenshot json kamu
                    .setApplicationId("1:419876464236:android:4b7f1d87a2f1a6d7a09a49")
                    .build()

                FirebaseApp.initializeApp(this, options)

            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal Init Manual: ${e.message}", Toast.LENGTH_LONG).show()
        }

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val name = binding.etName.text.toString().trim()


            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Mohon isi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Loading..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            saveUserToDatabase(userId, name, email)
                        }
                    } else {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "REGISTER"
                        Toast.makeText(this, "Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Tombol pindah ke Login jika sudah punya akun
        binding.tvLogin.setOnClickListener {
            finish() // Tutup halaman register, kembali ke halaman sebelumnya (Login)
        }
    }

    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        val database = FirebaseDatabase.getInstance().getReference("users")

        val userMap = mapOf(
            "name" to name,
            "email" to email,
            "role" to "User"
        )

        database.child(userId).setValue(userMap)
            .addOnCompleteListener {
                Toast.makeText(this, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }
    }
}