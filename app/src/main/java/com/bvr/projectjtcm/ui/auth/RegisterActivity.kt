package com.bvr.projectjtcm.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bvr.projectjtcm.databinding.ActivityRegisterBinding
import com.bvr.projectjtcm.ui.user.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init Firebase Auth (Cukup ini saja, plugin google-services yang urus sisanya)
        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLogin.setOnClickListener {
            finish() // Kembali ke Login
        }
    }

    private fun registerUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val name = binding.etName.text.toString().trim()

        // Validasi Input
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            showToast("Mohon isi semua data!")
            return
        }

        if (password.length < 6) {
            showToast("Password minimal 6 karakter")
            return
        }

        // UI Loading State
        setLoading(true)

        // Proses Create User di Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserToDatabase(userId, name, email)
                    } else {
                        setLoading(false)
                        showToast("Gagal mendapatkan User ID")
                    }
                } else {
                    setLoading(false)
                    showToast("Register Gagal: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserToDatabase(userId: String, name: String, email: String) {
        // PENTING: Karena kamu pakai region asia-southeast1, kita pasang URL spesifik agar aman
        // URL ini harus sama persis dengan yang ada di google-services.json
        val dbUrl = "https://projecjtcm-default-rtdb.asia-southeast1.firebasedatabase.app"
        val database = FirebaseDatabase.getInstance(dbUrl).getReference("users")

        val userMap = mapOf(
            "name" to name,
            "email" to email,
            "role" to "User", // Default role
            "balance" to 0    // Tambahan: Siapkan field saldo untuk fitur 'Cash' nanti
        )

        database.child(userId).setValue(userMap)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    showToast("Akun berhasil dibuat!")
                    val intent = Intent(this, MainActivity::class.java)
                    // Clear task agar user tidak bisa back ke halaman register
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    showToast("Gagal menyimpan data user: ${task.exception?.message}")
                }
            }
    }

    // Helper untuk loading agar code lebih bersih
    private fun setLoading(isLoading: Boolean) {
        binding.apply {
            btnRegister.isEnabled = !isLoading
            btnRegister.text = if (isLoading) "Loading..." else "REGISTER"
            etEmail.isEnabled = !isLoading
            etPassword.isEnabled = !isLoading
            etName.isEnabled = !isLoading
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}