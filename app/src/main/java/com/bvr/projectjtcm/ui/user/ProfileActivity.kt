package com.bvr.projectjtcm.ui.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bvr.projectjtcm.R
import com.bvr.projectjtcm.databinding.ActivityProfileBinding
import com.bvr.projectjtcm.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    // Launcher untuk ganti foto profil (Disimpan lokal dulu sementara)
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.ivProfileImage.setImageURI(it)
            // Note: Upload ke Firebase Storage butuh penanganan khusus,
            // untuk saat ini kita biarkan ganti di tampilan saja.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Cek apakah user login? Jika tidak, tendang ke LoginActivity
        if (currentUser == null) {
            goToLogin()
            return
        }

        // 2. Siapkan Referensi Database ke path: users -> (UID User)
        // Pastikan path ini sama dengan saat Register ("users")
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)

        // Setup Tampilan
        setupWindowInsets()

        // Setup Logic
        loadUserDataFromFirebase() // Ambil data realtime
        fetchRecycledStats()       // Ambil data sampah (dummy logic)
        setupClickListeners()      // Handle klik tombol edit & logout

        // 3. Setup Navigasi Bawah (Warisan dari BaseActivity)
        setupBottomNavigation(R.id.navProfile)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val backParams = binding.btnBack.layoutParams as ViewGroup.MarginLayoutParams
            backParams.topMargin = systemBars.top + (20 * resources.displayMetrics.density).toInt()
            binding.btnBack.layoutParams = backParams
            binding.bottomNavContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    // --- FUNGSI UTAMA: Baca Data Live dari Firebase ---
    private fun loadUserDataFromFirebase() {
        // Tampilkan loading sementara
        binding.tvName.text = "Loading..."

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Ambil data dari snapshot database
                    // Key string ("name", "email", dll) HARUS SAMA dengan yang ada di Firebase Console
                    val name = snapshot.child("name").value.toString()
                    val email = snapshot.child("email").value.toString()
                    val role = snapshot.child("role").value.toString()

                    // Gunakan Elvis Operator (?:) untuk data yang mungkin kosong
                    val phone = snapshot.child("phone").value?.toString() ?: "-"
                    val instagram = snapshot.child("instagram").value?.toString() ?: "-"

                    // Update UI
                    binding.tvName.text = name
                    binding.tvRole.text = if (role == "null") "User" else role
                    binding.tvEmailValue.text = email
                    binding.tvPhoneValue.text = phone
                    binding.tvInstagramValue.text = instagram
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Gagal memuat profil: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- FUNGSI UPDATE: Simpan Perubahan ke Firebase ---
    private fun saveToFirebase(key: String, value: String) {
        // dbRef sudah mengarah ke user yang login, jadi tinggal set child(key)
        dbRef.child(key).setValue(value)
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal update: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Dialog pop-up untuk mengedit data
    private fun showEditDialog(title: String, databaseKey: String, currentValue: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val etValue = dialogView.findViewById<EditText>(R.id.etValue)

        // Jika isinya tanda strip "-", kosongkan edit text agar enak diedit
        etValue.setText(if (currentValue == "-") "" else currentValue)

        AlertDialog.Builder(this)
            .setTitle("Edit $title")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val newValue = etValue.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    saveToFirebase(databaseKey, newValue)
                } else {
                    Toast.makeText(this, "$title tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cvProfileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Edit Nama
        binding.tvName.setOnClickListener {
            showEditDialog("Nama", "name", binding.tvName.text.toString())
        }

        // Edit Role (Job)
        binding.tvRole.setOnClickListener {
            showEditDialog("Role", "role", binding.tvRole.text.toString())
        }

        // Email biasanya read-only karena terkait login
        binding.itemEmail.setOnClickListener {
            Toast.makeText(this, "Email tidak dapat diubah", Toast.LENGTH_SHORT).show()
        }

        // Edit No HP
        binding.itemPhone.setOnClickListener {
            showEditDialog("No HP", "phone", binding.tvPhoneValue.text.toString())
        }

        // Edit Instagram
        binding.itemInstagram.setOnClickListener {
            showEditDialog("Instagram", "instagram", binding.tvInstagramValue.text.toString())
        }

        // --- TOMBOL LOGOUT (Sesuai ID di XML baru) ---
        binding.btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun fetchRecycledStats() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        // UBAH DISINI: Tambahkan .child(currentUser.uid) agar hanya membaca data milik sendiri
        val database = FirebaseDatabase.getInstance().getReference("waste_history").child(currentUser.uid)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalWeight = 0
                for (dataSnapshot in snapshot.children) {
                    val weight = dataSnapshot.child("weight").getValue(Int::class.java) ?: 0
                    totalWeight += weight
                }
                binding.tvRecycledTotal.text = "$totalWeight kg"
            }
            override fun onCancelled(error: DatabaseError) {
                binding.tvRecycledTotal.text = "0 kg"
            }
        })
    }

    private fun logoutUser() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari akun?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                // 1. Hapus sesi Firebase
                auth.signOut()

                // 2. Pindah ke halaman Login & Hapus history activity
                goToLogin()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}